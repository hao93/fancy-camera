package com.silverroom.camera;
import android.graphics.Canvas;import android.os.SystemClock;import android.view.MotionEvent;import java.util.*;
/** Actual touch router, activity exposure methods and Guides timers, with deterministic UI time. */
public final class GuidesOverlayProbe{
 static List<String> passed=new ArrayList<>();static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);passed.add(name);}
 static class H{MainActivity a;FinderTouch touch;Controls.Guides g;H(){SystemClock.now=1000;a=new MainActivity();touch=new FinderTouch(a);g=a.guides;g.size(300,400);}void e(int action,float x,float y){touch.onTouch(g,new MotionEvent(action,x,y));}void drag(){e(0,150,300);e(2,150,180);}void up(){e(1,150,180);}void tap(float x,float y){e(0,x,y);e(1,x,y);}boolean drawsEv(){Canvas c=new Canvas();g.onDraw(c);return c.texts.stream().anyMatch(t->t.endsWith(" EV"));}}
 public static List<String> run(){
 H h=new H();h.drag();check(h.g.exposureVisible&&h.g.exposureDragging&&h.drawsEv()&&h.a.cameraEv==1,"Actual exposure drag shows its EV overlay while adjusting");h.up();h.g.advance(999);check(h.g.exposureVisible&&!h.g.exposureDragging&&h.drawsEv(),"A normal drag release preserves the EV hint for the intended one-second tail");h.g.advance(1);check(!h.g.exposureVisible&&!h.drawsEv(),"The real Guides callback removes the hint at the tail deadline");
 h=new H();h.drag();h.up();h.g.advance(200);h.tap(60,80);check(h.a.cameraEv==0&&h.a.evButton.value==0&&!h.g.exposureVisible&&!h.drawsEv()&&h.g.fx==60&&h.g.fy==80,"Refocusing at a new position resets EV and immediately removes the old hint instead of moving it with the box");h.tap(60,80);h.g.advance(1500);check(!h.g.exposureVisible&&!h.drawsEv(),"Repeated taps at the same point cannot revive a stale exposure hint");
 h=new H();h.drag();h.up();for(int n=0;n<9;n++){h.g.advance(100);h.g.exposure(h.a.cameraEv,false);}check(h.g.exposureVisible,"Repeated inactive refreshes retain only the original grace interval");h.g.advance(100);check(!h.g.exposureVisible&&!h.drawsEv(),"Inactive refreshes cannot keep postponing the existing hide deadline");
 h=new H();h.drag();h.up();h.g.advance(400);h.g.focus(90,100);check(!h.g.exposureVisible&&!h.g.deadlines.containsKey(h.g.clearExposure),"A new focus explicitly clears both the previous hint and its timeout");
 h=new H();h.drag();h.g.advance(1600);check(h.g.exposureVisible&&h.g.exposureDragging&&h.drawsEv(),"An active adjustment is not hidden by an earlier timeout");h.up();h.g.advance(1000);check(!h.g.exposureVisible,"Releasing a prolonged adjustment starts a fresh bounded tail");
 h=new H();h.drag();h.up();h.g.advance(600);h.drag();h.g.advance(500);check(h.g.exposureVisible&&h.g.exposureDragging,"A new adjustment cancels the previous release timer before its deadline");h.up();h.g.advance(1000);check(!h.g.exposureVisible,"The new adjustment still expires one second after its own release");
 h=new H();h.drag();float value=h.a.cameraEv;h.touch.cancel();h.g.advance(1500);check(h.a.cameraEv==value&&!h.g.exposureVisible&&!h.drawsEv(),"Lifecycle or lens cancellation clears the hint immediately without changing exposure");
 h=new H();h.drag();h.e(5,150,180);h.up();check(h.a.cameraEv==1&&!h.g.exposureVisible,"A second pointer cancels the displayed hint without issuing a refocus reset");
 h=new H();h.drag();h.up();h.e(0,120,150);h.g.fireHeld();h.e(1,120,150);check(h.a.cameraEv==1&&h.a.camera.locks==1&&!h.g.exposureVisible,"A stationary hold after adjustment locks without carrying the old EV hint");
 h=new H();h.drag();h.g.lock(true);check(!h.g.exposureVisible&&!h.g.exposureDragging,"Entering a confirmed focus lock removes exposure-drag feedback");
 h=new H();h.drag();h.up();h.g.onDetachedFromWindow();h.g.advance(5000);check(!h.g.exposureVisible&&h.g.fx<0&&!h.g.deadlines.containsKey(h.g.clearExposure),"Detaching the finder clears the hint and pending callback permanently");
 h=new H();h.drag();h.up();h.g.advance(3000);check(h.g.fx<0&&!h.drawsEv(),"Focus-frame expiry cannot leave a floating EV label behind");return passed;
 }
}
