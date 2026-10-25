package com.silverroom.camera;

/** Immutable phone controls; film nominal ISO and external metering EI never enter this object. */
public final class PhotoControls {
    public final boolean manual;public final int iso,awb;public final long exposureNs;public final float aperture;
    public PhotoControls(boolean m,int i,long t,int white,float f){manual=m;iso=Math.max(1,i);exposureNs=Math.max(1,t);awb=white;aperture=Float.isFinite(f)&&f>0?f:0;}
    public boolean matches(int actualIso,long actualNs){return !manual||actualIso>0&&actualNs>0&&Math.abs(actualIso-iso)<=Math.max(2,iso*.05)&&Math.abs(actualNs-exposureNs)<=Math.max(100000,exposureNs*.05);}
    public static PhotoControls automatic(){return new PhotoControls(false,100,10000000,1,0);}
    public PhotoControls exposure(int i,long t){return new PhotoControls(true,i,t,awb,aperture);}
    public PhotoControls white(int w){return new PhotoControls(manual,iso,exposureNs,w,aperture);}
    public PhotoControls aperture(float f){return new PhotoControls(manual,iso,exposureNs,awb,f);}
}
