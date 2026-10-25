package com.silverroom.camera;
/** GPU-only old-lens overlay. Readiness and the live frame cache remain independent. */
final class PreviewTransition {
 private final PreviewHold snapshot=new PreviewHold();private boolean waiting;private long started=-1;
 void clear(){waiting=false;started=-1;snapshot.clear();}
 void hold(int width,int height){if(waiting)return;snapshot.save(width,height);waiting=true;started=-1;}
 boolean blend(int width,int height,long now,boolean animations){if(waiting){waiting=false;started=now;}if(started<0)return false;long age=Math.max(0,now-started);if(!animations||age>=120){clear();return false;}boolean drawn=snapshot.draw(width,height,1-age/120f);if(!drawn)clear();return drawn;}
}
