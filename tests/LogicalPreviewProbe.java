package com.silverroom.camera;
import java.util.*;import android.opengl.GLES20;

/** Reproduces fast-zoom frame/result gaps using the shipping render guard and blend path. */
public final class LogicalPreviewProbe {
 static List<String> passed=new ArrayList<>();static void check(boolean yes,String name){if(!yes)throw new AssertionError(name);passed.add(name);}
 static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
 public static List<String> run(){
  ZoomEngine e=new ZoomEngine();e.previewTransition=new PreviewTransition();GLES20.version=10;e.previewHold.save(300,400);e.previewTransition.hold(300,400);e.logicalPlan=new ZoomEngine.Plan();e.previewReady=false;e.uiState=new CameraUiState(CameraUiState.SWITCHING,85,85,false,"");
  int copies=GLES20.copies,clears=GLES20.clears;
  for(int i=0;i<20;i++)e.renderPrefix();
  check(GLES20.copies==copies,"Repeated fast-zoom metadata gaps do not create or restart an old-image fade");
  check(GLES20.presented==10&&GLES20.clears==clears,"The last proved fast-zoom frame remains visible without clearing to black");
  GLES20.version=20;e.previewHold.save(300,400);e.previewHold.draw(300,400);int draws=GLES20.draws,renders=e.view.renders;e.blendForTest();
  check(GLES20.draws==draws&&GLES20.presented==20&&e.view.renders==renders,"A real new logical frame is presented directly without an older overlay or an animation loop");
  for(int i=21;i<40;i++){GLES20.version=i;e.previewHold.save(300,400);e.previewHold.draw(300,400);e.blendForTest();e.renderPrefix();if(GLES20.presented!=i)throw new AssertionError("Logical preview rolled backwards at "+i);}
  check(GLES20.presented==39,"Alternating new frames and late metadata never roll back to an earlier lens image");
  e.logicalPlan=null;e.uiState=new CameraUiState(CameraUiState.SWITCHING,23,23,false,"");e.renderPrefix();check(GLES20.copies>copies,"Independent-camera cold switches retain their existing transition snapshot");
  GLES20.version=50;e.previewHold.save(300,400);e.previewHold.draw(300,400);draws=GLES20.draws;e.blendForTest();check(GLES20.draws==draws+1&&GLES20.presented==39,"The original independent-camera blend remains active after fast-mode fallback");
  e.logicalPlan=new ZoomEngine.Plan();GLES20.version=60;e.previewHold.save(300,400);e.previewHold.draw(300,400);e.blendForTest();check(GLES20.presented==60,"Entering fast mode cancels an unfinished independent-camera overlay");
  for(int i=70;i<90;i++){GLES20.version=i;e.cacheForTest(true);int live=GLES20.presented;e.previewReady=false;e.renderPrefix();if(live!=i||GLES20.presented!=live)throw new AssertionError("Live/held presentation mismatch at "+i);}
  check(GLES20.presented==89,"Stationary logical preview alternates paired/live and delayed/held frames through the same texture presenter");
  draws=GLES20.draws;GLES20.version=90;e.cacheForTest(false);check(GLES20.draws==draws,"The stable optical route does not gain an extra presentation pass");
  e.logicalZoom.reset(LogicalZoomProbe.bases());e.logicalZoom.begin(85);LogicalZoomState.Tag tag=e.logicalZoom.tag(0);
  e.logicalZoom.result(tag,100L,"tele",85/23f,1f,100);e.logicalZoom.image(100);LogicalZoomState.Proof first=e.logicalZoom.proof();
  e.exposurePlan=ExposureScale.plan(1.5f,new ExposureLimits(-6,6,1f/3));e.previewAppliedEv=-1f;e.lastIso=6400;Film.Settings recipe=new Film.Settings();recipe.exposure=.2f;recipe.iso=7;
  Film.Settings shown=e.frameSettingsForTest(recipe,first);check(near(shown.exposure,.2f)&&shown.iso==100,"Displayed exposure and grain ISO come from the exact proved frame, not a newer result");
  e.logicalZoom.result(tag,101L,"tele",85/23f,0f,800);shown=e.frameSettingsForTest(recipe,first);check(near(shown.exposure,.2f)&&shown.iso==100,"A later result cannot alter an already paired frame's brightness or ISO");
  e.logicalZoom.image(101);shown=e.frameSettingsForTest(recipe,e.logicalZoom.proof());check(near(shown.exposure,1.2f)&&shown.iso==800,"The next paired frame uses its own native EV compensation exactly once");
  e.logicalZoom.image(102);check(e.logicalZoom.proof()==null,"An image without matching metadata still cannot bypass optical verification");
  e.logicalZoom.result(tag,102L,"tele",85/23f,null,200);shown=e.frameSettingsForTest(recipe,e.logicalZoom.proof());check(near(shown.exposure,recipe.exposure+e.exposurePlan.fine)&&shown.iso==200,"Missing per-frame EV uses the existing fine residual without borrowing a different frame's EV");
  shown=e.frameSettingsForTest(recipe,null);check(near(shown.exposure,2.2f)&&shown.iso==6400,"Stable-route preview retains its original exposure and ISO path");
  check(recipe.iso==7&&near(recipe.exposure,.2f),"Frame-specific preview correction leaves the saved recipe unchanged");
  return passed;
 }
}
