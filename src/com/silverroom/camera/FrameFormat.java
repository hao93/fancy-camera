package com.silverroom.camera;

/** Shared finder and export framing. Ratios refer to a portrait-locked UI. */
public final class FrameFormat {
    public static final String[] LABELS={"铺满","4:3","3:2","1:1","16:9"};
    public static final String[] NAMES={"铺满取景框","原生画幅","胶片画幅","方形构图","宽幅电影"};
    public static final String[] NOTES={"与当前取景区域同宽高比","保留原生 4:3 画幅","经典 35mm 胶片比例","适合静物、肖像与分享","适合风景与电影感构图"};
    private static final float[] PORTRAIT={0,3f/4,2f/3,1,9f/16};
    private FrameFormat(){}
    public static int safe(int id){return id>=0&&id<LABELS.length?id:0;}
    public static float aspect(int id,float viewport){id=safe(id);return id==0?viewport:PORTRAIT[id];}
    public static int[] fit(int id,int width,int height){
        if(width<=0||height<=0)return new int[]{1,1};
        float a=aspect(id,width/(float)height);
        return width/(float)height>a?new int[]{Math.max(1,Math.round(height*a)),height}:new int[]{width,Math.max(1,Math.round(width/a))};
    }
    public static final class Snapshot {
        public final int format;public final float portraitAspect;
        public Snapshot(int id,int width,int height){if(width<=0||height<=0)throw new IllegalArgumentException("Empty finder");format=safe(id);portraitAspect=aspect(format,width/(float)height);}
        public float outputAspect(int deviceDegrees){return deviceDegrees%180==0?portraitAspect:1/portraitAspect;}
    }
    public static int[] crop(int width,int height,float aspect){
        if(width<=0||height<=0||!Float.isFinite(aspect)||aspect<0)throw new IllegalArgumentException("Invalid frame");
        if(aspect==0)return new int[]{0,0,width,height};
        int w=width,h=height;
        if(width/(float)height>aspect)w=Math.max(1,Math.min(width,Math.round(height*aspect)));
        else h=Math.max(1,Math.min(height,Math.round(width/aspect)));
        return new int[]{(width-w)/2,(height-h)/2,w,h};
    }
}
