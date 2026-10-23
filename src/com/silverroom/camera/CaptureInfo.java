package com.silverroom.camera;
import java.util.Locale;

public final class CaptureInfo {
    public final int iso,base;public final long exposure,timestamp;public final float mm,physical,fNumber,ev;public final int awb;public final String cameraId;
    public CaptureInfo(int i,long e,long t,float m,float p,float f,float v,int a,int b,String id){iso=i;exposure=e;timestamp=t;mm=m;physical=p;fNumber=f;ev=v;awb=a;base=b;cameraId=id==null?"":id;}
    public String summary(){return String.format(Locale.US,"%dmm · %s · %s · %s · EV %+.1f",(int)mm,base+"mm 光学",iso>0?"ISO "+iso:"ISO 未返回",shutter(exposure),ev);}
    public static String shutter(long ns){if(ns<=0)return "快门未返回";return ns<1000000000L?"1/"+Math.round(1000000000d/ns)+"s":String.format(Locale.US,"%.1fs",ns/1e9);}
}
