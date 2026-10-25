package com.silverroom.camera;
import android.hardware.camera2.*;import android.util.Range;import java.util.*;

/** Native request capability checks shared by the PRO panel, preview and still output. */
final class NativeCameraControls {
    private NativeCameraControls(){}
    static boolean has(int[] values,int n){if(values!=null)for(int v:values)if(v==n)return true;return false;}
    static PhotoCapabilities capabilities(CameraCharacteristics owner,CameraCharacteristics sensor,String physical){
        if(owner==null||sensor==null)return PhotoCapabilities.none();List<CaptureRequest.Key<?>> keys=physical==null?owner.getAvailableCaptureRequestKeys():owner.getAvailablePhysicalCameraRequestKeys();
        Range<Integer> a=owner.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE),b=sensor.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);Range<Long> t=owner.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE),u=sensor.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        int lo=a==null||b==null?100:Math.max(a.getLower(),b.getLower()),hi=a==null||b==null?100:Math.min(a.getUpper(),b.getUpper());long min=t==null||u==null?10000000:Math.max(t.getLower(),u.getLower()),max=t==null||u==null?10000000:Math.min(t.getUpper(),u.getUpper());
        boolean nativeManual=has(owner.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)&&has(sensor.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)&&has(owner.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),CaptureRequest.CONTROL_AE_MODE_OFF)&&has(sensor.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),CaptureRequest.CONTROL_AE_MODE_OFF)&&keys!=null&&keys.containsAll(Arrays.asList(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.SENSOR_EXPOSURE_TIME,CaptureRequest.SENSOR_SENSITIVITY))&&lo<=hi&&min<=max;
        List<Integer> whites=new ArrayList<>();if(keys!=null&&keys.contains(CaptureRequest.CONTROL_AWB_MODE)){int[] modes=sensor.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);if(modes!=null)for(int w:modes)if(w!=CaptureRequest.CONTROL_AWB_MODE_OFF&&has(owner.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),w))whites.add(w);}int[] wb=new int[whites.size()];for(int i=0;i<wb.length;i++)wb[i]=whites.get(i);
        float[] apertures=sensor.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);if(apertures!=null&&apertures.length>1&&(keys==null||!keys.contains(CaptureRequest.LENS_APERTURE)))apertures=new float[]{apertures[0]};
        Long frame=owner.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION),sensorFrame=sensor.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);long frameMax=Math.min(frame==null?Long.MAX_VALUE:frame,sensorFrame==null?Long.MAX_VALUE:sensorFrame);max=Math.min(max,frameMax);return new PhotoCapabilities(nativeManual&&min<=max,lo,hi,min,max,wb,apertures,nativeManual?"原生手动曝光可用":"此镜头未开放原生手动 ISO / 快门，保留自动拍摄",keys!=null&&keys.contains(CaptureRequest.CONTROL_AE_LOCK),keys!=null&&keys.contains(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION),keys!=null&&keys.contains(CaptureRequest.FLASH_MODE),keys!=null&&keys.contains(CaptureRequest.SENSOR_FRAME_DURATION),frameMax);
    }
    static <T> void set(CaptureRequest.Builder b,String physical,CaptureRequest.Key<T> key,T value){if(physical==null)b.set(key,value);else b.setPhysicalCameraKey(key,value,physical);}
    static void apply(CaptureRequest.Builder b,PhotoControls controls,PhotoCapabilities caps,String physical){
        PhotoControls p=caps.clamp(controls);
        if(p.manual){set(b,physical,CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF);if(caps.aeLock)set(b,physical,CaptureRequest.CONTROL_AE_LOCK,false);if(caps.compensation)set(b,physical,CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,0);set(b,physical,CaptureRequest.SENSOR_SENSITIVITY,p.iso);set(b,physical,CaptureRequest.SENSOR_EXPOSURE_TIME,p.exposureNs);if(caps.flash)set(b,physical,CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_OFF);if(caps.frameDuration)set(b,physical,CaptureRequest.SENSOR_FRAME_DURATION,Math.min(caps.maxFrameNs,Math.max(33333333L,p.exposureNs)));}
        // Auto white balance is already part of the device template. Only expose native presets.
        for(int mode:caps.whites)if(mode==p.awb){set(b,physical,CaptureRequest.CONTROL_AWB_MODE,mode);break;}
        if(caps.apertures.length>1)set(b,physical,CaptureRequest.LENS_APERTURE,p.aperture>0?p.aperture:null);
    }
}
