$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$folder = Join-Path $root 'app/src/main/res/drawable-nodpi'
$ids = @('mint','cheese','popo','lumi','nunu','piko')
$total = 0
foreach ($id in $ids) {
    $palette = [Collections.Generic.HashSet[int]]::new()
    $files = @(Get-ChildItem $folder -Filter "sprite_${id}_*.png")
    if ($files.Count -ne 12) { throw "${id}: expected 12 frames" }
    foreach ($file in $files) {
        $bitmap = [Drawing.Bitmap]::FromFile($file.FullName)
        try {
            if ($bitmap.Width -ne 48 -or $bitmap.Height -ne 48) { throw "Size: $file" }
            $bottom = -1
            for ($y=0; $y -lt 48; $y++) {
                for ($x=0; $x -lt 48; $x++) {
                    $pixel = $bitmap.GetPixel($x,$y)
                    if ($pixel.A -ne 0 -and $pixel.A -ne 255) { throw "Partial alpha: $file" }
                    if ($pixel.A -eq 255) { $bottom=$y; [void]$palette.Add($pixel.ToArgb()) }
                }
            }
            if ($bottom -ne 45) { throw "Baseline $bottom : $file" }
        } finally { $bitmap.Dispose() }
        $total++
    }
    if ($palette.Count -gt 16) { throw "Palette overflow: $id" }
    Write-Output "PASS ${id}: 12 frames, $($palette.Count) colors, alpha 0/255, baseline y45"
}
Write-Output "PASS $total sprite PNGs"
