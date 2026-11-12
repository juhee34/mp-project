"""World thumbnail: remove the mockup's six characters (keep sky, trees, grass, campfire) and seat the six
original idle sprites around the fire."""
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
out = rowfill(img, m, valid=valid, std=3.0)
# soften the horizontal streaks a little, only inside the mask
blur = cv2.GaussianBlur(out, (1, 9), 0)
out[m > 0] = blur[m > 0]
n = noise(H, W, 3.0)
out = np.clip(out.astype(np.float32) + n * (m > 0)[..., None], 0, 255).astype(np.uint8)
canvas = Image.fromarray(cv2.cvtColor(out, cv2.COLOR_BGR2RGB)).convert('RGBA')
SEATS = [('nunu', 74, 150), ('lumi', 166, 174), ('popo', 252, 162),       # back, on the left/right of the fire
         ('cheese', 126, 238), ('mint', 56, 266), ('piko', 266, 268)]     # front
for sid, cx, foot in SEATS:
    sc = 2 if sid in ('nunu', 'lumi', 'popo', 'cheese') else 3
    s = Image.open(f'orig/sprite_{sid}_idle_0.png').convert('RGBA').resize((48 * sc, 48 * sc), Image.NEAREST)
    canvas.alpha_composite(s, (int(cx - 22 * sc), int(foot - 46 * sc)))
res = np.dstack([cv2.cvtColor(np.array(canvas.convert('RGB')), cv2.COLOR_RGB2BGR), alpha])
cv2.imwrite('out/parts/story_thumb_world.png', res)
cv2.imwrite('z_world.png', cv2.resize(np.hstack([img, res[:, :, :3]]), None, fx=1.5, fy=1.5, interpolation=cv2.INTER_NEAREST))
