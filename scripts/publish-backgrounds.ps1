$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)

function Invoke-Git {
    & git @args
    if ($LASTEXITCODE -ne 0) { throw "git failed ($LASTEXITCODE). Stopped; no force push." }
}

$branch = & git branch --show-current
if ($branch -ne 'codex/character-screens') { throw "Expected codex/character-screens; found $branch" }
$staged = & git diff --cached --name-only
if ($LASTEXITCODE -ne 0) { throw 'Could not inspect staged files.' }
if ($staged) { throw 'Existing staged changes found. Review them before publishing this batch.' }

Invoke-Git diff --check
& node scripts/validate-content.cjs
if ($LASTEXITCODE -ne 0) { throw 'Content validation failed.' }

$files = @(
    'PROJECT_STATE.md',
    'app/src/main/assets/content/assets.json',
    'app/src/main/assets/backgrounds',
    'app/src/main/java/com/tinyrunners/school/game/RunnerView.kt',
    'app/src/main/java/com/tinyrunners/school/game/BackgroundMotion.kt',
    'app/src/main/java/com/tinyrunners/school/game/StoryBackground.kt',
    'app/src/test/java/com/tinyrunners/school/game/BackgroundMotionTest.kt',
    'docs/DATA_CONTRACT.md',
    'docs/art/BACKGROUND_HANDOFF.md',
    'docs/art/background-manifest.json',
    'docs/art/background-source',
    'docs/art/review/background-preview.html',
    'scripts/background-preview.template.html',
    'scripts/build-background-preview.py',
    'scripts/validate-content.cjs',
    'scripts/publish-backgrounds.ps1'
)
Invoke-Git add -- @files
Invoke-Git diff --cached --stat
Invoke-Git commit -m 'Integrate six approved story backgrounds with distance-based scrolling'
Invoke-Git push origin HEAD:codex/character-screens
