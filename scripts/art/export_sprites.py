from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np
from scipy.ndimage import label, find_objects
import json, hashlib
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'docs/art/source'
OUT=ROOT/'app/src/main/res/drawable-nodpi'
ORDER=['greet_0','idle_0','idle_1','run_0','run_1','run_2','run_3','jump_0','jump_1','land_0','fail_0','home_0']
REQUIRED=[1,2,3,4,5,6,7,8,10]
PAIRS=[('mint-cheese',['mint','cheese']),('popo-lumi',['popo','lumi']),('nunu-piko',['nunu','piko'])]
report={'frames':[], 'sources':[], 'notes':['Source expressions and poses retained. Detached motion marks excluded from gameplay silhouettes.','Uniform scale within each character; y45 bottom alignment includes jumps.','Optional greet/home contain one frame only; they are not complete two-frame animations.','App integration and collision-body alignment require engine review.']}
allframes={}
for prefix,ids in PAIRS:
 p=next(SRC.glob(prefix+'*.png')); rgba=np.array(Image.open(p).convert('RGBA'))
 labs,n=label(rgba[:,:,3]>=200); counts=np.bincount(labs.ravel()); boxes=find_objects(labs)
 comps=[]
 for k in range(1,n+1):
  if counts[k]>4000:
   sy,sx=boxes[k-1]; comps.append((k,(sx.start,sy.start,sx.stop,sy.stop)))
 assert len(comps)==24,(p,len(comps))
 # Row identity from component center, not hard crop boundaries: Lail feet cross 256px boundaries.
 comps.sort(key=lambda z:(int(((z[1][1]+z[1][3])/2)//256),z[1][0]))
 report['sources'].append({'file':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'size':list(rgba.shape[:2][::-1])})
 for group,cid in enumerate(ids):
  cs=comps[group*12:group*12+12]
  idle=cs[1][1]; scale=32/(idle[3]-idle[1])
  frames=[]
  for idx,(k,box) in enumerate(cs):
   x0,y0,x1,y1=box; crop=rgba[y0:y1,x0:x1].copy(); mask=labs[y0:y1,x0:x1]==k
   crop[:,:,3]=np.where(mask,255,0); crop[~mask,:3]=0
   size=(round((x1-x0)*scale),round((y1-y0)*scale))
   im=Image.fromarray(crop).resize(size,Image.Resampling.NEAREST)
   a=np.array(im); b=Image.fromarray(a[:,:,3]).getbbox(); im=im.crop(b)
   # Centre torso rather than the full silhouette, which includes a leftward tail.
   anchor={'mint':.57,'cheese':.60,'popo':.55,'lumi':.60,'nunu':.52,'piko':.54}[cid]
   left=24-round(im.width*anchor); top=46-im.height
   assert left>=1 and left+im.width<=47 and top>=1,(cid,idx,left,top,im.size)
   frame=Image.new('RGBA',(48,48)); frame.paste(im,(left,top))
   frames.append(frame)
  # One deterministic 16-colour palette for all frames of a character, without dithering.
  pixels=np.concatenate([np.array(f)[np.array(f)[:,:,3]>0,:3] for f in frames])
  colors_by_id={
   'mint':'30191b 71304d 9f456e b58aca cfb1e5 fff0ce d9bc88 2266b4 4895df 224979 f18caa fff9e9 815b43 a47b55 719e58 f5c04b',
   'cheese':'30191b 744327 a96b31 ed8e2c ffa942 ffc064 ffe0a4 fff0ce fff9e8 338650 6daf71 205a3b f18ca3 d36678 dbac49 a97e31',
   'popo':'30191b 28374f 3b4f75 586c93 8096b6 fff0ce fff9e8 dcc59c eb984d f5ba72 b56737 8c5739 b18bd9 d6b4f3 79539e ef9fa5',
   'lumi':'30191b 784329 b76b3f ec9861 f5b27d fff0ce fff9e8 d7bb96 a83239 d34e4f ec7870 754b36 a47650 c79562 f0c86c ef9d9e',
   'nunu':'30191b 68505e fff8ec fff0dc e9d8ed cab4df a389bc 896494 69496d 6bb99a a0d6bf 397c6a f0a7b7 d48b9e b591c6 d5b6e7',
   'piko':'30191b 655140 f3c942 ffde5d ffe994 f9f1d5 fffbed dd8a32 f7aa41 2e507f 4677b4 709bd7 a5c3ea 243c60 e9a591 b17a37'
  }
  from PIL import ImageColor
  palcolors=[ImageColor.getrgb('#'+h) for h in colors_by_id[cid].split()]
  palette=Image.new('P',(1,1)); palette.putpalette([v for c in (palcolors*16) for v in c])
  for idx,f in enumerate(frames):
   alpha=f.getchannel('A'); rgb=f.convert('RGB').quantize(palette=palette,dither=Image.Dither.NONE).convert('RGB'); rgb.putalpha(alpha)
   rgb.save(OUT/f'sprite_{cid}_{ORDER[idx]}.png')
   frames[idx]=rgb
   a=np.array(rgb); ys,xs=np.where(a[:,:,3]>0)
   report['frames'].append({'character':cid,'pose':ORDER[idx],'required':idx in REQUIRED,'sourceBox':cs[idx][1],'scale':scale,'bbox':[int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1)],'colors':len(np.unique(a[a[:,:,3]>0,:3],axis=0)),'alpha':np.unique(a[:,:,3]).tolist()})
  allframes[cid]=frames
# Read exported bytes back and enforce actual output contracts.
for f in OUT.glob('*.png'):
 im=Image.open(f); a=np.array(im); assert im.mode=='RGBA' and im.size==(48,48)
 assert set(np.unique(a[:,:,3]))=={0,255}
 assert not a[46:,:,3].any() and a[45,:,3].any(),f
for cid,frames in allframes.items():
 colors=set()
 for f in frames: colors.update(map(tuple,np.array(f)[np.array(f)[:,:,3]>0,:3]))
 assert len(colors)<=16,(cid,len(colors))
report['validation']={'characters':6,'required_frames':54,'optional_single_pose_frames':18,'total_frames':72,'size':'48x48 RGBA','alpha':'0 or 255','bottom_pixel_y':45,'empty_rows':[46,47],'palette':'16 colors or fewer per character','runtime_tested':False}
(ROOT/'docs/art/review/validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
# Nearest-neighbour review board with a visible baseline, not baked into game assets.
board=Image.new('RGB',(9*202+100,6*230+70),'#f7f2e8'); d=ImageDraw.Draw(board)
for j,i in enumerate(REQUIRED):d.text((110+j*202,15),ORDER[i],fill='#302b30')
for row,(cid,frames) in enumerate(allframes.items()):
 d.text((10,100+row*230),cid,fill='#302b30')
 for col,idx in enumerate(REQUIRED):
  x,y=100+col*202,40+row*230
  for yy in range(0,192,16):
   for xx in range(0,192,16):d.rectangle((x+xx,y+yy,x+xx+15,y+yy+15),fill='#dedee2' if (xx//16+yy//16)%2 else '#eeeeef')
  board.paste(frames[idx].resize((192,192),Image.Resampling.NEAREST),(x,y),frames[idx].resize((192,192),Image.Resampling.NEAREST))
  d.line((x,y+184,x+191,y+184),fill='#dd8c6f')
board.save(ROOT/'docs/art/review/contact-sheet.png')
# Combined animation for idle/run/jump/fail visual review on a steady ground line.
seq=[1]*5+[2]*5+[3,4,5,6]*3+[7]*4+[8]*4+[10]*7
anim=[]
for idx in seq:
 canvas=Image.new('RGB',(6*200,220),'#f7f2e8'); d=ImageDraw.Draw(canvas)
 for c,(cid,frames) in enumerate(allframes.items()):
  x=c*200+4; d.text((x+12,7),cid,fill='#302b30'); im=frames[idx].resize((192,192),Image.Resampling.NEAREST);canvas.paste(im,(x,20),im);d.line((x,204,x+191,204),fill='#c7b3a1')
 anim.append(canvas)
anim[0].save(ROOT/'docs/art/review/animation.gif',save_all=True,append_images=anim[1:],duration=100,loop=0)
print(json.dumps(report['validation']))
