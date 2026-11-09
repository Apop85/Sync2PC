#!/usr/bin/env python3
"""
Nachbearbeitung der festen Leo-Frames (SYNC-20261109-066, Ergaenzung): Aussenlinie neu zeichnen.

Liest die vorhandenen app/src/main/res/drawable-nodpi/anim_dog_*.png (RGBA, 320x320), bestimmt die
Silhouette aus dem Alpha-Kanal, glaettet sie, entfernt dunkle Hintergrundreste (grau-dunkle Flaechen,
die nicht Teil der Figur sind) und zeichnet danach eine geschlossene, ueberall gleich dicke dunkle
Aussenlinie. Antialiasing durch Zeichnen in 4-facher Aufloesung und Flaechenmittelung (kein heller Saum).

ACHTUNG: Ueberschreibt die Frames. Vorher Sicherung anlegen (z. B. .build/). Nicht Teil des Builds.
Aufruf:  python tools/refine-anim-outline.py [--src <Ordner>] [--out <Ordner>] [--width 2.6]
Voraussetzungen: Python 3 mit numpy, scipy, Pillow.
"""
import argparse, glob, os
import numpy as np
from PIL import Image
from scipy import ndimage as ndi

SS = 4                              # Ueberabtastung
OUTLINE_RGB = (30, 25, 25)          # Linienfarbe
SMOOTH_SIGMA = 1.3                  # Konturglaettung (Pixel im Zielbild)
HOLE_MAX = 400                      # umschlossene Loecher bis so viele Zielpixel werden gefuellt
GAP_SAT = 22                        # Hintergrundrest: wenig Farbsaettigung ...
GAP_LUM_MIN, GAP_LUM_MAX = 45, 150  # ... und mittel/dunkelgrau (Halsband/Linie sind schwarz < 45)
GAP_MIN_THICK = 2.0                 # Hintergrundrest nur, wenn mind. so dick (Zielpixel)
DARK_LUM = 80                      # dunkle dicke Flaechen im unteren Koerperteil (Schatten zwischen Beinen) ...
DARK_THICK = 0.9                    # ... mind. so dick (Zielpixel); Linie/Halsband (oben) bleiben
DARK_LOW = 0.58                    # nur wenn Schwerpunkt unterhalb dieses Anteils der Figurhoehe liegt
CLOSE_R = 3.5                      # Einrisse/Kerben schmaler als 2*CLOSE_R (Zielpixel) werden geschlossen


def refine(img, width):
    a8 = np.asarray(img)                                  # H,W,4 uint8
    n = a8.shape[0]
    big = n * SS
    A = Image.fromarray(a8[..., 3]).resize((big, big), Image.BICUBIC)
    A = np.asarray(A).astype(np.float32) / 255.0
    rgbp = a8[..., :3].astype(np.float32) * (a8[..., 3:4].astype(np.float32) / 255.0)
    chans = [np.asarray(Image.fromarray(rgbp[..., i], mode='F').resize((big, big), Image.BICUBIC)) for i in range(3)]
    rgb = np.stack(chans, -1) / np.maximum(A[..., None], 1e-3)
    rgb = np.clip(rgb, 0, 255)

    S = A > 0.5
    # Dunkelgraue Hintergrundreste (nicht Linie/Halsband: die sind schwarz) entfernen, wenn dick genug
    lum = rgb @ np.array([0.299, 0.587, 0.114], np.float32)
    sat = rgb.max(-1) - rgb.min(-1)
    gapc = S & (sat < GAP_SAT) & (lum >= GAP_LUM_MIN) & (lum <= GAP_LUM_MAX)
    core = ndi.distance_transform_edt(gapc) >= GAP_MIN_THICK * SS
    lab, k = ndi.label(gapc)
    if k:
        keep = np.unique(lab[core]); keep = keep[keep > 0]
        S = S & ~np.isin(lab, keep)
    # Dicke dunkle Flaechen im unteren Koerperteil (Schatten/Rest zwischen den Beinen) entfernen
    dark = S & (lum < DARK_LUM) & (sat < 60)
    dcore = ndi.distance_transform_edt(dark) >= DARK_THICK * SS
    ys = np.nonzero(S.any(1))[0]; top, bot = ys.min(), ys.max()
    lab, k = ndi.label(dcore)
    for i in range(1, k + 1):
        cy = np.nonzero(lab == i)[0].mean()
        if (cy - top) / max(bot - top, 1) > DARK_LOW:
            S &= ~ndi.binary_dilation(ndi.binary_propagation(lab == i, mask=dark), iterations=SS)
    # Einrisse schliessen (Morphologie-Schliessen mit Scheibe)
    r = int(CLOSE_R * SS)
    yy, xx = np.mgrid[-r:r + 1, -r:r + 1]
    disc = (xx * xx + yy * yy) <= r * r
    pad = r + 2
    Sp = np.pad(S, pad)
    Sp = ndi.binary_erosion(ndi.binary_dilation(Sp, disc), disc)
    S = Sp[pad:-pad, pad:-pad] | S
    # Kleine umschlossene Loecher fuellen
    holes, hk = ndi.label(~S)
    if hk:
        sizes = ndi.sum(np.ones_like(holes), holes, index=np.arange(1, hk + 1))
        border = set(np.unique(np.concatenate([holes[0], holes[-1], holes[:, 0], holes[:, -1]])))
        for i, sz in enumerate(sizes, 1):
            if i not in border and sz <= HOLE_MAX * SS * SS:
                S[holes == i] = True
    # Nur die groesste Flaeche, Kontur glaetten
    lab, k = ndi.label(S)
    if k > 1:
        sizes = ndi.sum(S, lab, index=np.arange(1, k + 1))
        S = lab == (1 + int(np.argmax(sizes)))
    S = ndi.gaussian_filter(S.astype(np.float32), SMOOTH_SIGMA * SS) > 0.5
    S = ndi.binary_fill_holes(S) if False else S

    # Farbe: Original, ausserhalb des Originals (durch Glaettung dazugekommen) naechster deckender Pixel
    src = (A > 0.5) & ~((lum < DARK_LUM) & (sat < 60) & (ndi.distance_transform_edt(S) > width * SS + 2))
    idx = ndi.distance_transform_edt(~src, return_distances=False, return_indices=True)
    fill = rgb[idx[0], idx[1]]
    # Konstante Linie: alles innerhalb Abstand <= width vom Rand der Silhouette
    dist = ndi.distance_transform_edt(S)
    ring = S & (dist <= width * SS)
    out_rgb = fill.copy()
    out_rgb[ring] = OUTLINE_RGB
    alpha = S.astype(np.float32)

    # Verkleinern per Flaechenmittelung (premultiplied)
    prem = out_rgb * alpha[..., None]
    sm = [np.asarray(Image.fromarray(prem[..., i], mode='F').resize((n, n), Image.BOX)) for i in range(3)]
    sa = np.asarray(Image.fromarray(alpha, mode='F').resize((n, n), Image.BOX))
    out = np.zeros((n, n, 4), np.uint8)
    for i in range(3):
        out[..., i] = np.clip(np.where(sa > 1e-4, sm[i] / np.maximum(sa, 1e-4), 0), 0, 255).astype(np.uint8)
    out[..., 3] = np.clip(sa * 255 + 0.5, 0, 255).astype(np.uint8)
    out[out[..., 3] == 0, :3] = 0
    return Image.fromarray(out, 'RGBA')


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    ap = argparse.ArgumentParser()
    ap.add_argument('--src', default=os.path.join(root, 'app', 'src', 'main', 'res', 'drawable-nodpi'))
    ap.add_argument('--out', default=None, help='Ausgabeordner (Standard: --src ueberschreiben)')
    ap.add_argument('--width', type=float, default=2.6, help='Liniendicke in Pixeln (320er-Bild)')
    args = ap.parse_args()
    out_dir = args.out or args.src
    os.makedirs(out_dir, exist_ok=True)
    files = sorted(glob.glob(os.path.join(args.src, 'anim_dog_*.png')))
    for f in files:
        refine(Image.open(f).convert('RGBA'), args.width).save(os.path.join(out_dir, os.path.basename(f)))
        print('.', end='', flush=True)
    print(f'\n{len(files)} Bilder -> {out_dir}')


if __name__ == '__main__':
    main()



