package com.silverroom.camera;

/** Shared display geometry only. Never used by camera or image treatment code. */
public final class UiSpec {
    private UiSpec(){}
    public static final int TITLE=20,BODY=14,CAPTION=12,TARGET=48,RADIUS=12;
    public static int toolHeight(float font){return Math.max(48,(int)Math.ceil(24+24*font));}
    public static int cameraToolsHeight(){return TARGET;}
    public static int captureHeight(float font){return Math.max(96,60+(int)Math.ceil(36*Math.max(1,font)));}
    public static int captureSide(int width){return Math.max(100,Math.min(112,(width-90)/2));}
    public static int galleryActionHeight(float font){return Math.max(48,(int)Math.ceil(18*Math.max(1,font)+28));}
    public static int galleryActionWidth(float font){return Math.max(56,(int)Math.ceil(28*Math.max(1,font)+24));}
    public static boolean galleryWrap(int width,float font){return width<48+(int)Math.ceil(42*Math.max(1,font)+8)+3*galleryActionWidth(font)+24;}
    public static int filmHeight(float font){return (int)Math.ceil(48*Math.max(1,font)+16);}
    public static int lensHeight(float font){return (int)Math.ceil(60*Math.max(1,font)+16);}
    public static int columns(boolean detail,int width,float font){int minimum=(int)Math.ceil((detail?160:104)*Math.max(1,font));return Math.max(1,Math.min(detail?2:3,width/Math.max(1,minimum)));}
    public static int cardHeight(boolean detail,int width,int columns,float font){int picture=Math.max(84,(int)((width/(float)columns-12)*(detail?1.1f:.85f)));return picture+(int)Math.ceil(40*Math.max(1,font))+(int)Math.ceil((detail?36:24)*Math.max(1,font))+8;}
    public static int adjusted(float cyan,float magenta,float yellow,float grain,float fade){int n=0;for(float v:new float[]{cyan,magenta,yellow,grain-.2f,fade-.1f})if(Math.abs(v)>.005f)n++;return n;}
}
