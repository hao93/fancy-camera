package com.silverroom.camera;
import java.util.*;

/** Actual frame/JPEG gates and extracted shipping zoom methods, with synthetic callbacks. */
public final class LogicalZoomProbe {
 static List<String> passed=new ArrayList<>();static void check(boolean value,String name){if(!value)throw new AssertionError(name);passed.add(name);}
 static Map<String,Integer> bases(){Map<String,Integer> m=new HashMap<>();m.put("wide",14);m.put("main",23);m.put("tele",85);return m;}
 static String physical(float mm){int b=LensPlan.base(mm);return b==14?"wide":b==23?"main":"tele";}
 public static List<String> run(){
  LogicalZoomState state=new LogicalZoomState();state.reset(bases());
  for(float mm:new float[]{14,23,35,50,85,135,230,84.9f,22.9f,23,85,14}){
   long epoch=state.begin(mm);LogicalZoomState.Tag tag=state.tag(7);long timestamp=epoch*1000;
   check(state.proof()==null&&LogicalZoomState.meter(tag)==7,"New request clears proof and retains the lock epoch at "+mm+"mm");
   state.image(timestamp);check(state.proof()==null,"Image alone cannot prove the requested lens at "+mm+"mm");
   check(state.result(tag,timestamp,physical(mm),LogicalZoomState.ratio(mm))==LogicalZoomState.VALID&&state.proof()!=null&&state.proof().focal==mm,"Exact image and result admit the correct optical base at "+mm+"mm");
  }
  LogicalZoomState.Tag stale=state.tag(8);long epoch=state.begin(230);state.image(500);check(state.result(stale,500L,"wide",14/23f)==LogicalZoomState.STALE&&state.proof()==null,"Late old-lens results cannot prove a new telephoto request");
  LogicalZoomState.Tag tag=state.tag(9);check(state.result(tag,500L,"main",10f)==LogicalZoomState.INVALID&&state.proof()==null,"A main-camera 10x crop is rejected as telephoto");
  check(state.result(tag,500L,"tele",null)==LogicalZoomState.INVALID,"Missing returned zoom ratio is rejected");
  check(state.result(tag,null,"tele",10f)==LogicalZoomState.INVALID,"Missing sensor timestamp is rejected");
  check(state.result(tag,500L,null,10f)==LogicalZoomState.INVALID,"Missing active physical ID is rejected");
  check(state.result(tag,500L,"tele",1f)==LogicalZoomState.INVALID,"A stale returned ratio is rejected even on the correct lens");
  state.result(tag,501L,"tele",10f);check(state.proof()==null,"A different frame timestamp cannot unlock rendering");state.image(501);check(state.proof()!=null,"Result-before-image pairing also works");
  state.image(500);check(state.proof()==null,"Overwriting the texture with an unverified frame revokes its proof");
  for(long t=600;t<650;t++)state.result(tag,t,"tele",10f);state.image(600);check(state.proof()==null,"Result history is bounded and evicts old frames");
  state.image(649);state.clear();check(state.proof()==null&&state.result(tag,649L,"tele",10f)==LogicalZoomState.STALE,"Pause or fallback invalidates both image and result generations");
  check(LogicalZoomState.meter(Long.valueOf(12))==12&&LogicalZoomState.meter(null)==Long.MIN_VALUE,"Stable-route lock tags remain backward compatible");
  for(boolean imageFirst:new boolean[]{true,false}){LogicalJpegGate gate=new LogicalJpegGate();byte[] jpeg={1,2};if(imageFirst){gate.image(10,jpeg);check(gate.take()==null,"JPEG waits for its own result");gate.result(10,true);}else{gate.result(10,true);check(gate.take()==null,"Still result waits for its own JPEG");gate.image(10,jpeg);}check(gate.take()==jpeg&&gate.take()==null,"Verified JPEG is delivered exactly once; imageFirst="+imageFirst);}
  LogicalJpegGate bad=new LogicalJpegGate();bad.image(10,new byte[1]);bad.result(11,true);check(bad.invalid()&&bad.take()==null,"A JPEG from another timestamp never reaches saving");
  bad=new LogicalJpegGate();bad.result(10,false);bad.image(10,new byte[1]);check(bad.invalid()&&bad.take()==null,"An unverified still result cannot be rescued by matching pixels");
  bad=new LogicalJpegGate();bad.image(10,new byte[1]);bad.cancel();bad.result(10,true);check(bad.take()==null,"A cancelled JPEG cannot later enter the gallery");
  ZoomEngine engine=new ZoomEngine();engine.logicalPlan=new ZoomEngine.Plan();engine.logicalZoom.reset(bases());engine.displayedMm=85;engine.displayedSource="tele";engine.focal(230);engine.handler.flush();
  check(engine.closed==0&&engine.opened==0&&engine.updated==1&&engine.requestedMm==230&&engine.previewCrop==1&&!engine.previewReady,"Shipping logical zoom reuses its session and never applies a second software crop");
  check(engine.switchTrace.report().contains("实际显示 85 → 目标 230mm")&&engine.switchTrace.report().contains("上次请求 23mm"),"Trace separates last displayed lens from the earlier requested focal");
  engine.focal(14);engine.focal(50);engine.applyForTest();check(engine.requestedMm==230&&engine.targetMm==50&&engine.updated==1,"Rapid reversal replaces the target without queuing multiple camera zoom requests");
  engine.texture.timestamp=999;engine.frame=true;engine.latchForUi();engine.previewResultSeen=true;engine.deliverResult();check(!engine.previewReady,"Shipping readiness cannot accept an unverified logical texture");
  engine.logicalZoom.result(engine.logicalZoom.tag(0),999L,"tele",10f);engine.externalFrameReady=true;engine.deliverResult();check(engine.previewReady&&engine.logicalEstablished&&engine.startup.profile(engine.startupKey(),8)==-1,"Verified logical preview is ready without polluting stable route profiles");
  engine.presentedForTest(engine.sessionGeneration-1,engine.logicalZoom.epoch());check(engine.requestedMm==230,"A stale GL acknowledgement cannot drain a pending zoom target");engine.presentedForTest(engine.sessionGeneration,engine.logicalZoom.epoch());check(engine.requestedMm==50&&engine.updated==2&&engine.closed==0&&engine.opened==0,"The latest pending target uses the same session after verified readiness");
  ZoomEngine timed=new ZoomEngine();timed.logicalPlan=new ZoomEngine.Plan();timed.logicalZoom.reset(bases());timed.focal(85);timed.handler.flush();Runnable firstDeadline=timed.handler.tasks.get(0);timed.logicalZoom.begin(230);firstDeadline.run();check(timed.recovered==0,"An expired timer from an earlier zoom epoch cannot abort the new request");
  timed.previewReady=true;timed.focal(230);timed.handler.flush();Runnable currentDeadline=timed.handler.tasks.get(timed.handler.tasks.size()-1);currentDeadline.run();check(timed.recovered==1&&timed.logicalPlan==null,"The shipping 1.2 second deadline triggers fallback when no verified frame arrives");
  LensSwitchTrace trace=new LensSwitchTrace();trace.begin(14,23,100,116);trace.context("physical-wide",50,3,3);trace.counts(3,3);trace.end(180);check(trace.report().contains("设备打开 +0／会话创建 +0"),"A same-session handoff reports zero device and session creations");
  return passed;
 }
}
