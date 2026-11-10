$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) { throw 'Set JAVA_HOME to your installed JDK 17 directory first. See docs/START_HERE.md.' }
$javaExe = Join-Path $env:JAVA_HOME 'bin/java.exe'
# java -version writes to stderr; with ErrorActionPreference=Stop, Windows PowerShell 5.1 treats that as a terminating error.
$previousPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$javaVersion = (& $javaExe -version 2>&1 | ForEach-Object { "$_" } | Out-String)
$ErrorActionPreference = $previousPreference
if ($javaVersion -notmatch 'version "17[.]') { throw 'This starter expects JDK 17. Select JDK 17 rather than Java 25.' }
$toolsDir = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
$archive = Join-Path $toolsDir 'gradle-8.9-bin.zip'
$checksumUri = 'https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256'
$ProgressPreference = 'SilentlyContinue'
$checksumContent = (Invoke-WebRequest -UseBasicParsing -Uri $checksumUri).Content
if ($checksumContent -is [byte[]]) { $checksumContent = [System.Text.Encoding]::ASCII.GetString($checksumContent) }
$expected = ($checksumContent | Out-String).Trim().Split(' ')[0].ToLowerInvariant()
if ($expected -notmatch '^[0-9a-f]{64}$') { throw 'Invalid checksum response.' }
if (-not (Test-Path -LiteralPath $archive)) { Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile $archive }
$actual = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expected) { throw 'Checksum mismatch. Do not run this archive; inspect the download and retry.' }
$gradle = Join-Path $toolsDir 'gradle-8.9/bin/gradle.bat'
if (-not (Test-Path -LiteralPath $gradle)) { Expand-Archive -LiteralPath $archive -DestinationPath $toolsDir }
$bootstrap = Join-Path $toolsDir 'wrapper-project'
New-Item -ItemType Directory -Force -Path $bootstrap | Out-Null
# Write without a BOM: Windows PowerShell 5.1 '-Encoding UTF8' adds one and Gradle's Groovy parser rejects it.
[System.IO.File]::WriteAllText((Join-Path $bootstrap 'settings.gradle'), "rootProject.name = 'wrapper-bootstrap'", (New-Object System.Text.UTF8Encoding($false)))
# Gradle may write warnings to stderr; judge success by exit code only.
$ErrorActionPreference = 'Continue'
& $gradle --no-daemon -p $bootstrap wrapper --gradle-version 8.9 --distribution-type bin --gradle-distribution-sha256-sum $expected 2>&1 | ForEach-Object { "$_" }
$gradleExit = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($gradleExit -ne 0) { throw 'Gradle wrapper generation failed.' }
Copy-Item -LiteralPath (Join-Path $bootstrap 'gradlew') -Destination $projectRoot
Copy-Item -LiteralPath (Join-Path $bootstrap 'gradlew.bat') -Destination $projectRoot
Copy-Item -LiteralPath (Join-Path $bootstrap 'gradle/wrapper/gradle-wrapper.jar') -Destination (Join-Path $projectRoot 'gradle/wrapper/gradle-wrapper.jar')
Copy-Item -LiteralPath (Join-Path $bootstrap 'gradle/wrapper/gradle-wrapper.properties') -Destination (Join-Path $projectRoot 'gradle/wrapper/gradle-wrapper.properties')
Write-Host 'Wrapper ready. Open Android Studio, set Gradle JDK 17 and SDK 35, then Sync.'
