package com.silverroom.camera;
import android.graphics.*;

final class GuideOverlay {
    static void draw(Canvas c,Paint p,float w,float h,float d,CompositionState s,LevelMath.Reading level){if(s==null||!s.on)return;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d*.8f);p.setColor(0x70fff7dc);if(s.lines==1||s.lines==2){float a=s.lines==1?1f/3:.381966f,b=1-a;for(float f:new float[]{a,b}){c.drawLine(w*f,0,w*f,h,p);c.drawLine(0,h*f,w,h*f,p);}}else if(s.lines==3){c.drawLine(0,0,w,h,p);c.drawLine(w,0,0,h,p);}if(s.cross){p.setColor(0x99fff7dc);c.drawLine(w/2-9*d,h/2,w/2+9*d,h/2,p);c.drawLine(w/2,h/2-9*d,w/2,h/2+9*d,p);}if(s.level&&level!=null&&level.valid){float cx=w/2,cy=h*.60f,arm=34*d;c.save();c.rotate(-level.axis,cx,cy);p.setColor(0x77000000);p.setStrokeWidth(4*d);c.drawLine(cx-arm,cy,cx+arm,cy,p);p.setColor(0xaafff7dc);p.setStrokeWidth(d);c.drawLine(cx-arm,cy,cx+arm,cy,p);c.drawLine(cx,cy-5*d,cx,cy+5*d,p);c.rotate(-level.tilt,cx,cy);p.setColor(level.aligned?0xffffd365:0xddfff7dc);p.setStrokeWidth(2*d);c.drawLine(cx-arm*.65f,cy,cx+arm*.65f,cy,p);c.restore();}p.setStyle(Paint.Style.FILL);}
}
