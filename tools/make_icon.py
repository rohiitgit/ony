"""Turn 'ony' in Manrope into icon path data on the 108x108 adaptive-icon canvas.

Usage (needs fonttools):
    python tools/make_icon.py app/src/main/res/font/manrope.ttf 400 44 paths.txt
Writes one path per letter (one per line) for res/drawable/ic_launcher_foreground.xml.
Arguments: font file, weight, word width in dp, output file.
"""
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.transformPen import TransformPen

font_path, weight, text_width, out = sys.argv[1], float(sys.argv[2]), float(sys.argv[3]), sys.argv[4]
f = instantiateVariableFont(TTFont(font_path), {"wght": weight})
gs, cmap = f.getGlyphSet(), f.getBestCmap()
names = [cmap[ord(c)] for c in "ony"]
# Lay glyphs out by advance width (font units, y up).
x, placed = 0, []
for n in names:
    placed.append((n, x)); x += f["hmtx"][n][0]
def bounds(items):
    bp = BoundsPen(gs)
    for n, dx in items:
        gs[n].draw(TransformPen(bp, (1, 0, 0, 1, dx, 0)))
    return bp.bounds
xmin, ymin, xmax, ymax = bounds(placed)
o = bounds(placed[:1])                       # the 'o' body: x-height band
scale = text_width / (xmax - xmin)
cx, cy = 54.0, 54.0
# Map font units -> canvas: centre ink horizontally, centre the 'o' vertically (optical centre).
tx = cx - (xmin + xmax) / 2 * scale
o_mid = (o[1] + o[3]) / 2
ty = cy + o_mid * scale                      # y flips: canvas_y = ty - font_y*scale
paths = []
for n, dx in placed:  # one path per glyph keeps each under lint's long-path limit
    pen = SVGPathPen(gs, ntos=lambda v: f"{v:.1f}".rstrip("0").rstrip("."))
    gs[n].draw(TransformPen(pen, (scale, 0, 0, -scale, tx + dx * scale, ty)))
    paths.append(pen.getCommands())
open(out, "w").write("\n".join(paths))
top, bottom = ty - ymax * scale, ty - ymin * scale
print(f"wght {weight}: ink x {cx - text_width/2:.1f}-{cx + text_width/2:.1f}, y {top:.1f}-{bottom:.1f}, 'o' centre at {cy}")
