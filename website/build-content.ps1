$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path $PSScriptRoot -Parent
$backgroundRoot = Join-Path $projectRoot 'app/src/main/assets/backgrounds'
$assetRoot = Join-Path $PSScriptRoot 'dist/assets'
$manifest = Get-Content (Join-Path $backgroundRoot 'manifest.json') -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($character in $manifest.characters) {
    $sourceImage = [System.Drawing.Bitmap]::new((Join-Path $backgroundRoot $character.source))
    try {
        $r = $character.rect
        $region = [System.Drawing.Rectangle]::new($r[0], $r[1], $r[2], $r[3])
        $crop = $sourceImage.Clone($region, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try { $crop.Save((Join-Path $assetRoot ($character.id + '-home.png')), [System.Drawing.Imaging.ImageFormat]::Png) }
        finally { $crop.Dispose() }
    } finally { $sourceImage.Dispose() }
}
python (Join-Path $PSScriptRoot 'build-content.py')
if ($LASTEXITCODE -ne 0) { throw 'Content export failed' }
