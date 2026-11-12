"""Make both eyes the same on every original sprite frame (A request 2026-11-12).
Per frame the near/far eye pixels are listed by hand (bounding boxes of the dark eye pieces, read from the
frame). The bigger eye's pixel block is used for both; eyes stay level on the bigger eye's rows; only face
pixels are painted. Where the far eye was hidden, it is added at near + DX."""
import numpy as np, os, json
from PIL import Image
from collections import deque
OUTLINE = np.array([48, 25, 27])
SRC, DST = 'src', 'fixed'
os.makedirs(DST, exist_ok=True)

# frame: (near boxes, far boxes or None, far centre offset from near centre when far is None, extra pixels to erase)
SPEC = {
 'mint': {'dx': 5, 'shade': [], 'frames': {
   'idle_0': ([(24,22,26,26)], None, [(32,25,32,25)], 'run2w'), 'idle_1': ([(22,22,27,25)], None, [(32,25,32,25)], 'arch'),
   'run_0': ([(25,22,26,26)], None, [(32,25,32,25)]), 'run_1': ([(24,21,26,25)], None, [(32,24,32,24)]),
   'run_2': ([(25,22,26,26)], None, [(32,25,32,25)]), 'run_3': ([(24,22,25,26)], None, [(32,25,32,25)]),
   'jump_0': ([(24,23,26,27)], None, [(31,25,31,25)], 'run2w'), 'jump_1': ([(24,22,26,26)], None, [(32,25,32,25)], 'run2w'),
   'land_0': ([(24,23,24,23),(24,24,26,26)], None, [(32,25,32,25)]), 'fail_0': ([(24,22,26,26)], None, [(32,25,32,25)], 'run2w'),
   'greet_0': ([(25,22,26,26)], None, [(32,25,32,25)]), 'home_0': ([(22,23,26,25)], None, [(31,25,31,25)], 'arch')}},
 'cheese': {'shade': [(169,126,49),(169,107,49),(116,67,39)], 'frames': {
   'idle_0': ([(24,23,24,27)], [(32,24,33,26)]), 'idle_1': ([(21,24,25,26)], [(31,24,33,26),(30,25,30,26)]),
   'run_0': ([(23,24,24,28)], [(32,25,33,26)]), 'run_1': ([(23,22,24,26)], [(32,23,33,25)]),
   'run_2': ([(24,24,25,27)], [(32,25,33,26)]), 'run_3': ([(23,22,24,26)], [(32,23,33,24)]),
   'jump_0': ([(24,25,25,28)], [(32,25,33,27)]), 'land_0': ([(21,24,24,28)], [(33,27,33,27)], [], 'mirror'),
   'fail_0': ([(23,26,24,27),(20,27,22,28)], [(30,24,33,26)]), 'greet_0': ([(23,23,24,27)], [(31,24,31,26)]),
   'home_0': ([(22,25,22,25),(19,26,21,27)], [(29,23,31,24),(32,23,32,23)])}},
 'popo': {'shade': [], 'frames': {
   'idle_0': ([(24,24,24,27)], [(31,24,32,26)]), 'idle_1': ([(23,25,25,26),(22,26,22,26)], [(31,24,33,25)]),
   'run_1': ([(26,25,27,28)], [(33,25,34,27)]), 'fail_0': ([(25,25,26,27)], [(32,24,33,27)]),
   'greet_0': ([(23,24,23,26)], [(29,23,30,26)])}},
 'lumi': {'dx': 6, 'shade': [], 'frames': {
   'idle_0': ([(25,25,26,27)], [(31,24,31,26)]), 'run_0': ([(27,25,28,27)], None),
   'run_2': ([(27,25,28,27)], [(33,25,33,27)]), 'run_3': ([(28,25,28,27)], [(33,25,34,27)]),
   'jump_0': ([(25,24,26,26)], None), 'jump_1': ([(25,25,26,28)], [(31,25,32,27)]),
   'greet_0': ([(22,25,24,27)], [(29,24,30,26)])}},
 'nunu': {'shade': [], 'frames': {
   'idle_1': ([(19,25,24,27)], [(29,23,32,25),(33,24,34,25)]), 'run_1': ([(24,23,25,27)], [(33,22,34,25)]),
   'run_3': ([(25,23,26,27)], [(33,22,34,25)]), 'jump_0': ([(20,24,24,25),(25,25,25,25)], [(30,22,33,24)]),
   'jump_1': ([(23,23,25,27)], [(32,22,33,25)]), 'fail_0': ([(19,24,21,27)], [(28,23,29,26)]),
   'greet_0': ([(21,24,22,27)], [(29,22,31,25)])}},
 'piko': {'dx': 7, 'shade': [], 'frames': {
   'idle_0': ([(23,27,24,29)], [(30,26,30,28)]), 'idle_1': ([(23,27,23,29)], [(29,26,30,28)]),
   'run_0': ([(23,25,24,27)], [(30,24,30,26)]), 'run_1': ([(23,25,24,27)], [(30,24,30,25)]),
   'run_2': ([(23,26,24,28)], [(30,25,30,26)]), 'run_3': ([(23,26,24,28)], [(29,24,30,26)]),
   'jump_1': ([(22,27,23,29)], None), 'greet_0': ([(21,25,22,27)], [(29,27,29,27)])}},
}

def dark_mask(im):
    return (im[:, :, 3] > 0) & (np.abs(im[:, :, :3] - OUTLINE).sum(2) < 40)

def pixels_in(boxes, dark):
    s = set()
    for x0, y0, x1, y1 in boxes:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if dark[y, x]: s.add((y, x))
    return s

def patch_of(pix, im):
    ys = [p[0] for p in pix]; xs = [p[1] for p in pix]
    y0, y1, x0, x1 = min(ys), max(ys), min(xs), max(xs)
    m = np.zeros((y1 - y0 + 1, x1 - x0 + 1), bool)
    for y, x in pix: m[y - y0, x - x0] = True
    # enclosed light pixels (eye highlights) belong to the eye
    for yy in range(m.shape[0]):
        for xx in range(m.shape[1]):
            if not m[yy, xx] and m[yy, :xx].any() and m[yy, xx+1:].any() and m[:yy, xx].any() and m[yy+1:, xx].any():
                m[yy, xx] = True
    return im[y0:y1 + 1, x0:x1 + 1].copy(), m, (x0, y0)

def fill_face(out, pts, okmask):
    pts = set(pts)
    for _ in range(6):
        left = set()
        for y, x in pts:
            nb = [out[ny, nx, :3] for ny, nx in ((y,x-1),(y,x+1),(y-1,x),(y+1,x))
                  if 0 <= ny < 48 and 0 <= nx < 48 and okmask[ny, nx] and (ny, nx) not in pts]
            if nb: out[y, x, :3] = np.median(nb, 0).astype(int)
            else: left.add((y, x))
        for p in pts - left: okmask[p] = True
        pts = left
        if not pts: break

def place(out, okmask, patch, m, cx, top):
    h, w = m.shape
    for trim in range(0, 2):
        for dy in (0, 1, -1):
            for dx in (0, -1, 1):
                left = int(round(cx - (w - 1) / 2)) + dx; t = top + dy + trim
                cells = [(t + yy - trim, left + xx) for yy in range(trim, h) for xx in range(w) if m[yy, xx]]
                if all(0 <= Y < 48 and 0 <= X < 48 and okmask[Y, X] for Y, X in cells):
                    return left, t - trim, trim
    return None

report = {}
for cid, spec in SPEC.items():
    for f in ['idle_0','idle_1','run_0','run_1','run_2','run_3','jump_0','jump_1','land_0','fail_0','greet_0','home_0']:
        name = f'sprite_{cid}_{f}.png'
        im = np.array(Image.open(os.path.join(SRC, name)).convert('RGBA')).astype(int)
        if f not in spec['frames']:
            Image.fromarray(im.astype(np.uint8), 'RGBA').save(os.path.join(DST, name)); report[name] = 'unchanged (eyes already even or closed)'; continue
        fs = spec['frames'][f]
        near_b, far_b = fs[0], fs[1]; extra = fs[2] if len(fs) > 2 else []; mode = fs[3] if len(fs) > 3 else None
        dark = dark_mask(im)
        near = pixels_in(near_b, dark); far = pixels_in(far_b, dark) if far_b else set()
        ex = pixels_in(extra, dark)
        big = near if len(near) >= len(far) else far
        patch, m, (bx, by) = patch_of(big, im)
        if mode == 'run2w':
            # mint's 2-wide running eye (dark column + highlight), same height as this frame's eye
            ref = np.array(Image.open(os.path.join(SRC, 'sprite_mint_run_0.png')).convert('RGBA')).astype(int)
            patch = ref[22:27, 25:27].copy(); m = np.ones((5, 2), bool); by = min(p[0] for p in near)
        if mode == 'mirror':
            patch = patch[:, ::-1].copy(); m = m[:, ::-1].copy()
            m_near = m[:, ::-1].copy()
        if mode == 'arch':
            fillc = None
            arch = np.array([[0,1,1,0],[1,0,0,1],[1,0,0,1]], bool)
            patch = np.zeros((3, 4, 4), int); patch[arch] = [48, 25, 27, 255]; m = arch; by = 23
        out = im.copy()
        face = (im[:, :, 3] > 0) & ~dark
        okmask = face.copy()
        erase = set(near) | set(far) | ex
        for grp in (near, far):
            if grp:
                _p, _m, (_x, _y) = patch_of(grp, im)
                for yy in range(_m.shape[0]):
                    for xx in range(_m.shape[1]):
                        if _m[yy, xx]: erase.add((_y + yy, _x + xx))
        # also erase eye shading next to the eyes
        if spec['shade']:
            for boxes in (near_b, far_b or []):
                for x0, y0, x1, y1 in boxes:
                    for y in range(y0, y1 + 1):
                        for x in range(x0 - 1, x1 + 2):
                            if tuple(im[y, x, :3]) in [tuple(c) for c in spec['shade']]: erase.add((y, x))
        for p in erase: okmask[p] = False
        fill_face(out, erase, okmask)
        ncx = np.mean([p[1] for p in near]); fcx = np.mean([p[1] for p in far]) if far else ncx + spec['dx']
        if mode == 'arch': ncx, fcx = 24.5, 30.5
        if mode == 'mirror': fcx = fcx - 2.5
        if mode == 'run2w': ncx = max(p[1] for p in near) - 0.5; fcx = ncx + 5
        top = by
        res = []
        for k, cx in enumerate((ncx, fcx)):
            if mode == 'mirror' and k == 0:
                r = place(out, okmask, patch[:, ::-1], m[:, ::-1], cx, top)
            else:
                r = place(out, okmask, patch, m, cx, top)
            res.append(r)
        if None in res or res[0][2] != res[1][2]:
            # make both eyes the same (trimmed) size
            trim = 1
            m2 = m.copy(); m2[:trim] = False
            res = [place(out, okmask, patch, m2, cx, top) for cx in (ncx, fcx)]
            m = m2
        if None in res:
            Image.fromarray(im.astype(np.uint8), 'RGBA').save(os.path.join(DST, name)); report[name] = 'NO ROOM - unchanged'; continue
        for k, (left, t, _) in enumerate(res):
            P, M = (patch[:, ::-1], m[:, ::-1]) if (mode == 'mirror' and k == 0) else (patch, m)
            for yy in range(M.shape[0]):
                for xx in range(M.shape[1]):
                    if M[yy, xx]: out[t + yy, left + xx] = P[yy, xx]
        Image.fromarray(out.astype(np.uint8), 'RGBA').save(os.path.join(DST, name))
        report[name] = {'eye_px': int(m.sum()), 'near': [int(res[0][0]), int(res[0][1])], 'far': [int(res[1][0]), int(res[1][1])]}
json.dump(report, open('report.json', 'w'), indent=1, ensure_ascii=False)
for k, v in report.items():
    if v != 'unchanged (eyes already even or closed)': print(k, v)
