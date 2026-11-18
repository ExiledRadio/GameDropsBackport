import math, sys
from PIL import Image, ImageDraw, ImageFilter

# Family tile layout (stars, corner radius) comes from the Wild Update tile, so every icon matches.
TILE = "E:/MincraftModding/Workspaces/BackportCore/src/main/resources/assets/backportcore/textures/gui/family/wild-update-backport.png"
TEX = sys.argv[1]   # sulfur_cube_outer.png (26.2), for the comparison overlay
OUT = sys.argv[2]
src = Image.open(TILE).convert("RGBA")
px = src.load()
W = H = 64
S = 16

BASE = (22, 17, 26)
GLOW = (112, 100, 22)    # sulfur yellow glow
SIGMA2 = 267.0


def bg_color(r):
    f = math.exp(-(r * r) / SIGMA2)
    edge = max(0.0, (r - 22.0) / 14.0)
    base = (BASE[0] - 4 * edge, BASE[1] - 2 * edge, BASE[2] - 2 * edge)
    return tuple(int(round(max(0, base[i] + GLOW[i] * f))) for i in range(3)) + (255,)


def is_star(p):
    return p[0] > 120 and p[1] > 120 and p[2] > 120 and abs(p[0] - p[1]) < 40


seen, stars = set(), []
for y in range(H):
    for x in range(W):
        if (x, y) in seen or not is_star(px[x, y]) or (13 <= x <= 51 and 11 <= y <= 51):
            continue
        stack, pts = [(x, y)], []
        while stack:
            p = stack.pop()
            if p in seen or not (0 <= p[0] < W and 0 <= p[1] < H) or not is_star(px[p[0], p[1]]):
                continue
            seen.add(p); pts.append(p)
            stack += [(p[0] + 1, p[1]), (p[0] - 1, p[1]), (p[0], p[1] + 1), (p[0], p[1] - 1)]
        stars.append((sum(p[0] for p in pts) / len(pts) + 0.5, sum(p[1] for p in pts) / len(pts) + 0.5, len(pts)))
corner = sum(1 for x in range(32) if px[x, 0][3] < 128)

# ---- the sulfur cube, front view: a translucent jelly shell (outer 18 px box) around a sulfur core (inner 16 px box)
# whose lower part is tan liquid, the face on the shell. Colors read from 26.2's textures.
SHELL = (236, 242, 168)
SHELL_TOP = (250, 252, 226)
SHELL_SIDE = (200, 210, 120)
CORE_TOP = (244, 252, 220)
CORE_MID = (230, 232, 120)
CORE_YELLOW = (222, 214, 84)
LIQUID = (212, 182, 120)
LIQUID_DARK = (188, 154, 100)
EYE = (64, 59, 48, 255)
EYE_HI = (143, 101, 79, 255)
MOUTH = (82, 68, 55, 255)

art = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))

# shell spans 18 texture pixels; map 1 texture pixel to U units, centered
U = 1.78
X0, Y0 = 32 - 9 * U, 33 - 9 * U


def T(x, y):  # texture pixel coords (0..18 across the front face) to art pixels
    return (X0 + x * U) * S, (Y0 + y * U) * S


def layer():
    im = Image.new("RGBA", art.size, (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


# shadow
sh, d = layer()
d.ellipse([*T(-1.5, 17.0), *T(19.5, 20.0)], fill=(8, 6, 12, 160))
art.alpha_composite(sh.filter(ImageFilter.GaussianBlur(22)))

# core (inner 16 box, one texture pixel inside the shell): pale top, yellow middle, tan liquid with a wavy surface
core, d = layer()
cx0, cy0, cx1, cy1 = T(0.45, 0.45) + T(17.55, 17.55)
rad = 1.2 * U * S
d.rounded_rectangle([cx0, cy0, cx1, cy1], radius=rad, fill=CORE_MID + (255,))
d.rounded_rectangle([cx0, cy0, cx1, cy0 + (cy1 - cy0) * 0.30], radius=rad, fill=CORE_TOP + (255,))
d.rectangle([cx0, cy0 + (cy1 - cy0) * 0.22, cx1, cy0 + (cy1 - cy0) * 0.34], fill=CORE_TOP + (255,))
# liquid surface: a gentle wave across the core, tan below
pts = []
n = 64
for i in range(n + 1):
    x = cx0 + (cx1 - cx0) * i / n
    y = cy0 + (cy1 - cy0) * (0.60 + 0.045 * math.sin(i / n * math.pi * 3.0 + 0.6))
    pts.append((x, y))
d.polygon([(cx0, cy1)] + pts + [(cx1, cy1)], fill=LIQUID + (255,))
# a lighter yellow band just above the liquid
band = [(x, y - 0.07 * (cy1 - cy0)) for x, y in pts]
d.line(band, fill=CORE_YELLOW + (255,), width=int(0.9 * U * S))
d.rectangle([cx0, cy1 - (cy1 - cy0) * 0.12, cx1, cy1], fill=LIQUID_DARK + (255,))
# bubbles rising from the liquid
for bx, by, br in ((5.0, 9.5, 0.75), (12.5, 7.0, 0.55), (9.0, 11.2, 0.45), (14.0, 11.6, 0.6)):
    c = T(bx, by)
    r = br * U * S
    d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], fill=CORE_TOP + (255,))
# round the core's corners to the shell
mask = Image.new("L", art.size, 0)
ImageDraw.Draw(mask).rounded_rectangle([cx0, cy0, cx1, cy1], radius=rad, fill=255)
core.putalpha(Image.composite(core.getchannel("A"), Image.new("L", art.size, 0), mask))
art.alpha_composite(core)

# shell: translucent jelly over the core, a lit top strip and a shaded right side, a glint at the top left
shell, d = layer()
sx0, sy0, sx1, sy1 = T(0, 0) + T(18, 18)
srad = 1.8 * U * S
d.rounded_rectangle([sx0, sy0, sx1, sy1], radius=srad, fill=SHELL + (135,))
d.rounded_rectangle([sx0, sy0, sx1, sy0 + 2.2 * U * S], radius=srad, fill=SHELL_TOP + (210,))
d.rounded_rectangle([sx1 - 1.6 * U * S, sy0 + 1.0 * U * S, sx1, sy1], radius=srad, fill=SHELL_SIDE + (150,))
d.rounded_rectangle([sx0, sy1 - 1.4 * U * S, sx1, sy1], radius=srad, fill=SHELL_SIDE + (170,))
art.alpha_composite(shell)
# the jelly's rim: a slightly brighter outline
rim, d = layer()
d.rounded_rectangle([sx0, sy0, sx1, sy1], radius=srad, outline=SHELL_TOP + (235,), width=int(0.55 * U * S))
art.alpha_composite(rim)
gl, d = layer()
g0 = T(2.0, 2.6)
d.rounded_rectangle([g0[0], g0[1], g0[0] + 3.2 * U * S, g0[1] + 0.9 * U * S], radius=0.4 * U * S, fill=(255, 255, 255, 220))
d.rounded_rectangle([g0[0], g0[1] + 1.6 * U * S, g0[0] + 0.9 * U * S, g0[1] + 3.4 * U * S], radius=0.4 * U * S, fill=(255, 255, 255, 200))
art.alpha_composite(gl)

# face, from the texture's front face: 3x3 eyes at columns 2-4 and 13-15, rows 10-12; a 2x1 mouth at 8-9, row 16
face, d = layer()
for ex in (2, 13):
    a, b = T(ex, 10), T(ex + 3, 13)
    d.rounded_rectangle([a[0], a[1], b[0], b[1]], radius=0.35 * U * S, fill=EYE)
    h0, h1 = T(ex + 0.35, 10.35), T(ex + 1.25, 11.25)
    d.rounded_rectangle([h0[0], h0[1], h1[0], h1[1]], radius=0.2 * U * S, fill=EYE_HI)
a, b = T(7.6, 15.6), T(10.4, 16.6)
d.rounded_rectangle([a[0], a[1], b[0], b[1]], radius=0.3 * U * S, fill=MOUTH)
art.alpha_composite(face)


def compose(size):
    k = size / 64.0
    tile = Image.new("L", (size, size), 0)
    ImageDraw.Draw(tile).rounded_rectangle([0, 0, size - 1, size - 1], radius=corner * k * 1.15, fill=255)
    glow = Image.new("RGBA", (size, size))
    gp = glow.load()
    for y in range(size):
        for x in range(size):
            gp[x, y] = bg_color(math.hypot((x + 0.5) / k - 32, (y + 0.5) / k - 32))
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(glow, (0, 0), tile)
    sl = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sd = ImageDraw.Draw(sl)
    for sx, sy, n in stars:
        c, r = (sx * k, sy * k), (1.3 + 0.35 * min(n, 5)) * k
        w = r * 0.22
        sd.polygon([(c[0], c[1] - r), (c[0] + w, c[1] - w), (c[0] + r, c[1]), (c[0] + w, c[1] + w),
                    (c[0], c[1] + r), (c[0] - w, c[1] + w), (c[0] - r, c[1]), (c[0] - w, c[1] - w)], fill=(240, 236, 250, 255))
    out.alpha_composite(sl.filter(ImageFilter.GaussianBlur(max(0.6, 1.2 * k / 8))))
    out.alpha_composite(sl)
    out.alpha_composite(art.resize((size, size), Image.LANCZOS))
    return out


big = compose(512)
big.save(OUT)
compose(64).save(OUT.replace(".png", "_64.png"))
# comparison: the texture's front face (scaled to the shell) under the drawing, for checking the face placement
tex = Image.open(TEX).convert("RGBA").crop((18, 18, 36, 36)).resize((int(18 * U * 8), int(18 * U * 8)), Image.NEAREST)
cmp = Image.new("RGBA", (1044, 512), (40, 40, 48, 255))
cmp.alpha_composite(big, (0, 0))
ov = big.copy()
ov.alpha_composite(tex, (int(X0 * 8), int(Y0 * 8)))
cmp.alpha_composite(ov, (532, 0))
cmp.save(OUT.replace(".png", "_compare.png"))
print("ok", len(stars), "stars")
