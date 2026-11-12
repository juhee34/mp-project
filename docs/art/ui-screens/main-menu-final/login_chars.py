"""Login/opening picture: replace the mockup-drawn six friends with the game's 48x48 sprites (x5, crisp)."""
import cv2, numpy as np, sys, os
from PIL import Image
sys.path.insert(0, '../ui'); sys.path.insert(0, '.')
from lib import rowfill, noise
from chars import BOX, segment
SPR = '../eyes/fixed'
img = cv2.imread('bg_login_wide.jpg'); H, W = img.shape[:2]
mask = np.zeros((H, W), np.uint8); place = {}
for cid, box in BOX['login'].items():
    fg = segment(img, box)
    # cut anything grabCut pulled in above the character's head row band
    ys, xs = np.where(fg > 0)
    place[cid] = ((xs.min() + xs.max()) / 2, ys.max())
    fg = cv2.dilate(fg, np.ones((21, 21), np.uint8))
    mask = np.maximum(mask, fg)
f = img.astype(np.float32)
low = cv2.GaussianBlur(rowfill(img, mask, std=0).astype(np.float32), (1, 15), 0)
detail = f - cv2.GaussianBlur(f, (0, 0), 3)
tex = np.zeros_like(f)
ys, xs = np.where(mask > 0)
tex[ys, xs] = detail[np.clip(ys + 0, 0, H - 1), (xs + 260) % W]   # grass grain from beside the gap
out = f.copy(); out[mask > 0] = (low + noise(H, W, 3.0))[mask > 0]
a = cv2.GaussianBlur((mask > 0).astype(np.float32), (7, 7), 0)[..., None]
out = np.clip(f * (1 - a) + out * a, 0, 255).astype(np.uint8)
canvas = Image.fromarray(cv2.cvtColor(out, cv2.COLOR_BGR2RGB)).convert('RGBA')
SC = 4
for cid, (cx, foot) in place.items():
    s = Image.open(f'{SPR}/sprite_{cid}_idle_0.png').convert('RGBA').resize((48 * SC, 48 * SC), Image.NEAREST)
    canvas.alpha_composite(s, (int(round(cx - 22 * SC)), int(foot - 46 * SC)))
res = cv2.cvtColor(np.array(canvas.convert('RGB')), cv2.COLOR_RGB2BGR)
os.makedirs('out', exist_ok=True)
cv2.imwrite('out/bg_login_wide.jpg', res, [cv2.IMWRITE_JPEG_QUALITY, 92])
cv2.imwrite('out/bg_login_wide.png', res)
both = np.vstack([img, res]); cv2.imwrite('z_login_cmp.png', cv2.resize(both[:, :], None, fx=0.45, fy=0.45))
print(place)
