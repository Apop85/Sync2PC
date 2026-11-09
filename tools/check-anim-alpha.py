#!/usr/bin/env python3
"""
Wiederholbare Qualitaetspruefung der Laufanimation (anim_dog_*.png, SYNC-20261109-066).

Misst je Bild den Alpha-Kanal und erzeugt eine Vorschau auf mehreren Hintergruenden.
  - Loecher: vollstaendig von der Figur umschlossene transparente Flaechen, getrennt nach klein
    (< SMALL_HOLE Pixel = Fell-Loch; Summe je Bild darf MAX_SMALL_TOTAL nicht ueberschreiten) und gross (= echte Luecke zwischen den Beinen, erlaubt)
  - Halbdeckung: Anteil teiltransparenter Pixel (1..254) an den deckenden Pixeln (weicher Rand: erwartet, aber nicht zu viel)
  - Saum: teiltransparente Randpixel, die viel heller sind als ihr deckender Nachbar (heller Saum)
Aufruf: python tools/check-anim-alpha.py [--dir <Ordner>] [--preview <Datei.png>]
Exit-Code 1, wenn ein Grenzwert verletzt ist.
"""
import argparse, glob, os, sys
import numpy as np
from PIL import Image

SMALL_HOLE = 60            # Pixel (320er-Bild): kleinere umschlossene Loecher gelten als Loch im Fell
MAX_SMALL_TOTAL = 60       # erlaubt je Bild (Summe): winzige echte Einbuchtungen, z. B. zwischen Rueken und Kopf
MAX_SEMI_RATIO = 0.06      # Anteil teiltransparenter Pixel an der Figur
MAX_FRINGE_PX = 60         # helle Saumpixel je Bild (alter Stand 0.13.4: ca. 220-270)
COLORS = [(40, 160, 80), (140, 190, 235), (150, 150, 150), (200, 50, 50), (250, 250, 250), (32, 33, 36)]


def outside_mask(transparent):
    h, w = transparent.shape
    cur = np.zeros_like(transparent)
    cur[0] = cur[-1] = True; cur[:, 0] = cur[:, -1] = True
    cur &= transparent
    while True:
        n = cur.copy()
        n[1:] |= cur[:-1]; n[:-1] |= cur[1:]; n[:, 1:] |= cur[:, :-1]; n[:, :-1] |= cur[:, 1:]
        n &= transparent
        if np.array_equal(n, cur):
            return cur
        cur = n


def hole_sizes(enclosed):
    seen = np.zeros_like(enclosed)
    sizes = []
    h, w = enclosed.shape
    for y, x in np.argwhere(enclosed):
        if seen[y, x]:
            continue
        stack = [(y, x)]; seen[y, x] = True; n = 0
        while stack:
            cy, cx = stack.pop(); n += 1
            for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ny, nx = cy + dy, cx + dx
                if 0 <= ny < h and 0 <= nx < w and enclosed[ny, nx] and not seen[ny, nx]:
                    seen[ny, nx] = True; stack.append((ny, nx))
        sizes.append(n)
    return sizes


def main():
    ap = argparse.ArgumentParser()
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    ap.add_argument('--dir', default=os.path.join(root, 'app', 'src', 'main', 'res', 'drawable-nodpi'))
    ap.add_argument('--preview', default=None)
    args = ap.parse_args()
    files = sorted(glob.glob(os.path.join(args.dir, 'anim_dog_*.png')))
    if not files:
        sys.exit('Keine Bilder in ' + args.dir)
    bad = 0
    tot_small = tot_big = 0
    print(f'{len(files)} Bilder in {args.dir}')
    print('Bild  kleineLoecher(px)  grosseLuecken(px)  Halbdeckung  Saum(px)')
    ims = []
    for f in files:
        im = np.asarray(Image.open(f).convert('RGBA')); ims.append(im)
        a = im[..., 3]
        enclosed = (a < 128) & ~outside_mask(a < 128)
        sizes = hole_sizes(enclosed)
        small = sum(s for s in sizes if s < SMALL_HOLE); big = sum(s for s in sizes if s >= SMALL_HOLE)
        figure = max(1, int((a > 0).sum()))
        semi = float(((a > 0) & (a < 255)).sum()) / figure
        lum = (0.299 * im[..., 0] + 0.587 * im[..., 1] + 0.114 * im[..., 2])
        opaque = a == 255
        # Saum: teiltransparent und heller als der hellste deckende Nachbar im 3x3-Feld um 25 Helligkeit
        nb = np.zeros_like(lum)
        padded = np.pad(np.where(opaque, lum, 0), 1)
        for dy in (0, 1, 2):
            for dx in (0, 1, 2):
                nb = np.maximum(nb, padded[dy:dy + lum.shape[0], dx:dx + lum.shape[1]])
        fringe = int((((a > 0) & (a < 255)) & (lum > nb + 25) & (nb > 0)).sum())
        flag = ''
        if small > MAX_SMALL_TOTAL or semi > MAX_SEMI_RATIO or fringe > MAX_FRINGE_PX:
            flag = '  <-- Grenzwert'; bad += 1
        tot_small += small; tot_big += big
        print(f'{os.path.basename(f)[9:11]:>4}  {small:>12}  {big:>17}  {semi * 100:>9.1f} %  {fringe:>7}{flag}')
    print(f'Summe: kleine Loecher {tot_small} px, grosse Luecken {tot_big} px, Bilder ueber Grenzwert: {bad}')
    if args.preview:
        pick = [ims[i] for i in (0, len(ims) // 4, len(ims) // 2, 3 * len(ims) // 4)]
        s = 320
        sheet = Image.new('RGB', (s * len(pick), s * len(COLORS)))
        for r, col in enumerate(COLORS):
            for c, im in enumerate(pick):
                bg = Image.new('RGBA', (s, s), col + (255,)); bg.alpha_composite(Image.fromarray(im, 'RGBA'))
                sheet.paste(bg.convert('RGB'), (c * s, r * s))
        sheet.save(args.preview)
        print('Vorschau:', args.preview)
    sys.exit(1 if bad else 0)


if __name__ == '__main__':
    main()
