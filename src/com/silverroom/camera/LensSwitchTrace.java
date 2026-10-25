package com.silverroom.camera;
import java.util.*;
/** Per-switch monotonic milestones. Evidence only: never changes camera readiness. */
final class LensSwitchTrace {
 private final LinkedHashMap<String,Long> marks=new LinkedHashMap<>();private float from,to;private long start;private boolean running;
 synchronized void begin(float source,float target,long requested,long now){from=source;to=target;start=requested>0&&requested<=now?requested:now;marks.clear();running=true;mark("调度",now);}
 synchronized void mark(String stage,long now){if(running&&!marks.containsKey(stage))marks.put(stage,Math.max(0,now-start));}
 synchronized void end(long now){mark("首帧已绘制",now);running=false;}
 synchronized void cancel(){running=false;}
 synchronized boolean running(){return running;}
 synchronized String report(){if(marks.isEmpty())return "尚无跨镜头耗时记录";StringBuilder s=new StringBuilder("最近跨镜头 "+ZoomScale.label(from)+" → "+ZoomScale.label(to)+"mm");for(Map.Entry<String,Long> e:marks.entrySet())s.append("\n  ").append(e.getKey()).append("：+").append(e.getValue()).append("ms");return s.toString();}
}
