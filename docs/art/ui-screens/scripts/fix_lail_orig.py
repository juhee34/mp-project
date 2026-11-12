"""Lail (mint): both eyes use Lail's ORIGINAL eye from the same frame (dark block + highlight, same size and
rows). The near eye moves 3 px left and the same eye is copied 7 px to its right, so the two eyes sit apart
like the other friends'. The snout dot is painted over. Closed-eye frames (idle_1, home_0) keep two matching
arches (the original single arc is too wide to fit twice)."""
import numpy as np, sys
from PIL import Image
sys.path.insert(0, '.')
D = np.array([48, 25, 27]); FACE = [207, 177, 229]
OPEN = {  # frame: original eye box (x0,y0,x1,y1) covering dark + highlight
 'idle_0': (24,22,26,26), 'run_0': (25,22,26,26), 'run_1': (25,21,26,25), 'run_2': (25,22,26,26),
 'run_3': (24,22,25,26), 'jump_0': (24,23,26,27), 'jump_1': (24,22,26,26), 'land_0': (24,23,26,26),
 'fail_0': (24,22,26,26), 'greet_0': (25,22,26,26)}
SHIFT, GAP = -3, 7
def dark(p): return np.abs(p[:3].astype(int) - D).sum() < 40
for f, (x0, y0, x1, y1) in OPEN.items():
    src = np.array(Image.open(f'src/sprite_mint_{f}.png').convert('RGBA'))
    out = src.copy(); a = src[:, :, 3]
    patch = src[y0:y1 + 1, x0:x1 + 1].copy()
    # mask: dark pixels + light pixels enclosed by them (highlight)
    m = np.ones(patch.shape[:2], bool)      # the box is exactly the original eye: copy all of it
    # drop columns of the box that hold no eye pixels (e.g. shading beside a 2-wide eye)
    cols = [c for c in range(m.shape[1]) if m[:, c].any()]
    patch, m = patch[:, cols[0]:cols[-1] + 1], m[:, cols[0]:cols[-1] + 1]
    ex0 = x0 + cols[0]
    # erase the old eye (+ shading 1px around it) and the snout dot
    for y in range(y0 - 1, y1 + 2):
        for x in range(x0 - 1, x1 + 2):
            if a[y, x] and (y, x) and not (y >= 27 and dark(src[y, x])):
                p = tuple(src[y, x, :3])
                if dark(src[y, x]) or p not in [tuple(FACE), (181, 138, 202)]:
                    if not (y >= 27): out[y, x, :3] = FACE
    for y in range(y0, y1 + 1):
        for x in range(x1 + 4, 36):
            if a[y, x] and dark(src[y, x]):
                nb = [(y, x-1), (y, x+1), (y-1, x), (y+1, x)]
                if all(a[yy, xx] and not dark(src[yy, xx]) for yy, xx in nb): out[y, x, :3] = FACE
    for k, lx in enumerate((ex0 + SHIFT, ex0 + SHIFT + GAP)):
        P = patch[:, ::-1] if (f == 'land_0' and k == 1) else patch   # squint: > <
        for r in range(m.shape[0]):
            for c in range(m.shape[1]):
                if m[r, c]:
                    Y, X = y0 + r, lx + c
                    assert a[Y, X] and not dark(src[Y, X]) or (x0 - 1 <= X <= x1 + 1), (f, X, Y)
                    out[Y, X] = P[r, c]
    Image.fromarray(out, 'RGBA').save(f'fixed/sprite_mint_{f}.png')
print('done')
