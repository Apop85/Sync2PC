#!/usr/bin/env python3
"""
EINMAL-Export der Laufanimation von Leo (Archivwerkzeug, NICHT Teil des Builds).

ACHTUNG: Ueberschreibt die festen Quell-Assets  app/src/main/res/drawable-nodpi/anim_dog_*.png
und app/src/main/res/layout/notif_dog_flipper.xml. Die Frames gelten seit SYNC-20261109-066 als
veredelte, eingefrorene Assets; tools/build.ps1 erzeugt sie nie neu. Vor dem Aufruf eine Sicherung
anlegen (z. B. nach .build/). Wird nur manuell aufgerufen, wenn die Animation bewusst neu entsteht.

Quelle:   .claude/images/new_animation.mp4  (Bilder 13-49 = eine Schleife)
Methode:  Freistellung ueber eine geschlossene Aussenkontur (nicht ueber Helligkeits-Schwellen allein):
  1. Fell (hell) und schwarze Umrisslinie bilden die Figur; kleine Luecken der Linie werden per
     Morphologie-Schliessen geschlossen, alles von der Kontur Umschlossene wird deckend (Fell,
     Halsband, Augen, ferne Beine).
  2. Nur echte Luecken (dunkler Hintergrund, mind. GAP_ERODE Pixel dick, z. B. zwischen den Beinen)
     werden wieder transparent; Halsband bleibt (schwarz, nicht neutral-dunkel).
  3. Weicher Alpha-Rand durch Flaechenmittelung beim Verkleinern (premultiplied, kein heller Saum).
Voraussetzungen: Python 3 mit numpy + Pillow, ffmpeg/ffprobe im PATH.
Aufruf:  python tools/export-anim-frames.py [--size 320] [--out <Ordner>]
         (--out: Ausgabe in anderen Ordner statt ins Projekt, z. B. fuer Vorschau)
"""
import argparse, glob, os, shutil, subprocess, sys, tempfile
import numpy as np
from PIL import Image

START, END = 13, 49                    # Schleife (1-basiert, wie bisher)
WATERMARK = (1210, 1335, 1430, 1430)   # x1,y1,x2,y2 im quadratischen Bild
SAT_MIN = 18                           # Farbsaettigung (max-min) ab der ein Pixel als Fell gilt (Hintergrund/Schein neutral)
FUR_LUM_MIN = 100                      # und mind. so hell
OUTLINE_MAX = 26                       # dunkler = Umrisslinie
CLOSE_R = 5                            # Schliessen kleiner Fell-Luecken (Pixel im 1430er-Bild)
LINE_REACH = 40                        # Umrisslinie/Halsband zaehlt nur so nah am Fell
DUP_DIFF = 0.1                         # Bild gilt als Doppelbild, wenn mittlere Abweichung kleiner (0-255)
GAP_ERODE = 10                         # Luecke nur, wenn mind. so dick (Halsband bleibt)


def lum_of(rgb):
    return (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]).astype(np.int16)


def dilate(m, r=1):
    for _ in range(r):
        n = m.copy()
        n[1:] |= m[:-1]; n[:-1] |= m[1:]; n[:, 1:] |= m[:, :-1]; n[:, :-1] |= m[:, 1:]
        n[1:, 1:] |= m[:-1, :-1]; n[:-1, :-1] |= m[1:, 1:]; n[1:, :-1] |= m[:-1, 1:]; n[:-1, 1:] |= m[1:, :-1]
        m = n
    return m


def erode(m, r=1):
    return ~dilate(~m, r)


def reconstruct(seed, mask):
    """Flutfuellung: alles in mask, das von seed erreichbar ist (4er-Nachbarschaft)."""
    cur = seed & mask
    while True:
        n = cur.copy()
        n[1:] |= cur[:-1]; n[:-1] |= cur[1:]; n[:, 1:] |= cur[:, :-1]; n[:, :-1] |= cur[:, 1:]
        n &= mask
        if np.array_equal(n, cur):
            return cur
        cur = n


def largest_component(m):
    """Nur die groesste zusammenhaengende Flaeche behalten (entfernt Einzelpunkte)."""
    h, w = m.shape
    best = np.zeros_like(m)
    rest = m.copy()
    while rest.any():
        y, x = np.argwhere(rest)[0]
        seed = np.zeros_like(m); seed[y, x] = True
        comp = reconstruct(seed, rest)
        if comp.sum() > best.sum():
            best = comp
        rest &= ~comp
        if best.sum() > rest.sum():
            break
    return best


def silhouette(rgb):
    """Maske der Figur. Fell = warm getoentes Pixel (Saettigung), Hintergrund und Schein sind neutral grau."""
    a = rgb.astype(np.int16)
    lum = lum_of(rgb)
    sat = a.max(axis=2) - a.min(axis=2)
    h, w = lum.shape
    x1, y1, x2, y2 = WATERMARK
    wm = np.zeros((h, w), bool); wm[y1:y2, x1:x2] = True
    fur = (sat >= SAT_MIN) & (lum >= FUR_LUM_MIN) & ~wm
    fur = erode(dilate(fur, CLOSE_R), CLOSE_R)            # kleine Fell-Luecken schliessen
    fur = largest_component(fur)
    # Umschlossenes fuellen (Augen, Nase, Halsband, Mundinneres): Hintergrund = vom Bildrand erreichbar
    border = np.zeros_like(fur); border[0] = border[-1] = True; border[:, 0] = border[:, -1] = True
    outside = reconstruct(border & ~fur, ~fur)
    filled = ~outside
    # Echte Luecken (neutral dunkler Hintergrund, mind. GAP_ERODE dick, z. B. zwischen den Beinen) wieder entfernen
    added = filled & ~fur
    cand = added & (sat < SAT_MIN) & (lum > OUTLINE_MAX) & (lum <= 200)
    seeds = erode(cand & (lum <= 80), GAP_ERODE)
    gap = reconstruct(seeds, cand)
    sil = filled & ~gap
    # Schwarze Umrisslinie dazunehmen (nur nahe an der Figur), Hintergrund (dunkelgrau, > OUTLINE_MAX) nicht
    # (auch das schwarze Halsband: zusammenhaengende dunkle Flaeche, die am Fell anliegt)
    dark = (lum <= OUTLINE_MAX) & dilate(sil, LINE_REACH) & ~wm & ~dilate(gap, 1)
    line = reconstruct(dilate(sil, 2) & dark, dark)
    sil |= line
    sil = erode(dilate(sil, 2), 2)                        # Linienreste glaetten
    outside = reconstruct(border & ~sil, ~sil)
    sil = ~outside & ~gap
    return largest_component(sil)

def to_small(rgb, mask, box, minx, miny, cropw, croph, size):
    """Ausschnitt quadratisch zentrieren und per Flaechenmittelung (premultiplied) verkleinern."""
    side = box
    canvas_rgb = np.zeros((side, side, 3), np.float32)
    canvas_a = np.zeros((side, side), np.float32)
    ox = (side - cropw) // 2; oy = (side - croph) // 2
    a = mask[miny:miny + croph, minx:minx + cropw].astype(np.float32)
    c = rgb[miny:miny + croph, minx:minx + cropw].astype(np.float32)
    canvas_rgb[oy:oy + croph, ox:ox + cropw] = c * a[..., None]
    canvas_a[oy:oy + croph, ox:ox + cropw] = a
    # Verkleinern je Kanal mit Box-/Lanczos-Filter im Float-Modus
    chans = [Image.fromarray(canvas_rgb[..., i], mode='F').resize((size, size), Image.BOX if side > 2 * size else Image.LANCZOS)
             for i in range(3)]
    al = Image.fromarray(canvas_a, mode='F').resize((size, size), Image.BOX if side > 2 * size else Image.LANCZOS)
    A = np.clip(np.asarray(al), 0, 1)
    out = np.zeros((size, size, 4), np.uint8)
    for i in range(3):
        ch = np.asarray(chans[i])
        out[..., i] = np.clip(np.where(A > 1e-4, ch / np.maximum(A, 1e-4), 0), 0, 255).astype(np.uint8)
    out[..., 3] = np.clip(A * 255 + 0.5, 0, 255).astype(np.uint8)
    out[out[..., 3] == 0, :3] = 0
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--size', type=int, default=320)
    ap.add_argument('--out', default=None, help='Ausgabeordner statt Projekt (res/drawable-nodpi)')
    ap.add_argument('--root', default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    args = ap.parse_args()
    root = args.root
    video = os.path.join(root, '.claude', 'images', 'new_animation.mp4')
    if not os.path.exists(video):
        sys.exit('Nicht gefunden: ' + video)
    out_dir = args.out or os.path.join(root, 'app', 'src', 'main', 'res', 'drawable-nodpi')
    layout_dir = args.out or os.path.join(root, 'app', 'src', 'main', 'res', 'layout')
    os.makedirs(out_dir, exist_ok=True); os.makedirs(layout_dir, exist_ok=True)

    tmp = tempfile.mkdtemp(prefix='leo_anim_')
    try:
        pr = subprocess.check_output(['ffprobe', '-v', 'error', '-select_streams', 'v:0', '-show_entries',
                                      'stream=width,height', '-of', 'csv=p=0', video], text=True).strip().split(',')
        vw, vh = int(pr[0]), int(pr[1]); side = min(vw, vh)
        cx, cy = (vw - side) // 2, (vh - side) // 2
        subprocess.check_call(['ffmpeg', '-v', 'error', '-y', '-i', video, '-vf',
                               f"select='between(n\\,{START - 1}\\,{END - 1})',crop={side}:{side}:{cx}:{cy}",
                               '-fps_mode', 'passthrough', os.path.join(tmp, 'f_%03d.png')])
        files = sorted(glob.glob(os.path.join(tmp, 'f_*.png')))
        print(len(files), 'Bilder extrahiert', side, 'x', side)

        frames = []
        for f in files:
            rgb = np.asarray(Image.open(f).convert('RGB'))
            frames.append((rgb, silhouette(rgb)))
            print('.', end='', flush=True)
        print()

        # Gemeinsamer Rahmen
        minx = miny = 10 ** 6; maxx = maxy = 0
        for _, m in frames:
            ys, xs = np.nonzero(m)
            minx = min(minx, xs.min()); maxx = max(maxx, xs.max() + 1)
            miny = min(miny, ys.min()); maxy = max(maxy, ys.max() + 1)
        cropw, croph = int(maxx - minx), int(maxy - miny); box = max(cropw, croph)

        small = []
        for rgb, m in frames:
            o = to_small(rgb, m, box, int(minx), int(miny), cropw, croph, args.size)
            if small and np.abs(small[-1].astype(np.int16) - o.astype(np.int16)).mean() < DUP_DIFF:
                continue                                       # Doppelbild
            small.append(o)

        for f in glob.glob(os.path.join(out_dir, 'anim_dog_*.png')):
            os.remove(f)
        for i, o in enumerate(small, 1):
            Image.fromarray(o, 'RGBA').save(os.path.join(out_dir, f'anim_dog_{i:02d}.png'))
        n = len(small)
        interval = round(((END - START + 1) * 1000 / 24) / n)
        imgs = '\n'.join('    <ImageView android:layout_width="match_parent" android:layout_height="match_parent" '
                         f'android:scaleType="fitCenter" android:src="@drawable/anim_dog_{i:02d}" />' for i in range(1, n + 1))
        xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Laufender Hund - feste Quell-Assets (eingefroren seit SYNC-20261109-066), NICHT vom Build erzeugt.
     Einmal-Export: tools/export-anim-frames.py ({n} Bilder, {interval} ms je Bild).
     Benachrichtigungen koennen keine Videos/GIFs abspielen; ein ViewFlipper blaettert die Bilder durch
     und pausiert automatisch bei ausgeschaltetem Bildschirm. -->
<ViewFlipper xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:autoStart="true"
    android:flipInterval="{interval}">
{imgs}
</ViewFlipper>'''
        with open(os.path.join(layout_dir, 'notif_dog_flipper.xml'), 'w', encoding='utf-8', newline='') as fh:
            fh.write(xml)
        print(f'{n} Animationsbilder ({args.size} px, {interval} ms) erzeugt in {out_dir}')
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == '__main__':
    main()
