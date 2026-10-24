package com.silverroom.camera;

/** Match a still to a verified, same-target preview. No pixel guesses or double application of EV. */
public final class ExposureMatch {
    private ExposureMatch(){}
    public static final class Sample {
        public final float target,aperture;public final Float reported;public final long time,duration,received;public final int iso;public final boolean settled;public final Integer boost;
        public Sample(float targetEv,Float compensation,Long ns,Integer sensitivity,Long frame,Float f,Integer state,long now){this(targetEv,compensation,ns,sensitivity,frame,f,state,now,null);}
        public Sample(float targetEv,Float compensation,Long ns,Integer sensitivity,Long frame,Float f,Integer state,long now,Integer digitalBoost){
            boost=digitalBoost!=null&&digitalBoost>0?digitalBoost:null;target=targetEv;reported=compensation;time=ns==null?0:ns;iso=sensitivity==null?0:sensitivity;duration=frame==null?0:frame;aperture=f==null?0:f;received=now;settled=state!=null&&(state==2||state==3||state==4);
        }
        public boolean ready(float desired,long now){return settled&&time>0&&iso>0&&now>=received&&now-received<=1500&&Math.abs(target-desired)<.001f&&(reported==null||Float.isFinite(reported)&&Math.abs(reported-desired)<.05f);}
    }
    public static final class Correction {
        public final float requested,amount;public final Float reported;public final String method;public final Integer previewBoost,captureBoost;
        Correction(float target,Float actual,float ev,String how,Integer before,Integer after){previewBoost=before;captureBoost=after;requested=target;reported=actual!=null&&Float.isFinite(actual)?actual:null;amount=Float.isFinite(ev)?Math.max(-4,Math.min(4,Math.abs(ev)<.08f?0:ev)):0;method=how;}
    }
    public static Correction compare(Sample preview,float desired,Float reported,long ns,int iso,float aperture,boolean manual,boolean flash){return compare(preview,desired,reported,ns,iso,aperture,manual,flash,null);}
    public static Correction compare(Sample preview,float desired,Float reported,long ns,int iso,float aperture,boolean manual,boolean flash,Integer digitalBoost){
        Integer before=preview==null?null:preview.boost,after=digitalBoost!=null&&digitalBoost>0?digitalBoost:null;
        if(!flash&&preview!=null&&preview.ready(desired,preview.received)&&(Math.abs(desired)>.001f||manual)&&ns>0&&iso>0){
            double ratio=(double)preview.time*preview.iso/((double)ns*iso);
            if(before!=null&&after!=null)ratio*=before/(double)after;
            if(preview.aperture>0&&aperture>0)ratio*=aperture*aperture/(preview.aperture*preview.aperture);
            return new Correction(desired,reported,(float)(Math.log(ratio)/Math.log(2)),manual?"sensor-transfer":"preview-sensor-match",before,after);
        }
        if(!manual&&reported!=null&&Float.isFinite(reported))return new Correction(desired,reported,desired-reported,"reported-compensation",before,after);
        return new Correction(desired,reported,0,manual?"sensor-transfer-unverified":"metadata-unavailable",before,after);
    }
}
