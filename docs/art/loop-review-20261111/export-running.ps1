# Technical game export of the image-tool artwork. Inputs are never overwritten.
param([string]$OutputDirectory = "$PSScriptRoot/final")
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Drawing.Drawing2D;
public static class RunningExport {
 public static Bitmap Scale(Bitmap src, Rectangle rect, int floor) {
  int w=(int)Math.Round(rect.Width*256.0/floor);
  var b=new Bitmap(w,280); using(var g=Graphics.FromImage(b)){
   g.InterpolationMode=InterpolationMode.NearestNeighbor;g.PixelOffsetMode=PixelOffsetMode.Half;
   g.DrawImage(src,new Rectangle(0,0,w,256),new Rectangle(rect.X,rect.Y,rect.Width,floor),GraphicsUnit.Pixel);
   g.DrawImage(src,new Rectangle(0,256,w,24),new Rectangle(rect.X,rect.Y+floor,rect.Width,rect.Height-floor),GraphicsUnit.Pixel);
  }return b;
 }
 // A minimum-color-error cut follows the existing shapes instead of a vertical splice.
 public static Bitmap Join(Bitmap a,Bitmap b,int overlap) {
  int h=a.Height,w=overlap;var costs=new double[h,w];var prev=new int[h,w];
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   var c=a.GetPixel(a.Width-w+x,y);var d=b.GetPixel(x,y);
   double cost=(c.R-d.R)*(c.R-d.R)+(c.G-d.G)*(c.G-d.G)+(c.B-d.B)*(c.B-d.B);
   cost+=Math.Abs(x-w/2)*3;
   if(x<12||x>=w-12)cost+=100000;
   if(y==0){costs[y,x]=cost;continue;}
   double best=double.MaxValue;int bx=x;
   for(int z=Math.Max(0,x-1);z<=Math.Min(w-1,x+1);z++)if(costs[y-1,z]<best){best=costs[y-1,z];bx=z;}
   costs[y,x]=cost+best;prev[y,x]=bx;
  }
  var path=new int[h];int end=0;for(int x=1;x<w;x++)if(costs[h-1,x]<costs[h-1,end])end=x;
  for(int y=h-1;y>=0;y--){path[y]=end;end=prev[y,end];}
  var result=new Bitmap(a.Width+b.Width-w,h);using(var g=Graphics.FromImage(result)){g.DrawImageUnscaled(a,0,0);g.DrawImageUnscaled(b,a.Width-w,0);}
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   var c=a.GetPixel(a.Width-w+x,y);var d=b.GetPixel(x,y);
   double t=Math.Max(0,Math.Min(1,(x-path[y]+3)/6.0));
   result.SetPixel(a.Width-w+x,y,Color.FromArgb((int)(c.R*(1-t)+d.R*t),(int)(c.G*(1-t)+d.G*t),(int)(c.B*(1-t)+d.B*t)));
  }return result;
 }
 public static void Build(string main,string connector,int[] mr,int mf,int[] cr,int cf,string folder,string id){
  using(var m=new Bitmap(main))using(var c=new Bitmap(connector))
  using(var a=Scale(m,new Rectangle(mr[0],mr[1],mr[2],mr[3]),mf))
  using(var b=Scale(c,new Rectangle(cr[0],cr[1],cr[2],cr[3]),cf)){
   const int ov=160;
   using(var doubled=Join(b,b,ov)){
    // Central period has the same local image neighborhood at both ends.
    int period=b.Width-ov,start=(id=="popo"||id=="nunu")?700:ov;
    using(var loop=doubled.Clone(new Rectangle(start,0,period,280),PixelFormat.Format32bppArgb))
    using(var joined=Join(a,loop,ov)){
     using(var intro=joined.Clone(new Rectangle(0,0,a.Width,280),PixelFormat.Format32bppArgb))
      intro.Save(System.IO.Path.Combine(folder,id+"-intro.png"),ImageFormat.Png);
     // intro ends after loop's first overlap pixels: rotate to the following pixel.
     using(var rotated=new Bitmap(period,280)){
      using(var g=Graphics.FromImage(rotated)){
       g.DrawImage(loop,new Rectangle(0,0,period-ov,280),new Rectangle(ov,0,period-ov,280),GraphicsUnit.Pixel);
       g.DrawImage(loop,new Rectangle(period-ov,0,ov,280),new Rectangle(0,0,ov,280),GraphicsUnit.Pixel);
      }
      rotated.Save(System.IO.Path.Combine(folder,id+"-loop.png"),ImageFormat.Png);
     }
    }
   }
  }
 }
}
'@
[IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$main = Join-Path $PSScriptRoot '../background-source'
$attached=Join-Path $PSScriptRoot 'attached'
# Exclude the right-hand house/tree-home return approaches from EVERY loop crop.
$jobs=@(
 @{id='mint';pair='mint-cheese';mr=@(0,4,2169,319);mf=278;cr=@(0,4,1650,299);cf=264},
 @{id='cheese';pair='mint-cheese';mr=@(0,336,2169,385);mf=355;cr=@(0,315,1740,364);cf=343},
 @{id='popo';pair='popo-lumi';mr=@(4,4,2010,352);mf=324;cr=@(330,4,1320,329);cf=307},
 @{id='lumi';pair='popo-lumi';mr=@(4,368,2161,353);mf=329;cr=@(4,345,1580,334);cf=310},
 @{id='nunu';pair='nunu-piko';mr=@(4,4,2161,352);mf=323;cr=@(300,4,1320,330);cf=296},
 @{id='piko';pair='nunu-piko';mr=@(4,368,2161,353);mf=329;cr=@(4,346,1690,334);cf=310}
)
foreach($j in $jobs){[RunningExport]::Build("$main/$($j.pair).png","$attached/$($j.pair).png",$j.mr,$j.mf,$j.cr,$j.cf,$OutputDirectory,$j.id)}
$jobs | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 "$OutputDirectory/export-inputs.json"
