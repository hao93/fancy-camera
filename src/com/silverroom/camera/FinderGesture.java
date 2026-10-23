package com.silverroom.camera;

/** One-finger state machine; raw exposure retains sub-tick movement but no out-of-range windup. */
public final class FinderGesture {
    public boolean active,dragging,locked,moved;public float x,y,value;private float lastY;private final float slop,pixelsPerEv;private final ExposureLimits limits;
    public FinderGesture(float threshold,float density,ExposureLimits range){slop=Math.max(1,threshold);pixelsPerEv=120*Math.max(.1f,density);limits=range;}
    public void begin(float px,float py,float ev){x=px;y=lastY=py;value=limits.snap(ev);active=true;dragging=locked=moved=false;}
    public boolean move(float px,float py){if(!active||!Float.isFinite(px)||!Float.isFinite(py))return false;float dx=Math.abs(px-x),dy=Math.abs(py-y);if(!dragging){if(Math.hypot(dx,dy)>slop)moved=true;if(!limits.supported()||dy<=slop||dy<dx*1.2f)return false;dragging=true;}value=Math.max(limits.min(),Math.min(limits.max(),value+(lastY-py)/pixelsPerEv));lastY=py;return true;}
    public boolean hold(){if(!active||moved||dragging)return false;locked=true;return true;}
    public boolean tap(){return active&&!dragging&&!locked&&!moved;}
    public void cancel(){active=false;dragging=false;}
}
