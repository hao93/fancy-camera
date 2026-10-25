package com.silverroom.camera;
import android.graphics.Canvas;import android.os.SystemClock;import android.view.MotionEvent;import java.util.*;

public final class MeteringTouchProbe{
 static List<String> passed=new ArrayList<>();static void check(boolean v,String n){if(!v)throw new AssertionError(n);passed.add(n);}
 static class H{MainActivity a=new MainActivity();Controls.Guides g=a.guides;FinderTouch t=new FinderTouch(a);H(){g.size(300,400);}void e(int n,float x,float y){t.onTouch(g,new MotionEvent(n,x,y));}void tap(float x,float y){e(0,x,y);e(1,x,y);}float[] handle(){return MeteringGeometry.handle(a.camera.points(),300,400,1);}}
 public static List<String> run(){
  H h=new H();h.a.cameraEv=1.7f;h.a.camera.value=1.7f;h.tap(120,200);check(h.a.cameraEv==0&&h.a.camera.points().mode==MeteringState.SPOT&&h.a.camera.points().afX==.4f&&h.a.camera.points().aeY==.5f,"Confirmed finder tap resets EV and creates a persistent shared AF/AE point");
  h.a.cameraEv=1.2f;h.a.camera.value=1.2f;float[] handle=h.handle();int af=h.a.camera.focuses;h.tap(handle[0],handle[1]);check(h.a.cameraEv==1.2f&&h.a.camera.focuses==af&&h.g.aeSelected,"A tap on the 48dp AE handle does not refocus or reset EV");
  h.e(0,handle[0]-8,handle[1]+6);h.e(2,handle[0]+22,handle[1]+46);h.e(1,handle[0]+22,handle[1]+46);MeteringState.Snapshot p=h.a.camera.points();check(Math.abs(p.aeX-.5f)<.0001&&Math.abs(p.aeY-.6f)<.0001&&p.afX==.4f&&p.afY==.5f&&h.a.cameraEv==1.2f&&h.a.camera.focuses==af,"AE drag preserves its initial finger offset and moves only metering, with no focus or EV jump");
  handle=h.handle();h.e(0,handle[0],handle[1]);h.e(2,handle[0]+20,handle[1]+20);float applied=h.a.camera.points().aeX;h.e(3,handle[0]+50,handle[1]+50);h.e(1,10,10);check(h.a.camera.points().aeX==applied&&h.a.camera.focuses==af&&h.a.cameraEv==1.2f,"Cancelled independent metering keeps the last applied point and cannot turn into a tap");
  p=h.a.camera.points();h.e(0,30,330);h.e(2,30,210);h.e(1,30,210);check(h.a.camera.points().aeX==p.aeX&&h.a.camera.points().aeY==p.aeY&&h.a.cameraEv>1.2f,"An EV drag outside the AE handle preserves the existing metering position");
  h=new H();h.tap(120,200);h.a.camera.meterAt(.7f,.7f);p=h.a.camera.points();h.e(0,120,200);h.g.fireHeld();h.e(1,120,200);check(h.a.camera.locks==1&&h.a.camera.points()==p,"Holding the AF marker locks the current separated points without joining them");
  handle=h.handle();h.e(0,handle[0],handle[1]);h.g.fireHeld();h.e(1,handle[0],handle[1]);check(h.a.camera.locks==2&&h.a.camera.points()==p,"Holding the AE marker also locks the two existing points in place");
  h=new H();h.a.cameraEv=-1;h.e(0,40,300);h.g.fireHeld();h.e(1,40,300);check(h.a.camera.locks==1&&h.a.cameraEv==-1&&h.a.camera.points().afX==h.a.camera.points().aeX,"Holding blank finder space creates a combined point and retains EV");
  h=new H();h.tap(120,200);handle=h.handle();af=h.a.camera.focuses;h.e(0,handle[0],handle[1]);h.e(5,handle[0],handle[1]);h.g.fireHeld();h.e(1,handle[0],handle[1]);check(h.a.camera.focuses==af&&h.a.camera.locks==0,"A second finger cancels an AE hold without focus, reset or lock");
  h.a.busy=true;h.tap(20,20);check(h.a.camera.focuses==af,"Capture busy rejects both marker and finder taps");
  h=new H();h.a.cameraEv=1;h.e(0,20,330);h.e(1,20,100);check(h.a.cameraEv==1&&h.a.camera.focuses==0,"A displaced final release without MOVE cannot be mistaken for a focus-reset tap");
  h=new H();h.tap(120,200);SystemClock.now+=10000;h.g.advance(10000);Canvas c=new Canvas();h.g.onDraw(c);check(h.g.fx>=0&&c.texts.contains("AE")&&c.texts.stream().noneMatch(t->t.endsWith(" EV")),"Persistent compact AF/AE marks survive while the temporary EV hint remains hidden");
  h.g.pointState(h.a.camera.points(),false);c=new Canvas();h.g.onDraw(c);check(!c.texts.contains("AE")&&!h.g.hitAE(164,156)&&h.g.fx>=0,"Unsupported native metering hides its AE drag handle while retaining the focus marker");
  boolean bounded=true,hit=true;MeteringState state=new MeteringState();for(int w:new int[]{320,360,432})for(float x:new float[]{0,.01f,.5f,.99f,1})for(float y:new float[]{0,.5f,1}){state.tap(x,y);float[] xy=MeteringGeometry.handle(state.snapshot(),w,500,1);bounded&=xy[0]>=24&&xy[0]<=w-24&&xy[1]>=24&&xy[1]<=476;hit&=MeteringGeometry.hit(state.snapshot(),true,xy[0]+23.9f,xy[1]-23.9f,w,500,1)&&!MeteringGeometry.hit(state.snapshot(),true,xy[0]+24.1f,xy[1],w,500,1);}check(bounded&&hit,"AE handles stay inside all tested viewport edges with a full 48dp touch target");
  return passed;
 }
}
