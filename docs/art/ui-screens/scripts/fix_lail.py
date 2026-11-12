"""Lail (mint): hand-placed matching eyes. Both eyes = the 2x4 running eye (dark, 1 highlight), 3 px apart;
closed frames get two matching 3x2 arches. Old eye, its shading and the snout dot are painted over with
the face colour."""
import numpy as np, os
from PIL import Image
D = (48, 25, 27); FACE = (207, 177, 229); HI = (255, 249, 233)
EYE = ['##', '#w', '##', '##']
ARCH = ['.##.', '#..#']
# frame: (near eye left, top, erase box x0,y0,x1,y1, mode)
F = {
 'idle_0': (25, 23, (23, 21, 27, 27), 'open'), 'run_0': (25, 23, (23, 21, 27, 27), 'open'),
 'run_1': (25, 22, (23, 20, 27, 26), 'open'), 'run_2': (25, 23, (23, 21, 27, 27), 'open'),
 'run_3': (24, 23, (22, 21, 26, 27), 'open'), 'jump_0': (25, 24, (23, 22, 27, 28), 'open'),
 'jump_1': (25, 23, (23, 21, 27, 27), 'open'), 'land_0': (25, 23, (23, 22, 27, 27), 'open'),
 'fail_0': (25, 23, (23, 21, 27, 27), 'open'), 'greet_0': (25, 23, (23, 21, 27, 27), 'open'),
 'idle_1': (24, 23, (21, 21, 28, 26), 'closed'), 'home_0': (24, 23, (21, 22, 27, 26), 'closed'),
}
GAP = 5   # far eye left = near left + 5
def is_dark(p): return abs(int(p[0])-D[0]) + abs(int(p[1])-D[1]) + abs(int(p[2])-D[2]) < 40
for f, (nx, ny, (ex0, ey0, ex1, ey1), mode) in F.items():
    src = np.array(Image.open(f'src/sprite_mint_{f}.png').convert('RGBA'))
    out = src.copy()
    a = src[:, :, 3]
    face_cols = {tuple(src[y, x, :3]) for y in range(ey0, ey1 + 1) for x in range(ex0 - 2, ex1 + 8) if a[y, x]}
    # 1) paint the old eye + shading inside the erase box with face colour (never touch the outline: pixels
    #    whose 4-neighbour is transparent, or dark pixels connected to such)
    for y in range(ey0, ey1 + 1):
        for x in range(ex0, ex1 + 1):
            p = tuple(src[y, x, :3])
            if a[y, x] and p not in [FACE, (181, 138, 202)] and not (y >= 27 and p in [(255, 240, 206), (255, 249, 233)]):
                if y >= 27 and is_dark(p): continue          # mouth line
                out[y, x, :3] = FACE
    # 2) snout dots (single dark pixels inside the face, right of the eyes)
    for y in range(ny - 1, ny + 5):
        for x in range(nx + 8, 36):
            if a[y, x] and is_dark(src[y, x, :3]):
                nb = [(y, x - 1), (y, x + 1), (y - 1, x), (y + 1, x)]
                if all(a[yy, xx] and not is_dark(src[yy, xx, :3]) for yy, xx in nb):
                    out[y, x, :3] = FACE
    # 3) stamp both eyes
    stamp = EYE if mode == 'open' else ARCH
    if mode == 'open': nx -= 2
    for lx in (nx, nx + GAP):
        for r, row in enumerate(stamp):
            for c, ch in enumerate(row):
                Y, X = ny + r + (1 if mode == 'closed' else 0), lx + c
                assert a[Y, X], (f, X, Y)
                if ch == '#': out[Y, X, :3] = D
                elif ch == 'w': out[Y, X, :3] = HI
                elif ch == '.': out[Y, X, :3] = FACE
    Image.fromarray(out, 'RGBA').save(f'fixed/sprite_mint_{f}.png')
print('lail done')
