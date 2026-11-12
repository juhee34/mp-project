"""bg_common_paper: the story-collection scene without its title, for the shared screens."""
import cv2, numpy as np, sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import *
im = cv2.imread('out/backgrounds/bg_story_collection.png'); H, W = im.shape[:2]
CREAM = np.array([215, 239, 253])
d = np.sqrt(((im.astype(int) - CREAM) ** 2).sum(2))
m = np.zeros((H, W), np.uint8); hit = (d > 30)
m[25:146, 488:1188] = hit[25:146, 488:1188]; m[38:118, 1538:1552] = hit[38:118, 1538:1552]
m = cv2.dilate(m * 255, np.ones((9, 9), np.uint8)); m[127:, 430:520] = 0
base = feather(im, rowfill(im, m, std=1.3), m, 5)
X0, Y0, X1, Y1 = 60, 226, 1622, 763                      # where the cards were
band = np.zeros((H, W), np.uint8); cv2.rectangle(band, (X0, Y0), (X1, Y1), 255, -1)
small = cv2.resize(base, None, fx=.25, fy=.25, interpolation=cv2.INTER_AREA)
ms = cv2.resize(band, None, fx=.25, fy=.25, interpolation=cv2.INTER_NEAREST)
smear = cv2.resize(cv2.inpaint(small, ms, 6, cv2.INPAINT_TELEA), (W, H), interpolation=cv2.INTER_CUBIC).astype(np.float32)
yy, xx = np.mgrid[0:H, 0:W]
a = np.clip(np.minimum(np.minimum(xx - X0, X1 - xx), yy - Y0) / 70.0, 0, 1)[..., None]
cream = np.zeros_like(smear) + CREAM.astype(np.float32)
bottom = np.clip((Y1 - yy) / 60.0, 0, 1)[..., None]
smear = smear * bottom + cream * (1 - bottom)
fill = smear * (1 - a) + cream * a + noise(H, W, 1.6)
out = base.astype(np.float32); out[band > 0] = fill[band > 0]; out = np.clip(out, 0, 255).astype(np.uint8)
cv2.imwrite('out/backgrounds/bg_common_paper.png', out)
TW = 2091; pl = (TW - W) // 2
lc = np.median(out[:, 2:8].reshape(-1, 3), 0); rc = np.median(out[:, W - 8:W - 2].reshape(-1, 3), 0)
w = np.zeros((H, TW, 3), np.float32); w[:, :pl] = lc; w[:, pl + W:] = rc; w += noise(H, TW, 1.5); w[:, pl:pl + W] = out
cv2.imwrite('out/backgrounds/bg_common_paper_wide.png', np.clip(w, 0, 255).astype(np.uint8))
