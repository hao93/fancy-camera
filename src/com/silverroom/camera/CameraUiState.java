package com.silverroom.camera;

/** Immutable presentation snapshot; it cannot control or validate a camera session. */
public final class CameraUiState {
    public static final int CONNECTING=0,SWITCHING=1,READY=2,ERROR=3,PAUSED=4;
    public final int phase,base;public final float focal,crop;public final boolean ready;public final String reason;
    public CameraUiState(int stage,float mm,int optical,boolean usable,String message){phase=stage;focal=mm;base=optical;crop=optical>0?mm/optical:1;ready=usable;reason=message==null?"":message;}
    public static String source(float mm){int base=LensPlan.base(mm);String label=base==14?"超广":base==23?"主摄":"长焦";float ratio=mm/base;return label+" "+(Math.abs(ratio-1)<.005f?"1":String.format(java.util.Locale.US,"%.1f",ratio))+"×";}
    public String label(){return phase==SWITCHING?"切镜中":phase==CONNECTING?"连接中":phase==ERROR?(ready?"拍摄提示 · 查看":"镜头未就绪 · 查看"):"";}
}
