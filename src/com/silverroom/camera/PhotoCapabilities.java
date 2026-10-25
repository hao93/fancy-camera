package com.silverroom.camera;

/** Intersected native controls for the selected optical route, never guessed from model names. */
public final class PhotoCapabilities {
    public final boolean manual,aeLock,compensation,flash,frameDuration;public final long maxFrameNs;public final int isoMin,isoMax;public final long timeMin,timeMax;public final int[] whites;public final float[] apertures;public final String reason;
    public PhotoCapabilities(boolean m,int lo,int hi,long tlo,long thi,int[] w,float[] f,String why){this(m,lo,hi,tlo,thi,w,f,why,false,false,false,false,Long.MAX_VALUE);}
    public PhotoCapabilities(boolean m,int lo,int hi,long tlo,long thi,int[] w,float[] f,String why,boolean lock,boolean ev,boolean light,boolean duration,long frameMax){aeLock=lock;compensation=ev;flash=light;frameDuration=duration;maxFrameNs=frameMax;manual=m;isoMin=lo;isoMax=hi;timeMin=tlo;timeMax=thi;whites=w==null?new int[]{1}:w.clone();apertures=f==null?new float[0]:f.clone();reason=why==null?"":why;}
    public static PhotoCapabilities none(){return new PhotoCapabilities(false,100,100,10000000,10000000,null,null,"等待镜头就绪");}
    public PhotoControls clamp(PhotoControls p){int white=1;for(int w:whites)if(w==p.awb)white=w;float f=0;for(float value:apertures)if(Math.abs(value-p.aperture)<.001f)f=value;return new PhotoControls(p.manual&&manual,Math.max(isoMin,Math.min(isoMax,p.iso)),Math.max(timeMin,Math.min(timeMax,p.exposureNs)),white,f);}
}
