param([string]$ShaderValidator,[string]$JsonJar)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$jdkRoot = $env:JAVA_HOME
if (-not $jdkRoot) {
    $jdkRoot = (Get-ChildItem (Join-Path (Split-Path $projectRoot -Parent) 'toolchain\java') -Directory | Select-Object -First 1).FullName
}
$testClasses = Join-Path $projectRoot 'build\test-classes'
New-Item -ItemType Directory -Force $testClasses | Out-Null
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -d $testClasses "$projectRoot\src\com\silverroom\camera\LensPlan.java" "$projectRoot\src\com\silverroom\camera\LogicalZoomState.java" "$projectRoot\src\com\silverroom\camera\MeteringState.java" "$projectRoot\src\com\silverroom\camera\MeteringGeometry.java" "$projectRoot\src\com\silverroom\camera\PreviewFraming.java" "$projectRoot\src\com\silverroom\camera\ToolbarConfig.java" "$projectRoot\src\com\silverroom\camera\PhotoControls.java" "$projectRoot\src\com\silverroom\camera\PhotoCapabilities.java" "$projectRoot\src\com\silverroom\camera\MeterMath.java" "$projectRoot\src\com\silverroom\camera\NativeReading.java" "$projectRoot\src\com\silverroom\camera\ArchiveSchema.java" "$projectRoot\src\com\silverroom\camera\MeterReferences.java"
if ($LASTEXITCODE -ne 0) { throw 'Metering helper compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -d $testClasses "$projectRoot\src\com\silverroom\camera\UiSpec.java" "$projectRoot\src\com\silverroom\camera\CameraUiState.java" "$projectRoot\src\com\silverroom\camera\LensPlan.java" "$projectRoot\src\com\silverroom\camera\ZoomScale.java" "$projectRoot\src\com\silverroom\camera\YuvFrames.java" "$projectRoot\src\com\silverroom\camera\CameraHandoff.java" "$projectRoot\src\com\silverroom\camera\YuvGeometry.java" "$projectRoot\src\com\silverroom\camera\CameraShaders.java" "$projectRoot\src\com\silverroom\camera\CaptureOrientation.java" "$projectRoot\src\com\silverroom\camera\FilmGrain.java" "$projectRoot\src\com\silverroom\camera\FilmCatalog.java" "$projectRoot\src\com\silverroom\camera\FilmGroups.java" "$projectRoot\src\com\silverroom\camera\FilterShelf.java" "$projectRoot\tests\OpticalPolicyTest.java" "$projectRoot\tests\PipelineRegression.java" "$projectRoot\tests\PhotoFeaturesRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\PreviewQuality.java" "$projectRoot\src\com\silverroom\camera\ImageQuality.java" "$projectRoot\src\com\silverroom\camera\FrameFormat.java"
if ($LASTEXITCODE -ne 0) { throw 'Quality helper compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\RotaryGesture.java" "$projectRoot\src\com\silverroom\camera\PreviewFraming.java" "$projectRoot\tests\InteractionRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Interaction test compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\FrameFormat.java" "$projectRoot\tests\FrameFormatRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Frame format test compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\FilmWheelState.java" "$projectRoot\tests\FilmWheelRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Film wheel test compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\LensWheelPolicy.java" "$projectRoot\src\com\silverroom\camera\ExposureValue.java" "$projectRoot\tests\CompactControlsRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Compact control test compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\PreviewPixels.java" "$projectRoot\src\com\silverroom\camera\PreviewFidelity.java" "$projectRoot\src\com\silverroom\camera\PreviewBudget.java" "$projectRoot\src\com\silverroom\camera\PreviewChannel.java" "$projectRoot\tests\LivePreviewRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Live preview test compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\PreviewFlow.java" "$projectRoot\src\com\silverroom\camera\PreviewAtlas.java" "$projectRoot\src\com\silverroom\camera\Histogram.java" "$projectRoot\tests\FastPreviewRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Fast preview test compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\PreviewFidelityRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Preview fidelity test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses PreviewFidelityRegression
if ($LASTEXITCODE -ne 0) { throw 'High-resolution preview atlas or quality recovery failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\FilmFamiliesRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Film family test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses FilmFamiliesRegression
if ($LASTEXITCODE -ne 0) { throw 'Film families or extended atlas regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\fakes\android\graphics\Bitmap.java" "$projectRoot\src\com\silverroom\camera\FilmEffects.java" "$projectRoot\src\com\silverroom\camera\FilmStrength.java" "$projectRoot\src\com\silverroom\camera\Film.java" "$projectRoot\tests\NewStylesRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Spatial film export test compilation failed' }
& "$jdkRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $testClasses NewStylesRegression "$projectRoot\build\style-checks"
if ($LASTEXITCODE -ne 0) { throw 'New recipe classification or spatial export regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\MeteringLock.java" "$projectRoot\tests\CreativeControlsRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Creative controls test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses CreativeControlsRegression
if ($LASTEXITCODE -ne 0) { throw 'Film strength, favorites or AE/AF lock regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\PreviewGrain.java" "$projectRoot\tests\GrainRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Grain precision test compilation failed' }
& "$jdkRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $testClasses GrainRegression "$projectRoot\build\grain-checks"
if ($LASTEXITCODE -ne 0) { throw 'Grain precision or diagonal correlation regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\GrainStyleRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Style grain restraint test compilation failed' }
& "$jdkRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $testClasses GrainStyleRegression "$projectRoot\build\light-grain-checks"
if ($LASTEXITCODE -ne 0) { throw 'Style-specific grain restraint regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\UpdatePolicy.java" "$projectRoot\src\com\silverroom\camera\UpdateTrigger.java" "$projectRoot\tests\UpdateRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Update test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses UpdateRegression "$projectRoot\build\update-tests"
if ($LASTEXITCODE -ne 0) { throw 'Release update policy or lifecycle trigger regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\AppTheme.java" "$projectRoot\tests\ThemeRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Appearance test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses ThemeRegression
if ($LASTEXITCODE -ne 0) { throw 'Theme contrast, repeat switching or export isolation regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\WindowLifecycleRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Window lifecycle test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses WindowLifecycleRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Production theme window cold-start or attachment regression failed' }
if (-not $JsonJar) { $JsonJar=Join-Path (Split-Path $projectRoot -Parent) 'toolchain\json\json-20240303.jar' }
if (-not (Test-Path -LiteralPath $JsonJar)) { throw 'Host JSON tests need JSON-Java 20240303; pass -JsonJar. This dependency is not part of the APK.' }
$workflowClasspath="$testClasses;$JsonJar"
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $workflowClasspath -d $testClasses "$projectRoot\src\com\silverroom\camera\SettingsCodec.java" "$projectRoot\src\com\silverroom\camera\RecipeStore.java" "$projectRoot\src\com\silverroom\camera\CaptureInfo.java" "$projectRoot\src\com\silverroom\camera\RawPolicy.java" "$projectRoot\src\com\silverroom\camera\NegativeStore.java" "$projectRoot\src\com\silverroom\camera\NegativeTrash.java" "$projectRoot\tests\ExpansionRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Workflow regression compilation failed' }
New-Item -ItemType Directory -Force (Join-Path $projectRoot 'build\expansion-tests') | Out-Null
& "$jdkRoot\bin\java.exe" -cp $workflowClasspath com.silverroom.camera.ExpansionRegression "$projectRoot\build\expansion-tests"
if ($LASTEXITCODE -ne 0) { throw 'Quality, RAW policy, recipe or recycle regression failed' }
$printSources=@('ExposureLimits','ExposureScale','FinderGesture','CompositionState','LevelMath','PrintOptions','PrintLayout','PrintMetadata','PrintRenderer','PrintBatch','GuideOverlay') | ForEach-Object { Join-Path $projectRoot "src\com\silverroom\camera\$_.java" }
$printFakes=@('Bitmap','Paint','Canvas','RectF','Typeface') | ForEach-Object { Join-Path $projectRoot "tests\fakes\android\graphics\$_.java" }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $workflowClasspath -d $testClasses $printFakes $printSources "$projectRoot\tests\PrintExperienceRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Print experience test compilation failed' }
& "$jdkRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $workflowClasspath com.silverroom.camera.PrintExperienceRegression "$projectRoot\build\print-checks"
if ($LASTEXITCODE -ne 0) { throw 'Exposure gesture, composition or print regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\FinderLifecycleRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Finder lifecycle test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses FinderLifecycleRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Production touch or level sensor lifecycle regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\RawLifecycleRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'RAW lifecycle test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses RawLifecycleRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Production RAW transaction lifecycle regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\PreviewQuality.java" "$projectRoot\tests\PreviewQualityRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Preview detail test compilation failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\NegativeStore.java" "$projectRoot\src\com\silverroom\camera\NegativeSelection.java" "$projectRoot\src\com\silverroom\camera\PhotoViewport.java" "$projectRoot\src\com\silverroom\camera\PhotoBrowseGesture.java" "$projectRoot\tests\GalleryRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Gallery test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses GalleryRegression "$projectRoot\build\gallery-tests"
if ($LASTEXITCODE -ne 0) { throw 'Negative deletion or photo gesture regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\PhotoPager.java" "$projectRoot\tests\PhotoBrowseRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Photo browsing test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses PhotoBrowseRegression
if ($LASTEXITCODE -ne 0) { throw 'Photo browsing gesture or stale-image regression failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses PreviewQualityRegression
if ($LASTEXITCODE -ne 0) { throw 'Preview resolution or crop regression failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses FastPreviewRegression
if ($LASTEXITCODE -ne 0) { throw 'Fast preview or histogram regression failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses LivePreviewRegression
if ($LASTEXITCODE -ne 0) { throw 'Live preview regression failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses CompactControlsRegression
if ($LASTEXITCODE -ne 0) { throw 'Compact control regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\ReelInteractionRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Production reel event test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses ReelInteractionRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Production reel touch, strength or accessibility regression failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses FilmWheelRegression
if ($LASTEXITCODE -ne 0) { throw 'Film wheel regression failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses FrameFormatRegression
if ($LASTEXITCODE -ne 0) { throw 'Frame format regression failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses InteractionRegression
if ($LASTEXITCODE -ne 0) { throw 'Rotary interaction or preview framing regression failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses OpticalPolicyTest
if ($LASTEXITCODE -ne 0) { throw 'Optical routing or YUV test failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses PipelineRegression "$projectRoot\build\shader-tests"
if ($LASTEXITCODE -ne 0) { throw 'Camera handoff or YUV orientation test failed' }
& "$jdkRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $testClasses PhotoFeaturesRegression "$projectRoot\build\filter-check.png"
if ($LASTEXITCODE -ne 0) { throw 'Capture orientation or filter menu regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\CameraZoomRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Camera zoom test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses CameraZoomRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Continuous zoom or GPU frame hold regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\LogicalTrialRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Logical trial regression compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses LogicalTrialRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Logical trial eligibility or lifecycle failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\UiRefinementRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'UI refinement test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses UiRefinementRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Camera presentation, darkroom or confirmed photo gesture regression failed' }

& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\src\com\silverroom\camera\ExposureMatch.java" "$projectRoot\tests\ExposurePipelineRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Exposure pipeline test compilation failed' }
& "$jdkRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $testClasses ExposurePipelineRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Exposure transfer, no-double-compensation or saved-pixel regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\MeteringCameraRegression.java" "$projectRoot\tests\MeteringPolicyRegression.java" "$projectRoot\tests\ToolbarRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Metering/toolbar test compilation failed' }
& "$jdkRoot\bin\java.exe" '-Duser.language=en' -cp $testClasses MeteringCameraRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Native region request regression failed' }
& "$jdkRoot\bin\java.exe" '-Duser.language=en' -cp $testClasses MeteringPolicyRegression
if ($LASTEXITCODE -ne 0) { throw 'Metering geometry/state regression failed' }
& "$jdkRoot\bin\java.exe" '-Duser.language=en' -cp $testClasses ToolbarRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Toolbar visibility regression failed' }


& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\CaptureWorkflowRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Capture workflow test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses CaptureWorkflowRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Background capture queue or shutter release regression failed' }

& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\NavigationHierarchyRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Navigation hierarchy test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses NavigationHierarchyRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Camera/gallery/darkroom hierarchy or capture layout regression failed' }

& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $testClasses -d $testClasses "$projectRoot\tests\PreviewPanelRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Preview panel test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $testClasses PreviewPanelRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Undimmed preview panel or single-instance lifecycle regression failed' }


& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $workflowClasspath -d $testClasses "$projectRoot\src\com\silverroom\camera\CaptureQueue.java" "$projectRoot\tests\StudioDomainRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Studio domain compilation failed' }
& "$jdkRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $workflowClasspath com.silverroom.camera.StudioDomainRegression
if ($LASTEXITCODE -ne 0) { throw 'Studio domain regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $workflowClasspath -d $testClasses "$projectRoot\tests\NativeControlsRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Native PRO test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $workflowClasspath NativeControlsRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Native PRO capability/request regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $workflowClasspath -d $testClasses "$projectRoot\tests\StudioNavigationRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Studio navigation test compilation failed' }
& "$jdkRoot\bin\java.exe" -cp $workflowClasspath StudioNavigationRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Studio navigation/mode restoration regression failed' }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $workflowClasspath -d $testClasses "$projectRoot\tests\StudioMediaRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Studio media test compilation failed' }
& "$jdkRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $workflowClasspath com.silverroom.camera.StudioMediaRegression $projectRoot
if ($LASTEXITCODE -ne 0) { throw 'Studio byte-preserving IO/print regression failed' }
$archiveClasses=Join-Path $projectRoot 'build\archive-test-classes'
New-Item -ItemType Directory -Force $archiveClasses | Out-Null
$archiveFakes=Get-ChildItem (Join-Path $projectRoot 'tests\archive-fakes') -Filter '*.java' | ForEach-Object { $_.FullName }
& "$jdkRoot\bin\javac.exe" --release 8 -encoding UTF-8 -cp $workflowClasspath -d $archiveClasses $archiveFakes "$projectRoot\src\com\silverroom\camera\RollRepository.java" "$projectRoot\tests\StudioArchiveRegression.java"
if ($LASTEXITCODE -ne 0) { throw 'Archive SQL test compilation failed' }
$archivePython=Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
if (-not (Test-Path $archivePython)) { $archivePython=(Get-Command python -ErrorAction Stop).Source }
& "$jdkRoot\bin\java.exe" "-Darchive.python=$archivePython" "-Darchive.bridge=$projectRoot\tests\archive_sqlite_bridge.py" -cp "$archiveClasses;$workflowClasspath" com.silverroom.camera.StudioArchiveRegression "$projectRoot\build"
if ($LASTEXITCODE -ne 0) { throw 'Production archive SQL regression failed' }

if ($ShaderValidator) {
    & $ShaderValidator -l "$projectRoot\build\shader-tests\camera.vert" "$projectRoot\build\shader-tests\yuv.frag"
    if ($LASTEXITCODE -ne 0) { throw 'YUV shader compilation/link validation failed' }
    & $ShaderValidator -l "$projectRoot\build\shader-tests\camera.vert" "$projectRoot\build\shader-tests\external.frag"
    if ($LASTEXITCODE -ne 0) { throw 'External shader compilation/link validation failed' }
    & $ShaderValidator -l "$projectRoot\build\shader-tests\camera.vert" "$projectRoot\build\shader-tests\raw-external.frag"
    if ($LASTEXITCODE -ne 0) { throw 'Popup raw external shader compilation/link validation failed' }
    & $ShaderValidator -l "$projectRoot\build\shader-tests\camera.vert" "$projectRoot\build\shader-tests\raw-yuv.frag"
    if ($LASTEXITCODE -ne 0) { throw 'Popup raw YUV shader compilation/link validation failed' }
    Write-Output 'PASS ALL FOUR SHIPPING GLSL PROGRAMS COMPILE AND LINK'
    foreach($fallback in @('medium-external.frag','medium-yuv.frag','medium-raw-external.frag','medium-raw-yuv.frag')){
        & $ShaderValidator -l "$projectRoot\build\shader-tests\camera.vert" "$projectRoot\build\shader-tests\$fallback"
        if ($LASTEXITCODE -ne 0) { throw "Mediump fallback shader validation failed: $fallback" }
    }
    Write-Output 'PASS ALL FOUR MEDIUMP FALLBACK PROGRAMS COMPILE AND LINK'
    & $ShaderValidator -l "$projectRoot\build\shader-tests\hold.vert" "$projectRoot\build\shader-tests\hold.frag"
    if ($LASTEXITCODE -ne 0) { throw 'Hold-frame shader validation failed' }
    Write-Output 'HOLD FRAME GLSL PROGRAM COMPILE AND LINK PASSED'
}








