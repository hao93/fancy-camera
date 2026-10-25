package com.silverroom.camera;
import java.util.*;
/** Per-switch monotonic milestones. Evidence only: never changes camera readiness. */
final class LensSwitchTrace {
 private final LinkedHashMap<String,Long> marks=new LinkedHashMap<>();private float from,to;private long start;private boolean running;private String source="";private float previousRequest;private int opens,sessions,endOpens,endSessions;
 synchronized void begin(float source,float target,long requested,long now){from=source;to=target;this.source="";start=requested>0&&requested<=now?requested:now;marks.clear();running=true;mark("调度",now);}
 synchronized void context(String displayed,float request,int opened,int configured){source=displayed;previousRequest=request;opens=endOpens=opened;sessions=endSessions=configured;}
 synchronized void counts(int opened,int configured){endOpens=opened;endSessions=configured;}
 synchronized void mark(String stage,long now){if(running&&!marks.containsKey(stage))marks.put(stage,Math.max(0,now-start));}
 synchronized void end(long now){mark("首帧已绘制",now);running=false;}
 synchronized void cancel(){running=false;}
 synchronized boolean running(){return running;}
 synchronized String report(){if(marks.isEmpty())return "尚无跨镜头耗时记录";StringBuilder s=new StringBuilder("最近变焦 · 实际显示 "+ZoomScale.label(from)+" → 目标 "+ZoomScale.label(to)+"mm");if(!source.isEmpty())s.append("\n  起点来源：").append(source).append("；上次请求 ").append(ZoomScale.label(previousRequest)).append("mm\n  本次设备打开 +").append(endOpens-opens).append("／会话创建 +").append(endSessions-sessions);for(Map.Entry<String,Long> e:marks.entrySet())s.append("\n  ").append(e.getKey()).append("：+").append(e.getValue()).append("ms");return s.toString();}
}
