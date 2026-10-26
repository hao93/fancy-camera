package com.silverroom.camera;

import android.hardware.camera2.*;
import android.hardware.camera2.params.TonemapCurve;
import java.util.*;

/** Reuse the verified preview's reported curve; never guess a brightness offset or vendor LUT. */
final class StillTone {
    private StillTone(){}
    static final class Snapshot {
        final ExposureMatch.Sample sample;final TonemapCurve curve;
        Snapshot(ExposureMatch.Sample exposure,TonemapCurve tone){sample=exposure;curve=tone;}
    }
    static Snapshot read(CaptureResult result,ExposureMatch.Sample sample){
        if(result==null||sample==null)return null;
        Long time=result.get(CaptureResult.SENSOR_EXPOSURE_TIME);Integer iso=result.get(CaptureResult.SENSOR_SENSITIVITY);
        TonemapCurve curve=result.get(CaptureResult.TONEMAP_CURVE);
        return curve!=null&&time!=null&&time==sample.time&&iso!=null&&iso==sample.iso?new Snapshot(sample,curve):null;
    }
    static Snapshot match(Snapshot tone,ExposureMatch.Sample exposure){return tone!=null&&exposure!=null&&tone.sample==exposure?tone:null;}
    private static boolean has(int[] modes,int mode){if(modes!=null)for(int n:modes)if(n==mode)return true;return false;}
    static boolean valid(TonemapCurve curve,int maximum){
        if(curve==null||maximum<2)return false;
        for(int channel=0;channel<3;channel++){
            int count=curve.getPointCount(channel);if(count<2||count>maximum)return false;
            float[] points=new float[count*2];curve.copyColorCurve(channel,points,0);float x=-1,y=-1;
            for(int i=0;i<points.length;i+=2){float nextX=points[i],nextY=points[i+1];if(!Float.isFinite(nextX)||!Float.isFinite(nextY)||nextX<0||nextX>1||nextY<0||nextY>1||nextX<=x||nextY<y)return false;x=nextX;y=nextY;}
            if(Math.abs(points[0])>.00001f||Math.abs(points[points.length-2]-1)>.00001f)return false;
        }return true;
    }
    static boolean supported(CameraCharacteristics owner,CameraCharacteristics sensor,String physical,TonemapCurve curve){
        if(owner==null||sensor==null)return false;
        if(!has(owner.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING)||!has(sensor.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING))return false;
        if(!has(owner.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES),CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE)||!has(sensor.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES),CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE))return false;
        Integer a=owner.get(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS),b=sensor.get(CameraCharacteristics.TONEMAP_MAX_CURVE_POINTS);
        List<CaptureRequest.Key<?>> keys=owner.getAvailableCaptureRequestKeys(),physicalKeys=physical==null?keys:owner.getAvailablePhysicalCameraRequestKeys();
        return a!=null&&b!=null&&valid(curve,Math.min(a,b))&&keys!=null&&physicalKeys!=null&&keys.containsAll(Arrays.asList(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_CURVE))&&physicalKeys.containsAll(Arrays.asList(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_CURVE));
    }
    static boolean apply(CaptureRequest.Builder request,Snapshot tone,CameraCharacteristics owner,CameraCharacteristics sensor,String physical){
        if(tone==null||!supported(owner,sensor,physical,tone.curve))return false;
        request.set(CaptureRequest.TONEMAP_CURVE,tone.curve);request.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
        if(physical!=null){request.setPhysicalCameraKey(CaptureRequest.TONEMAP_CURVE,tone.curve,physical);request.setPhysicalCameraKey(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE,physical);}return true;
    }
}
