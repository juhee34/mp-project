using System;
using System.IO;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
using System.Collections.Generic;

// Windows System.Drawing export utility. No game logic or Git operations.
public static class ExportObstacleArt {
 public static void Run(string root) {
  string dir=Path.Combine(root,"docs/art/obstacles");
  Directory.CreateDirectory(Path.Combine(dir,"png"));
  string[] pairs={"mint-cheese","popo-lumi","nunu-piko"};
  string[] ids={"mint","cheese","popo","lumi","nunu","piko"};
  string[] names={"fern_log","ribbon_rock","map_crate","plant_pot","cushions","sun_tin","snow_rock","shell_basket","harbor_crate","moss_stump","pinecone_basket","music_box","books","return_crate","letter_basket","paper_crate","rope_spool","toolbox"};
  var rows=new List<string>();
  rows.Add("id,characterId,variant,source,cellX,cellY,cropX,cropY,cropW,cropH,x,y,w,h");
  using(var preview=new Bitmap(960,1440,PixelFormat.Format32bppArgb))
  using(var pg=Graphics.FromImage(preview))
  using(var font=new Font("Arial",11)) {
   pg.Clear(Color.FromArgb(245,241,228));
   pg.InterpolationMode=InterpolationMode.NearestNeighbor;
   pg.PixelOffsetMode=PixelOffsetMode.Half;
   for(int i=0;i<18;i++) {
    int chr=i/3,col=i%3,row=chr%2;
    string pair=pairs[chr/2];
    using(var src=new Bitmap(Path.Combine(dir,"source",pair+".png"))) {
     if(src.Width!=1536||src.Height!=1024) throw new Exception("Unexpected sheet size: "+pair);
     int minx=512,miny=512,maxx=-1,maxy=-1;
     for(int y=0;y<512;y++) for(int x=0;x<512;x++) {
      if(src.GetPixel(col*512+x,row*512+y).A<128) continue;
      minx=Math.Min(x,minx);maxx=Math.Max(x,maxx);miny=Math.Min(y,miny);maxy=Math.Max(y,maxy);
     }
     if(maxx<minx) throw new Exception("Empty cell");
     int cw=maxx-minx+1,ch=maxy-miny+1;
     double scale=Math.Min(44.0/cw,70.0/ch);
     int w=Math.Max(1,(int)Math.Round(cw*scale)),h=Math.Max(1,(int)Math.Round(ch*scale));
     int ox=(48-w)/2,oy=78-h;
     string id="obstacle_"+ids[chr]+"_"+names[i];
     using(var dst=new Bitmap(48,80,PixelFormat.Format32bppArgb)) {
      for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
       int sx=col*512+minx+Math.Min(cw-1,(int)((x+0.5)*cw/w));
       int sy=row*512+miny+Math.Min(ch-1,(int)((y+0.5)*ch/h));
       Color c=src.GetPixel(sx,sy);
       dst.SetPixel(ox+x,oy+y,c.A>=128?Color.FromArgb(255,c.R,c.G,c.B):Color.FromArgb(0,0,0,0));
      }
      // Drop detached sheet fragments; retain the main eight-connected sprite.
      var seen=new bool[48,80]; var largest=new List<Point>();
      for(int yy=0;yy<80;yy++)for(int xx=0;xx<48;xx++){
       if(seen[xx,yy]||dst.GetPixel(xx,yy).A==0)continue;
       var group=new List<Point>();var queue=new Queue<Point>();queue.Enqueue(new Point(xx,yy));seen[xx,yy]=true;
       while(queue.Count>0){var p=queue.Dequeue();group.Add(p);for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
        int nx=p.X+dx,ny=p.Y+dy;if(nx<0||nx>=48||ny<0||ny>=80||seen[nx,ny]||dst.GetPixel(nx,ny).A==0)continue;
        seen[nx,ny]=true;queue.Enqueue(new Point(nx,ny));
       }}
       if(group.Count>largest.Count)largest=group;
      }
      var keep=new bool[48,80];foreach(var p in largest)keep[p.X,p.Y]=true;
      for(int yy=0;yy<80;yy++)for(int xx=0;xx<48;xx++)if(!keep[xx,yy])dst.SetPixel(xx,yy,Color.FromArgb(0,0,0,0));
      // Record actual exported opaque bounds, not assumed resize bounds.
      int bx=48,by=80,ex=-1,ey=-1;
      for(int y=0;y<80;y++)for(int x=0;x<48;x++)if(dst.GetPixel(x,y).A==255){bx=Math.Min(bx,x);by=Math.Min(by,y);ex=Math.Max(ex,x);ey=Math.Max(ey,y);}
      if(ey!=77) throw new Exception("Baseline lost: "+id);
      dst.Save(Path.Combine(dir,"png",id+".png"),ImageFormat.Png);
      rows.Add(String.Join(",",new object[]{id,ids[chr],col,pair+".png",col*512,row*512,col*512+minx,row*512+miny,cw,ch,bx,by,ex-bx+1,ey-by+1}));
      int px=col*320,py=chr*240;
      pg.DrawString(ids[chr]+" / "+names[i],font,Brushes.Black,px+8,py+5);
      pg.FillRectangle(Brushes.White,px+4,py+30,154,205);
      pg.FillRectangle(Brushes.DimGray,px+162,py+30,154,205);
      // Tight source bounds at 3x for edge inspection, plus 1x at upper right.
      var rect=new Rectangle(bx,by,ex-bx+1,ey-by+1);
      pg.DrawImage(dst,new Rectangle(px+10,py+224-rect.Height*3,rect.Width*3,rect.Height*3),rect,GraphicsUnit.Pixel);
      pg.DrawImage(dst,new Rectangle(px+168,py+224-rect.Height*3,rect.Width*3,rect.Height*3),rect,GraphicsUnit.Pixel);
      pg.DrawImageUnscaled(dst,px+264,py+33);
     }
    }
   }
   preview.Save(Path.Combine(dir,"review.png"),ImageFormat.Png);
  }
  File.WriteAllLines(Path.Combine(dir,"export.csv"),rows);
 }
}
