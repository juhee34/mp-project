"""Replace the mockup-drawn characters in the story thumbnails with the original 48x48 idle sprites.
Houses and scenery stay as in the approved mockup."""
import cv2, numpy as np, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib import noise

# (sprite id, character box in the thumbnail x0,y0,x1,y1, feet y, centre x, scale)
JOBS = {
    'mint':   [('mint',   (4, 138, 110, 248), 244, 60, 3)],
    'cheese': [('cheese', (10, 148, 128, 258), 252, 68, 3)],
    'popo':   [('popo',   (18, 146, 120, 252), 246, 68, 3)],
    'world':  [('nunu',   (36, 66, 116, 146), 140, 76, 2),
               ('lumi',   (128, 64, 222, 172), 166, 176, 2),
               ('popo',   (210, 100, 282, 204), 198, 248, 2),
               ('mint',   (10, 146, 110, 262), 256, 62, 3),
               ('cheese', (90, 136, 172, 232), 226, 132, 3),
               ('piko',   (218, 166, 296, 270), 264, 256, 3)],
}

def grab(img, box):
    m = np.zeros(img.shape[:2], np.uint8)
    bgd = np.zeros((1, 65), np.float64); fgd = np.zeros((1, 65), np.float64)
    x0, y0, x1, y1 = box
    cv2.grabCut(img, m, (x0, y0, x1 - x0, y1 - y0), bgd, fgd, 6, cv2.GC_INIT_WITH_RECT)
    return np.where((m == cv2.GC_FGD) | (m == cv2.GC_PR_FGD), 255, 0).astype(np.uint8)

for name, jobs in JOBS.items():
    src = cv2.imread(f'parts_ai/story_thumb_{name}.png', cv2.IMREAD_UNCHANGED)
    if src.shape[2] == 4 and os.path.exists(f'parts_ai/story_thumb_{name}.png'):
        pass
    img = src[:, :, :3].copy(); alpha = src[:, :, 3]
    H, W = img.shape[:2]
    mask = np.zeros((H, W), np.uint8)
    for sid, box, foot, cx, sc in jobs:
        mask = np.maximum(mask, grab(img, box))
    mask = cv2.dilate(mask, np.ones((7, 7), np.uint8))
    small = cv2.resize(img, None, fx=.5, fy=.5, interpolation=cv2.INTER_AREA)
    ms = cv2.resize(mask, (small.shape[1], small.shape[0]), interpolation=cv2.INTER_NEAREST)
    fill = cv2.resize(cv2.inpaint(small, ms, 7, cv2.INPAINT_TELEA), (W, H), interpolation=cv2.INTER_CUBIC).astype(np.float32)
    fill += noise(H, W, 2.5)
    a = cv2.GaussianBlur((mask > 0).astype(np.float32), (5, 5), 0)[..., None]
    out = np.clip(img * (1 - a) + fill * a, 0, 255).astype(np.uint8)
    cv2.imwrite(f'z_mask_{name}.png', np.hstack([img, cv2.cvtColor(mask, cv2.COLOR_GRAY2BGR), out]))
    canvas = Image.fromarray(cv2.cvtColor(out, cv2.COLOR_BGR2RGB)).convert('RGBA')
    for sid, box, foot, cx, sc in jobs:
        s = Image.open(f'orig/sprite_{sid}_idle_0.png').convert('RGBA')
        s = s.resize((48 * sc, 48 * sc), Image.NEAREST)
        # sprite feet are at row 45 of 48, centre at x ~ 22
        left = int(cx - 22 * sc); top = int(foot - 46 * sc)
        canvas.alpha_composite(s, (left, top))
    res = np.dstack([cv2.cvtColor(np.array(canvas.convert('RGB')), cv2.COLOR_RGB2BGR), alpha])
    cv2.imwrite(f'out/parts/story_thumb_{name}.png', res)
print('ok')
