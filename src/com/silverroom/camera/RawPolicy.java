package com.silverroom.camera;
public final class RawPolicy {
    private RawPolicy(){}
    public static boolean pair(long image,long result){return image>0&&image==result;}
    public static boolean size(int w,int h){return w>0&&h>0&&(long)w*h<=50000000L;}
    public static int orientation(int degrees){return degrees==90?6:degrees==180?3:degrees==270?8:1;}
}
