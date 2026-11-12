import cv2, numpy as np, json, sys
BOX = {
 'login': {'mint': (256,630,420,815), 'cheese': (410,650,566,815), 'popo': (556,650,700,815),
           'lumi': (1396,640,1560,815), 'nunu': (1546,660,1676,815), 'piko': (1676,640,1830,815)},
 'menu':  {'mint': (306,600,480,795), 'cheese': (480,620,660,795), 'popo': (650,620,800,795),
           'lumi': (1290,610,1466,795), 'nunu': (1460,640,1590,795), 'piko': (1600,610,1772,795)},
}
def segment(img, box):
    x0,y0,x1,y1 = box
    m = np.zeros(img.shape[:2], np.uint8); b=np.zeros((1,65)); f=np.zeros((1,65))
    cv2.grabCut(img, m, (x0,y0,x1-x0,y1-y0), b, f, 8, cv2.GC_INIT_WITH_RECT)
    fg = np.where((m==1)|(m==3),255,0).astype(np.uint8)
    n,lab,st,_ = cv2.connectedComponentsWithStats(fg,8)
    if n>1:
        i = 1+np.argmax(st[1:,4]); fg = (lab==i).astype(np.uint8)*255
    return fg
if __name__ == '__main__':
    name = sys.argv[1]; src = {'login':'bg_login_wide.jpg','menu':'bg_main_menu_wide.jpg'}[name]
    img = cv2.imread(src); vis = img.copy(); res={}
    for cid, box in BOX[name].items():
        fg = segment(img, box); ys,xs = np.where(fg>0)
        res[cid] = [int(xs.min()),int(ys.min()),int(xs.max()),int(ys.max())]
        vis[fg>0] = (vis[fg>0]*0.4 + np.array([255,0,255])*0.6).astype(np.uint8)
        cv2.rectangle(vis, box[:2], box[2:], (0,0,255), 2)
    print(json.dumps(res))
    cv2.imwrite(f'z_seg_{name}.png', cv2.resize(vis[560:860, 200:1900], None, fx=0.6, fy=0.6))
