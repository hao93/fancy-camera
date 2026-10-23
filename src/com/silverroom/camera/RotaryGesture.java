package com.silverroom.camera;

/** Continuous clockwise drag, with a quiet centre and no jump at the angle seam. */
public final class RotaryGesture {
    private final float min,max,step;
    private float raw,radius,lastAngle=Float.NaN;
    public RotaryGesture(float min,float max,float step){this.min=min;this.max=max;this.step=step;}
    public void begin(float x,float y,float radius,float value){
        this.radius=radius;raw=clamp(value);lastAngle=angle(x,y);
    }
    private float angle(float x,float y){return x*x+y*y<radius*radius*.09f?Float.NaN:(float)Math.toDegrees(Math.atan2(y,x));}
    public float move(float x,float y){
        float next=angle(x,y);
        if(!Float.isNaN(next)&&!Float.isNaN(lastAngle)){
            float delta=next-lastAngle;
            if(delta>180)delta-=360;else if(delta< -180)delta+=360;
            raw=clamp(raw+delta*(max-min)/270f);
        }
        lastAngle=next;return clamp(Math.round(raw/step)*step);
    }
    private float clamp(float v){return Math.max(min,Math.min(max,v));}
}
