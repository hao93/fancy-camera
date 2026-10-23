package com.silverroom.camera;

public final class LevelMath {
    private LevelMath(){}
    public static final class Reading {public final boolean valid,aligned;public final float tilt,axis;Reading(boolean v,float t,float a,boolean l){valid=v;tilt=t;axis=a;aligned=l;}}
    public static Reading read(float x,float y,float z,boolean aligned){double magnitude=Math.sqrt(x*x+y*y+z*z),projected=Math.hypot(x,y);if(!Double.isFinite(magnitude)||magnitude<1||projected/magnitude<.2)return new Reading(false,0,0,false);float angle=(float)Math.toDegrees(Math.atan2(-x,y)),axis=Math.round(angle/90)*90,tilt=angle-axis;return new Reading(true,tilt,axis,Math.abs(tilt)<=(aligned?1.5f:1));}
}
