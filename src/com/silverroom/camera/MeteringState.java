package com.silverroom.camera;

/** Persistent mode, session-local points. Exposure locking is a separate state machine. */
public final class MeteringState {
    public static final int GLOBAL=0,CENTER=1,SPOT=2;
    public static int safe(int mode){return mode>=0&&mode<=2?mode:GLOBAL;}
    public static String label(int mode){return new String[]{"全局自动","中央区域","点测光"}[safe(mode)];}
    public static final class Snapshot {
        public final int mode;public final boolean hasFocus;public final float afX,afY,aeX,aeY;public final long revision;
        Snapshot(int m,boolean f,float x,float y,float ex,float ey,long r){mode=m;hasFocus=f;afX=x;afY=y;aeX=ex;aeY=ey;revision=r;}
        public boolean separated(){return hasFocus&&mode==SPOT&&(Math.abs(afX-aeX)>.01f||Math.abs(afY-aeY)>.01f);}
    }
    private volatile Snapshot value=new Snapshot(GLOBAL,false,.5f,.5f,.5f,.5f,0);
    private static float unit(float v){return Float.isFinite(v)?Math.max(0,Math.min(1,v)):.5f;}
    public Snapshot snapshot(){return value;}
    public synchronized void mode(int mode){Snapshot v=value;int next=safe(mode);if(next!=v.mode)value=new Snapshot(next,v.hasFocus,v.afX,v.afY,v.aeX,v.aeY,v.revision+1);}
    public synchronized void focus(float x,float y){Snapshot v=value;value=new Snapshot(v.mode,true,unit(x),unit(y),v.aeX,v.aeY,v.revision);}
    public synchronized void tap(float x,float y){Snapshot v=value;value=new Snapshot(SPOT,true,unit(x),unit(y),unit(x),unit(y),v.revision+1);}
    public synchronized void move(float x,float y){Snapshot v=value;float ex=unit(x),ey=unit(y);if(v.mode!=SPOT||Math.abs(v.aeX-ex)>.00001f||Math.abs(v.aeY-ey)>.00001f)value=new Snapshot(SPOT,v.hasFocus,v.afX,v.afY,ex,ey,v.revision+1);}
    public synchronized void remap(){Snapshot v=value;value=new Snapshot(v.mode,v.hasFocus,v.afX,v.afY,v.aeX,v.aeY,v.revision+1);}
    public synchronized void resetPoints(){Snapshot v=value;value=new Snapshot(v.mode,false,.5f,.5f,.5f,.5f,v.revision+1);}
}
