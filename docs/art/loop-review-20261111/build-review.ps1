Add-Type -AssemblyName System.Drawing
$root = $PSScriptRoot
$pairs = @('mint-cheese','popo-lumi','nunu-piko')
$ids = @('mint','cheese','popo','lumi','nunu','piko')
$sourceRects = @(@(0,4,2169,319),@(0,336,2169,385),@(4,4,2161,352),@(4,368,2161,353),@(4,4,2161,352),@(4,368,2161,353))
$newRects = @(@(0,4,2048,299),@(0,315,2048,364),@(4,4,2040,329),@(4,345,2040,334),@(4,4,2040,330),@(4,346,2040,334))
$sheet = [Drawing.Bitmap]::new(1000,1380)
$g = [Drawing.Graphics]::FromImage($sheet)
$g.Clear([Drawing.Color]::FromArgb(248,242,230))
$font = [Drawing.Font]::new('Arial',12)
$pen = [Drawing.Pen]::new([Drawing.Color]::Magenta,1)
$records = @()
for ($i=0; $i -lt 6; $i++) {
  $pair=$pairs[[int][Math]::Floor($i/2)]
  $original=[Drawing.Bitmap]::FromFile((Join-Path $root "../background-source/$pair.png"))
  $revised=[Drawing.Bitmap]::FromFile((Join-Path $root "attached/$pair.png"))
  $s=$sourceRects[$i]; $n=$newRects[$i]
  # Comparison only: equal width scale and bottom alignment; no edits to input art.
  $scale=$n[2]/$s[2]
  $h=[int][Math]::Round($s[3]*$scale)
  $normalized=[Drawing.Bitmap]::new($n[2],$h)
  $ng=[Drawing.Graphics]::FromImage($normalized)
  $ng.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
  $ng.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
  $ng.DrawImage($original,[Drawing.Rectangle]::new(0,0,$n[2],$h),$s[0],$s[1],$s[2],$s[3],[Drawing.GraphicsUnit]::Pixel)
  $ng.Dispose()
  $top=$i*230+27
  $g.DrawString("$($ids[$i]): MAIN > CONNECTOR",$font,[Drawing.Brushes]::Black,10,$top-24)
  $g.DrawString("$($ids[$i]): CONNECTOR > MAIN",$font,[Drawing.Brushes]::Black,510,$top-24)
  $g.DrawImage($normalized,[Drawing.Rectangle]::new(10,$top,240,200),$normalized.Width-240,$h-200,240,200,[Drawing.GraphicsUnit]::Pixel)
  $g.DrawImage($revised,[Drawing.Rectangle]::new(250,$top,240,200),$n[0],$n[1]+$n[3]-200,240,200,[Drawing.GraphicsUnit]::Pixel)
  $g.DrawImage($revised,[Drawing.Rectangle]::new(510,$top,240,200),$n[0]+$n[2]-240,$n[1]+$n[3]-200,240,200,[Drawing.GraphicsUnit]::Pixel)
  $g.DrawImage($normalized,[Drawing.Rectangle]::new(750,$top,240,200),0,$h-200,240,200,[Drawing.GraphicsUnit]::Pixel)
  $g.DrawLine($pen,250,$top,250,$top+200)
  $g.DrawLine($pen,750,$top,750,$top+200)
  $records += @{id=$ids[$i]; pair=$pair; sourceRect=$s; connectorRect=$n; comparisonScale=$scale; normalizedSourceHeight=$h; cropStatus='review_only'; loopReady=$false}
  $original.Dispose();$revised.Dispose();$normalized.Dispose()
}
$sheet.Save((Join-Path $root 'seams.png'),[Drawing.Imaging.ImageFormat]::Png)
$g.Dispose();$sheet.Dispose();$font.Dispose();$pen.Dispose()
$records | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $root 'review-rects.json')
