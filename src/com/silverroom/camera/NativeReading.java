package com.silverroom.camera;

/** Result metadata after route/request validation, independent of rendered RGB pixels. */
public final class NativeReading {
    public final long timestamp,receivedAt,epoch,exposureNs;public final int iso,awb,base,aeState;public final float aperture,focal,hardwareEv,focusDiopters;public final int distanceCalibration;public final String source;public final boolean verified;public final double ev100;
    public NativeReading(long t,long now,long version,int sensitivity,long ns,float f,float mm,int optical,int white,int state,float bias,float diopters,int calibration,String camera,boolean valid,int boost){timestamp=t;receivedAt=now;epoch=version;iso=sensitivity;exposureNs=ns;aperture=f;focal=mm;base=optical;awb=white;aeState=state;hardwareEv=bias;focusDiopters=diopters;distanceCalibration=calibration;source=camera==null?"":camera;verified=valid;double result=Double.NaN;try{result=MeterMath.ev100(f,ns/1e9,sensitivity*Math.max(1,boost)/100d)+bias;}catch(IllegalArgumentException ignored){}ev100=result;}
    public boolean ready(long now,long currentEpoch){return verified&&timestamp>0&&epoch==currentEpoch&&now>=receivedAt&&now-receivedAt<=1000&&Double.isFinite(ev100)&&(aeState==2||aeState==4);}
    public double estimatedFocusMetres(){return distanceCalibration==2&&Float.isFinite(focusDiopters)&&focusDiopters>0?1d/focusDiopters:Double.NaN;}
}
