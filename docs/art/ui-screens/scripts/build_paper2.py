"""Story-collection background v2: the mockup's own scene (frame, clouds, trees, bushes, bench, grass) with
the title, top widgets and cards removed. Hidden foliage is completed by mirroring the visible part (so it ends
in a bumpy edge instead of a straight cut or a blur); the 20:9 version widens the picture in the middle so the
wooden frame stays at the screen edges."""
import cv2, numpy as np, sys, os
sys.path.insert(0, '.')
from lib import rowfill, feather, noise
im = cv2.imread('src/stories.png'); H, W = im.shape[:2]
CREAM = np.array([215, 239, 253], np.float32)        # BGR of the paper
f = im.astype(np.float32)
hsv = cv2.cvtColor(im, cv2.COLOR_BGR2HSV)
def foliage(y, x):
    b, g, r = im[y, x].astype(int)
    return g > r - 5 and g > b + 25 and hsv[y, x, 1] > 50
# 1) title + top widgets out (row fill), as before
d = np.sqrt(((f - CREAM) ** 2).sum(2))
m = np.zeros((H, W), np.uint8); hit = d > 30
m[25:146, 488:1188] = hit[25:146, 488:1188]; m[38:118, 1538:1552] = hit[38:118, 1538:1552]
m = cv2.dilate(m.astype(np.uint8) * 255, np.ones((9, 9), np.uint8)); m[127:, 430:520] = 0
for b in [(47, 41, 371, 112), (1233, 41, 1526, 112), (1562, 34, 1637, 120)]:
    cv2.rectangle(m, (b[0] - 4, b[1] - 4), (b[2] + 4, b[3] + 4), 255, -1)
base = feather(im, rowfill(im, m, std=1.2), m, 5).astype(np.float32)
# 2) card area -> paper
X0, Y0, X1, Y1 = 62, 216, 1622, 764
out = base.copy()
out[Y0:Y1, X0:X1] = CREAM + noise(Y1 - Y0, X1 - X0, 1.4)
# bench rows under the cards: each row's wood colour sampled in the gaps between the cards
nz = noise(H, W, 2.0)
for y in range(764, 800):
    c = np.median(np.concatenate([f[y, g - 2:g + 3] for g in (449, 836, 1221)]), 0)
    out[y, X0 - 6:X1 + 6] = c + nz[y, X0 - 6:X1 + 6]
# 3) top foliage (tree crown left, bushes right): mirror the visible part below the cut row
for x in range(X0, X1):
    if not foliage(Y0 - 3, x): continue
    top = Y0 - 3
    while top > 0 and foliage(top - 1, x): top -= 1
    depth = min(Y0 - top, 90)
    for k in range(depth):
        out[Y0 + k, x] = base[Y0 - 1 - k, x]
# 4) side foliage: mirror columns inward with a bumpy edge
yy = np.arange(H)
for side in ('L', 'R'):
    for y in range(Y0, Y1):
        edge = X0 if side == 'L' else X1 - 1
        if not foliage(y, edge - 3 if side == 'L' else edge + 3): continue
        depth = int(34 + 10 * np.sin(y / 21.0) + 6 * np.sin(y / 7.3 + 1))
        for k in range(depth):
            if side == 'L': out[y, X0 + k] = base[y, X0 - 1 - k % (X0 - 12)]
            else: out[y, X1 - 1 - k] = base[y, X1 + k % (W - 12 - X1)]
out = np.clip(out, 0, 255).astype(np.uint8)
cv2.imwrite('out/backgrounds/bg_common_paper.png', out)
# 5) 20:9: insert a copy of the middle band so the frame stays at the edges
TW = 2091; add = TW - W; mid = W // 2
band = out[:, mid - add // 2: mid - add // 2 + add]
wide = np.concatenate([out[:, :mid], band, out[:, mid:]], 1).astype(np.float32)
for seam in (mid, mid + add):                      # 24 px crossfade at both seams
    for i in range(-12, 12):
        t = (i + 12) / 24
        a = wide[:, seam + i].copy()
        src = out[:, (mid - add // 2 + (i if seam == mid else add + i)) % W] if False else None
    wide[:, mid - 12: mid + 12] = cv2.GaussianBlur(wide[:, mid - 14: mid + 14], (9, 1), 0)[:, 2:-2]
    wide[:, mid + add - 12: mid + add + 12] = cv2.GaussianBlur(wide[:, mid + add - 14: mid + add + 14], (9, 1), 0)[:, 2:-2]
wide = np.clip(wide, 0, 255).astype(np.uint8)
cv2.imwrite('out/backgrounds/bg_common_paper_wide.png', wide)
cv2.imwrite('out/backgrounds/bg_common_paper_wide.jpg', wide, [cv2.IMWRITE_JPEG_QUALITY, 92])
cv2.imwrite('../z_cp2.png', cv2.resize(wide, (1045, 470)))
