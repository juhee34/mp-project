"""World thumbnail: remove the mockup's six characters (keep sky, trees, grass, campfire) and seat the six
original idle sprites on the grass around the fire (back row behind the fire, front row in front)."""
import cv2, numpy as np, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import rowfill, noise
src = cv2.imread('parts_ai/story_thumb_world.png', cv2.IMREAD_UNCHANGED)
img, alpha = src[:, :, :3].copy(), src[:, :, 3]
H, W = img.shape[:2]
m = np.zeros((H, W), np.uint8)
for b in [(36, 64, 124, 152), (138, 60, 218, 178), (210, 98, 292, 208), (6, 148, 112, 268), (93, 138, 178, 244), (218, 166, 302, 270)]:
    cv2.rectangle(m, b[:2], b[2:], 255, -1)
fire = np.zeros_like(m); cv2.ellipse(fire, (186, 228), (42, 40), 0, 0, 360, 255, -1)
m[fire > 0] = 0
valid = alpha > 200
low = rowfill(img, m, valid=valid, std=0).astype(np.float32)
low = cv2.GaussianBlur(low, (1, 11), 0)
# texture detail borrowed from the visible grass strip at the bottom and the tree band at the left edge
f = img.astype(np.float32)
detail = f - cv2.GaussianBlur(f, (0, 0), 3)
tex = np.zeros_like(f)
for y in range(H):
    for x in range(W):
        if m[y, x]:
            if y >= 160:
                sy = 268 + (y * 3) % 12; sx = (x + y * 37) % (W - 20) + 10
                tex[y, x] = detail[sy, sx] * min(1.0, (y - 160) / 20)
out = f.copy()
out[m > 0] = (low + tex * 0.9)[m > 0]
a = cv2.GaussianBlur((m > 0).astype(np.float32), (5, 5), 0)[..., None]
out = np.clip(f * (1 - a) + out * a, 0, 255).astype(np.uint8)
canvas = Image.fromarray(cv2.cvtColor(out, cv2.COLOR_BGR2RGB)).convert('RGBA')
# (sprite, centre x, feet y, scale) – back row first so the front row overlaps it
SEATS = [('nunu', 62, 198, 2), ('lumi', 142, 192, 2), ('popo', 244, 196, 2),
         ('mint', 50, 266, 2), ('cheese', 116, 268, 2), ('piko', 262, 266, 2)]
for sid, cx, foot, sc in SEATS:
    s = Image.open(f'orig/sprite_{sid}_idle_0.png').convert('RGBA').resize((48 * sc, 48 * sc), Image.NEAREST)
    canvas.alpha_composite(s, (int(cx - 22 * sc), int(foot - 46 * sc)))
res = np.dstack([cv2.cvtColor(np.array(canvas.convert('RGB')), cv2.COLOR_RGB2BGR), alpha])
cv2.imwrite('out/parts/story_thumb_world.png', res)
cv2.imwrite('z_world.png', cv2.resize(np.hstack([img, res[:, :, :3]]), None, fx=1.5, fy=1.5, interpolation=cv2.INTER_NEAREST))
