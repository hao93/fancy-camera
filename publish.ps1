param([string]$Apk,[string]$NotesFile,[string]$GitHubCli,[switch]$CreateRepository,[ValidateSet('public','private')][string]$Visibility='private')
$ErrorActionPreference='Stop'
$projectRoot=$PSScriptRoot
$toolRoot=Join-Path (Split-Path $projectRoot -Parent) 'toolchain'
$config=Get-Content -LiteralPath (Join-Path $projectRoot 'release.json') -Raw | ConvertFrom-Json
$repository=$config.repository
if($repository -notmatch '^[A-Za-z0-9-]+/[A-Za-z0-9._-]+$'){throw 'Invalid repository configuration'}
$repositoryOwner=$repository.Split('/')[0]
if(-not $GitHubCli){$command=Get-Command gh -ErrorAction SilentlyContinue;if($command){$GitHubCli=$command.Source}else{$GitHubCli=Join-Path $toolRoot 'github-cli\gh.exe'}}
if(-not (Test-Path -LiteralPath $GitHubCli)){throw 'GitHub CLI is required; install gh and run gh auth login first'}
function Check([string]$stage){if($LASTEXITCODE -ne 0){throw "$stage failed; APK retained"}}
$login=& $GitHubCli api user --jq .login
Check 'GitHub login readback'
if(($login -join '').Trim() -ne $repositoryOwner){throw 'GitHub login differs from the configured repository owner'}
if(-not $env:JAVA_HOME){$env:JAVA_HOME=(Get-ChildItem (Join-Path $toolRoot 'java') -Directory | Select-Object -First 1).FullName}
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$sdkRoot=if($env:ANDROID_HOME){$env:ANDROID_HOME}else{Join-Path $toolRoot 'sdk'}
$bt=Join-Path $sdkRoot 'build-tools\35.0.0'
[xml]$manifest=Get-Content -LiteralPath (Join-Path $projectRoot 'AndroidManifest.xml')
$version=$manifest.manifest.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
$versionCode=$manifest.manifest.GetAttribute('versionCode','http://schemas.android.com/apk/res/android')
if($version -notmatch '^[0-9]+\.[0-9]+\.[0-9]+$' -or $versionCode -notmatch '^[1-9][0-9]*$'){throw 'Invalid release version'}
if(-not $Apk){$Apk=Join-Path $projectRoot "build\standard\Silverroom-$version.apk"}
$apkPath=(Resolve-Path -LiteralPath $Apk).Path
$badging=& "$bt\aapt2.exe" dump badging $apkPath
Check 'APK metadata'
$packageLine=$badging | Where-Object {$_ -like 'package:*'}
if($packageLine -notlike "*name='$($config.packageName)'*" -or $packageLine -notlike "*versionCode='$versionCode'*" -or $packageLine -notlike "*versionName='$version'*"){throw 'APK package/version does not match source'}
$cert=& "$bt\apksigner.bat" verify --print-certs $apkPath
Check 'APK signing'
if(($cert -join "`n") -notmatch [regex]::Escape($config.certificateSha256)){throw 'Original signing certificate mismatch'}
& "$bt\zipalign.exe" -c 4 $apkPath
Check 'APK alignment'
if(-not $NotesFile){$NotesFile=Join-Path $projectRoot "releases\$version.md"}
if(-not (Test-Path -LiteralPath $NotesFile)){throw 'Write version-specific release notes before publishing'}
$tag="v$version"
$stage=Join-Path $projectRoot "build\release-$version"
New-Item -ItemType Directory -Force $stage | Out-Null
$assetName="FancyCamera-$version.apk"
$asset=Join-Path $stage $assetName
Copy-Item -LiteralPath $apkPath -Destination $asset -Force
$sha=(Get-FileHash -LiteralPath $asset -Algorithm SHA256).Hash.ToLowerInvariant()
$size=(Get-Item -LiteralPath $asset).Length
$metadata=[ordered]@{schema=1;packageName=$config.packageName;versionCode=[long]$versionCode;versionName=$version;minSdk=30;apkName=$assetName;size=$size;sha256=$sha}
$encoding=[System.Text.UTF8Encoding]::new($false)
$updateFile=Join-Path $stage 'update.json'
[IO.File]::WriteAllText($updateFile,($metadata | ConvertTo-Json -Compress),$encoding)
$checksums=Join-Path $stage 'SHA256SUMS.txt'
[IO.File]::WriteAllText($checksums,"$sha  $assetName`n",$encoding)
if(-not (Test-Path -LiteralPath (Join-Path $projectRoot '.git'))){git -C $projectRoot init -b main;Check 'Git initialization'}
$gitRoot=(git -C $projectRoot rev-parse --show-toplevel).Trim()
Check 'Git root'
if([IO.Path]::GetFullPath($gitRoot) -ne [IO.Path]::GetFullPath($projectRoot)){throw 'Publication must run from the isolated source repository'}
$author=git -C $projectRoot config user.name
if(-not $author){git -C $projectRoot config user.name $repositoryOwner;Check 'Local Git author'}
$email=git -C $projectRoot config user.email
if(-not $email){$id=& $GitHubCli api user --jq .id;Check 'GitHub ID';git -C $projectRoot config user.email "$id+$repositoryOwner@users.noreply.github.com";Check 'Local Git email'}
$include=@('.gitignore','AGENTS.md','AndroidManifest.xml','README.md','build.ps1','install.ps1','publish.ps1','test.ps1','release.json','src','res','tests','releases')
foreach($entry in $include){if(Test-Path -LiteralPath (Join-Path $projectRoot $entry)){git -C $projectRoot add -- $entry;Check "Stage $entry"}}
$tracked=@(git -C $projectRoot ls-files)
Check 'Tracked source inventory'
foreach($file in $tracked){if($file -match '(^|/)(build|adb-state|toolchain|\.codex-remote-attachments)(/|$)|\.(p12|jks|keystore|apk|dex|class)$|(^|/)signing\.json$'){throw "Private/build file staged: $file"};$first=$file.Split('/')[0];if($first -notin $include){throw "File outside publication allowlist: $file"}}
git -C $projectRoot diff --cached --quiet
if($LASTEXITCODE -eq 1){git -C $projectRoot commit -m "Fancy Camera $version";Check 'Source commit'}elseif($LASTEXITCODE -ne 0){throw 'Cannot inspect staged source'}
$head=(git -C $projectRoot rev-parse HEAD).Trim();Check 'Commit readback'
$remote=& $GitHubCli repo view $repository --json nameWithOwner 2>$null
if($LASTEXITCODE -ne 0){if(-not $CreateRepository){throw 'Repository is unavailable; explicit creation is required'};& $GitHubCli repo create $repository "--$Visibility" --description 'Fancy Camera · 胶片风格摄影与彩色暗房' --disable-wiki;Check 'Repository creation'}
$remoteUrl="https://github.com/$repository.git"
$origin=git -C $projectRoot remote get-url origin 2>$null
if($LASTEXITCODE -ne 0){git -C $projectRoot remote add origin $remoteUrl;Check 'Add remote'}elseif(($origin -join '').Trim() -ne $remoteUrl){throw 'Origin points to another repository'}
git -C $projectRoot push -u origin HEAD:main
Check 'Source push'
$existingTag=git -C $projectRoot rev-parse -q --verify "$tag^{commit}" 2>$null
if($LASTEXITCODE -eq 0){if(($existingTag -join '').Trim() -ne $head){throw 'Version tag already belongs to another commit; increase the version'}}else{git -C $projectRoot tag -a $tag -m "Fancy Camera $version";Check 'Version tag'}
git -C $projectRoot push origin $tag
Check 'Version tag push'
$releaseText=& $GitHubCli release view $tag --repo $repository --json isDraft,url 2>$null
if($LASTEXITCODE -ne 0){& $GitHubCli release create $tag --repo $repository --verify-tag --target $head --title "Fancy Camera $version" --notes-file $NotesFile --draft;Check 'Draft release creation';$draft=$true}else{$draft=($releaseText | ConvertFrom-Json).isDraft}
if($draft){& $GitHubCli release upload $tag $asset $updateFile $checksums --repo $repository --clobber;Check 'Draft asset upload'}
$release=& $GitHubCli release view $tag --repo $repository --json assets | ConvertFrom-Json
Check 'Release asset readback'
foreach($local in @($asset,$updateFile,$checksums)){$name=[IO.Path]::GetFileName($local);$uploaded=@($release.assets | Where-Object name -eq $name);$expected=(Get-FileHash -LiteralPath $local -Algorithm SHA256).Hash.ToLowerInvariant();if($uploaded.Count -ne 1 -or $uploaded[0].size -ne (Get-Item -LiteralPath $local).Length -or $uploaded[0].digest -ne "sha256:$expected"){throw "Release asset verification failed: $name; release is not published by this run"}}
if($draft){& $GitHubCli release edit $tag --repo $repository --draft=false --latest;Check 'Release publication'}
$published=& $GitHubCli api "repos/$repository/releases/tags/$tag" | ConvertFrom-Json
Check 'Published release readback'
if($published.draft -or $published.prerelease){throw 'Release was not published as a stable release'}
Write-Output "PASS Source pushed: https://github.com/$repository / $head"
Write-Output "PASS Release published and all asset SHA256 values verified: $($published.html_url)"
