package com.silverroom.camera;

/** One-finger state machine; raw exposure retains sub-tick movement but no out-of-range windup. */
public final class FinderGesture {
    public boolean active,dragging,locked,moved;public float x,y,value;private float lastAxis;private int orientation;private final float slop,pixelsPerEv;private final ExposureLimits limits;
    public FinderGesture(float threshold,float density,ExposureLimits range){slop=Math.max(1,threshold);pixelsPerEv=120*Math.max(.1f,density);limits=range;}
    public void begin(float px,float py,float ev){begin(px,py,ev,0);}
    public void begin(float px,float py,float ev,int device){orientation=device>=0&&device%90==0?device%360:0;x=px;y=py;lastAxis=axis(px,py);value=ExposureScale.snap(ev,limits);active=true;dragging=locked=moved=false;}
    public static float projectAxis(float px,float py,int orientation){return orientation==90?px:orientation==180?-py:orientation==270?-px:py;}
    private float axis(float px,float py){return projectAxis(px,py,orientation);}
    public boolean move(float px,float py){if(!active||!Float.isFinite(px)||!Float.isFinite(py))return false;float along=Math.abs(axis(px,py)-axis(x,y)),across=orientation%180==0?Math.abs(px-x):Math.abs(py-y);if(!dragging){if(Math.hypot(px-x,py-y)>slop)moved=true;if(!limits.supported()||along<=slop||along<across*1.2f)return false;dragging=true;}float current=axis(px,py);value=Math.max(ExposureScale.min(limits),Math.min(ExposureScale.max(limits),value+(lastAxis-current)/pixelsPerEv));lastAxis=current;return true;}
    public boolean hold(){if(!active||moved||dragging)return false;locked=true;return true;}
    public boolean tap(){return active&&!dragging&&!locked&&!moved;}
    public void cancel(){active=false;dragging=false;}
}
