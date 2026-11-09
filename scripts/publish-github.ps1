param([string]$RepositoryName = 'tiny-runners-android')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if ($RepositoryName -notmatch '^[a-zA-Z0-9_.-]+$') { throw 'Use a repository name without an owner or URL.' }
& gh auth status
if ($LASTEXITCODE -ne 0) { throw 'Sign in with gh auth login, then run this script again.' }
$existingRemote = & git -C $projectRoot remote get-url origin 2>$null
if ($existingRemote) { throw 'origin already exists. Inspect it before publishing.' }
& git -C $projectRoot rev-parse --verify HEAD
if ($LASTEXITCODE -ne 0) { throw 'Create an initial Git commit first: git add .; git commit -m "Initial Android starter"' }
& gh repo create $RepositoryName --private --source $projectRoot --remote origin --push --description 'School Android runner project: ten characters, story research, and Claude Code development guide'
if ($LASTEXITCODE -ne 0) { throw 'GitHub creation or push failed. Inspect gh output and git remote -v before retrying.' }
