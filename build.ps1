param([ValidateSet('standard','compat-m')][string]$Variant='standard',[switch]$SkipInstall,[switch]$SkipPublish)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
[xml]$manifestXml=Get-Content "$projectRoot\AndroidManifest.xml"
$appVersion=$manifestXml.manifest.GetAttribute('versionName','http://schemas.android.com/apk/res/android')
if ($Variant -eq 'compat-m') {
    $manifestXml.manifest.SetAttribute('package','com.meitu.meiyancamera')
    $manifestXml.manifest.application.SetAttribute('label','http://schemas.android.com/apk/res/android','Fancy Camera · M') | Out-Null
    $manifestXml.manifest.application.provider.SetAttribute('authorities','http://schemas.android.com/apk/res/android','com.meitu.meiyancamera.updates') | Out-Null
}
$toolRoot = Join-Path (Split-Path $projectRoot -Parent) 'toolchain'
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = (Get-ChildItem (Join-Path $toolRoot 'java') -Directory | Select-Object -First 1).FullName
}
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $toolRoot 'sdk' }
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$buildTools = Join-Path $env:ANDROID_HOME 'build-tools\35.0.0'
$androidJar = Join-Path $env:ANDROID_HOME 'platforms\android-35\android.jar'
$build = Join-Path $projectRoot "build\$Variant"
$classes = Join-Path $build 'classes'
$dex = Join-Path $build 'dex'
New-Item -ItemType Directory -Force $classes,$dex | Out-Null
$buildManifest=Join-Path $build 'AndroidManifest.xml'
$manifestXml.Save($buildManifest)
Get-ChildItem -LiteralPath $classes -Recurse -File -Filter '*.class' | ForEach-Object { Remove-Item -LiteralPath $_.FullName }
Get-ChildItem -LiteralPath $dex -File -Filter '*.dex' | ForEach-Object { Remove-Item -LiteralPath $_.FullName }
$env:JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir="' + $build + '"'
function Check-Exit([string]$stage) { if ($LASTEXITCODE -ne 0) { throw "$stage failed: $LASTEXITCODE" } }
& "$buildTools\aapt2.exe" compile --dir "$projectRoot\res" -o "$build\resources.zip"
Check-Exit 'Resource compile'
& "$buildTools\aapt2.exe" link -o "$build\unsigned.apk" -I $androidJar --manifest $buildManifest --min-sdk-version 30 --target-sdk-version 35 --auto-add-overlay "$build\resources.zip"
Check-Exit 'Resource link'
$sources = @(Get-ChildItem "$projectRoot\src" -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& "$env:JAVA_HOME\bin\javac.exe" -encoding UTF-8 --release 8 -classpath $androidJar -d $classes $sources
Check-Exit 'Java compilation'
& "$env:JAVA_HOME\bin\jar.exe" cf "$build\classes.jar" -C $classes .
Check-Exit 'Class archive'
& "$buildTools\d8.bat" --release --min-api 30 --lib $androidJar --output $dex "$build\classes.jar"
Check-Exit 'DEX compilation'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open("$build\unsigned.apk",'Update')
try {
    Get-ChildItem $dex -Filter '*.dex' | ForEach-Object { [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$_.FullName,$_.Name) | Out-Null }
} finally { $zip.Dispose() }
& "$buildTools\zipalign.exe" -f -p 4 "$build\unsigned.apk" "$build\aligned.apk"
Check-Exit 'Alignment'
$key = $env:FANCY_CAMERA_KEYSTORE
$keyPassword = $env:FANCY_CAMERA_KEYSTORE_PASSWORD
$localSigning=Join-Path (Split-Path $projectRoot -Parent) 'adb-state\signing.json'
if(((-not $key) -or (-not $keyPassword)) -and (Test-Path -LiteralPath $localSigning)){
    $signing=Get-Content -LiteralPath $localSigning -Raw | ConvertFrom-Json
    if(-not $key){$key=$signing.keystore}
    if(-not $keyPassword){$keyPassword=$signing.password}
}
if(-not $key -or -not $keyPassword -or -not (Test-Path -LiteralPath $key)){throw 'Original signing key/password required via local adb-state/signing.json or FANCY_CAMERA_KEYSTORE and FANCY_CAMERA_KEYSTORE_PASSWORD'}
$previousSigningPassword=$env:FANCY_CAMERA_SIGNING_PASS
try{
    $env:FANCY_CAMERA_SIGNING_PASS=$keyPassword
    & "$buildTools\apksigner.bat" sign --ks $key --ks-pass env:FANCY_CAMERA_SIGNING_PASS --key-pass env:FANCY_CAMERA_SIGNING_PASS --ks-key-alias silverroom --out "$build\Silverroom-$appVersion.apk" "$build\aligned.apk"
}finally{$env:FANCY_CAMERA_SIGNING_PASS=$previousSigningPassword}
Check-Exit 'APK signing'
& "$buildTools\apksigner.bat" verify --verbose "$build\Silverroom-$appVersion.apk"
Check-Exit 'Signature verification'
Get-Item "$build\Silverroom-$appVersion.apk" | Select-Object FullName,Length
$publicationError=$null
if(-not $SkipPublish -and $Variant -eq 'standard'){
    try{
        $validator=Join-Path $toolRoot 'emulator30\emulator\lib64\vulkan\glslangValidator.exe'
        if(Test-Path -LiteralPath $validator){& "$projectRoot\test.ps1" -ShaderValidator $validator}else{& "$projectRoot\test.ps1"}
        & "$projectRoot\publish.ps1" -Apk "$build\Silverroom-$appVersion.apk"
    }catch{$publicationError=$_.Exception.Message;Write-Warning "APK built; GitHub publication NOT confirmed. $publicationError"}
}
if(-not $SkipInstall -and $Variant -eq 'standard'){
    try { & "$projectRoot\install.ps1" -Apk "$build\Silverroom-$appVersion.apk" }
    catch { Write-Warning "Build completed; automatic installation NOT confirmed. $($_.Exception.Message)" }
}

if($publicationError){throw "Build completed, but GitHub publication failed: $publicationError"}

