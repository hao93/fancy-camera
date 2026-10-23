package com.silverroom.camera;

/** OrientationEventListener angles, independent of a portrait-locked Activity. */
public final class CaptureOrientation {
    private CaptureOrientation(){}
    public static int update(int degrees,int previous){
        if(degrees<0)return previous;
        int d=((degrees-previous+540)%360)-180;
        if(Math.abs(d)<55)return previous;
        return ((degrees+45)/90*90)%360;
    }
    public static final class Snapshot {
        public final int sensor,device,rotation;public final boolean front;
        public Snapshot(int s,int d,boolean f){sensor=(s%360+360)%360;device=(d%360+360)%360;front=f;rotation=(sensor+(f?-device:device)+360)%360;}
        public String comment(){return "Silverroom; device_degrees="+device+"; sensor_degrees="+sensor+"; capture_rotation="+rotation+"; front="+front+"; pixels_upright=true";}
    }
}
