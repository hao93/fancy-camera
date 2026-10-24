package com.silverroom.camera;

/** Immutable Camera2 ticks inside the previous +/-2EV hardware envelope; soft UI levels use ExposureScale. */
public final class ExposureLimits {
    public static final ExposureLimits NONE=new ExposureLimits(0,0,0);
    public final int low,high;public final float step;
    public ExposureLimits(int min,int max,float unit){step=Float.isFinite(unit)&&unit>0?unit:0;low=step==0?0:Math.max(min,(int)Math.ceil(-2d/step-1e-6));high=step==0?0:Math.min(max,(int)Math.floor(2d/step+1e-6));}
    public boolean supported(){return step>0&&high>low;}
    public int ticks(float value){return !supported()?0:Math.max(low,Math.min(high,Math.round((Float.isFinite(value)?value:0)/step)));}
    public float snap(float value){return ticks(value)*step;}
    public float min(){return supported()?low*step:0;}public float max(){return supported()?high*step:0;}
    public int progress(float value){return supported()?ticks(value)-low:0;}
    public float value(int progress){return supported()?(low+Math.max(0,Math.min(high-low,progress)))*step:0;}
}
