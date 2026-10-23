param([Parameter(Mandatory=$true)][string]$Apk,[switch]$VerifyOnly)
$ErrorActionPreference='Stop'
$toolRoot=Join-Path (Split-Path $PSScriptRoot -Parent) 'toolchain'
$stateRoot=Join-Path (Split-Path $PSScriptRoot -Parent) 'adb-state'
$config=Get-Content -LiteralPath (Join-Path $stateRoot 'device.json') -Raw | ConvertFrom-Json
$adb=Join-Path $toolRoot 'sdk\platform-tools\adb.exe'
$bt=Join-Path $toolRoot 'sdk\build-tools\35.0.0'
$env:ANDROID_USER_HOME=$stateRoot
if(-not $env:JAVA_HOME){$env:JAVA_HOME=(Get-ChildItem (Join-Path $toolRoot 'java') -Directory | Select-Object -First 1).FullName}
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$apkPath=(Resolve-Path -LiteralPath $Apk).Path
$badging=& "$bt\aapt2.exe" dump badging $apkPath
if($LASTEXITCODE -ne 0){throw 'Cannot read APK manifest'}
$packageLine=$badging | Where-Object {$_ -like 'package:*'}
if($packageLine -notlike "*name='$($config.package)'*"){throw 'APK package does not match the default standard application; no installation performed'}
$cert=& "$bt\apksigner.bat" verify --print-certs $apkPath
if($LASTEXITCODE -ne 0 -or ($cert -join "`n") -notmatch '12f980371125eb5f2274ddf14fc421cba077255116401a399862aa7588e4b8bb'){throw 'APK original-signature verification failed; no installation performed'}
$state=& $adb -s $config.address get-state 2>&1
if($LASTEXITCODE -ne 0 -or ($state -join '').Trim() -ne 'device'){
    $connect=& $adb connect $config.address 2>&1
    $state=& $adb -s $config.address get-state 2>&1
    if($LASTEXITCODE -ne 0 -or ($state -join '').Trim() -ne 'device'){throw "Wireless phone unavailable at $($config.address). APK retained. $($connect -join ' ')"}
}
$actualSerial=(& $adb -s $config.address shell getprop ro.serialno) -join ''
if($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($actualSerial)){throw 'Wireless connection failed while reading phone identity; no installation performed'}
if($actualSerial.Trim() -ne $config.serial){throw 'Phone serial does not match the paired X100 Ultra; no installation performed'}
$actualModel=(& $adb -s $config.address shell getprop ro.product.model) -join ''
if($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($actualModel)){throw 'Wireless connection failed while reading phone model; no installation performed'}
if($actualModel.Trim() -ne $config.model){throw 'Phone model mismatch; no installation performed'}
if($VerifyOnly){Write-Output "PASS Wireless target verified: $($config.model), $($config.address); APK package and original signature verified. No installation requested by VerifyOnly.";return}
$result=& $adb -s $config.address install -r --no-incremental $apkPath 2>&1
if($LASTEXITCODE -ne 0 -or ($result -join "`n") -notmatch '(?m)^Success\s*$'){throw "APK built, but wireless installation failed: $($result -join ' ')"}
Write-Output "PASS Wireless APK replacement installed on $($config.model): $apkPath"
$installed=& $adb -s $config.address shell dumpsys package $config.package
if($LASTEXITCODE -ne 0){throw 'Installation succeeded, but installed-version readback failed'}
$expectedCode=[regex]::Match($packageLine,"versionCode='([^']+)'").Groups[1].Value
$expectedName=[regex]::Match($packageLine,"versionName='([^']+)'").Groups[1].Value
if(($installed -join "`n") -notmatch "versionCode=$([regex]::Escape($expectedCode))\s" -or ($installed -join "`n") -notmatch "(?m)^\s*versionName=$([regex]::Escape($expectedName))\s*$"){throw 'Installation completed, but installed-version readback differs from the APK'}
Write-Output "PASS Installed version confirmed: $expectedName ($expectedCode)"
