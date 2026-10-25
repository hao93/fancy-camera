package com.silverroom.camera;

import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.*;
import java.util.*;

/** Metadata eligibility only. Runtime request/result/image checks remain mandatory. */
final class LogicalCameraPlan {
    final String owner; final CameraCharacteristics characteristics; final Size preview,jpeg;
    final Map<String,Integer> bases; final Map<Integer,String> physical;
    private LogicalCameraPlan(String id,CameraCharacteristics c,Size p,Size j,Map<String,Integer> b,Map<Integer,String> ids){owner=id;characteristics=c;preview=p;jpeg=j;bases=Collections.unmodifiableMap(b);physical=Collections.unmodifiableMap(ids);}
    LensPlan.Route route(float mm){return new LensPlan.Route(owner,null,LensPlan.base(mm),mm,false,false,true);}
    static LogicalCameraPlan create(CameraManager manager,String id,List<LensPlan.Route> routes,int mode,int maxEdge)throws CameraAccessException {
        CameraCharacteristics c=manager.getCameraCharacteristics(id);Range<Float> range=c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);
        if(range==null||!range.contains(LogicalZoomState.ratio(14))||!range.contains(LogicalZoomState.ratio(230)))throw new IllegalArgumentException("逻辑变焦范围未覆盖14–230mm");
        Map<String,Integer> bases=new HashMap<>();Map<Integer,String> physical=new HashMap<>();
        for(LensPlan.Route r:routes)if(r.owner.equals(id)&&r.physical!=null){bases.put(r.physical,r.base);physical.put(r.base,r.physical);}
        if(!physical.keySet().containsAll(Arrays.asList(14,23,85)))throw new IllegalArgumentException("缺少完整的光学镜头映射");
        int previewW=0,previewH=0,jpegW=0,jpegH=0;
        for(int base:new int[]{14,23,85}){
            CameraCharacteristics sensor=manager.getCameraCharacteristics(physical.get(base));StreamConfigurationMap map=sensor.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if(map==null)throw new IllegalArgumentException("物理镜头没有尺寸信息");
            int format=base==23?ImageFormat.PRIVATE:ImageFormat.YUV_420_888;Size[] sizes=map.getOutputSizes(format);
            if(sizes==null||sizes.length==0)throw new IllegalArgumentException("物理镜头没有取景尺寸");
            int[][] values=new int[sizes.length][2];long[] durations=new long[sizes.length];
            for(int n=0;n<sizes.length;n++){values[n][0]=sizes[n].getWidth();values[n][1]=sizes[n].getHeight();durations[n]=map.getOutputMinFrameDuration(format,sizes[n]);}
            Size baseline=sizes[PreviewQuality.choose(values,durations,ImageQuality.previewBudget(ZoomScale.sessionFocal(base),0,mode),Math.min(4096,maxEdge))];
            previewW=Math.max(previewW,baseline.getWidth());previewH=Math.max(previewH,baseline.getHeight());
            Size shot=baselinePhoto(map.getOutputSizes(base==23?ImageFormat.JPEG:ImageFormat.YUV_420_888));jpegW=Math.max(jpegW,shot.getWidth());jpegH=Math.max(jpegH,shot.getHeight());
        }
        StreamConfigurationMap map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(map==null)throw new IllegalArgumentException("逻辑镜头没有输出配置");
        Size preview=atLeast(map.getOutputSizes(ImageFormat.PRIVATE),previewW,previewH,Math.min(4096,maxEdge));
        Size jpeg=atLeast(map.getOutputSizes(ImageFormat.JPEG),jpegW,jpegH,4096);
        return new LogicalCameraPlan(id,c,preview,jpeg,bases,physical);
    }
    private static Size baselinePhoto(Size[] sizes){
        if(sizes==null||sizes.length==0)throw new IllegalArgumentException("缺少基准拍照尺寸");Size best=null;double score=Double.MAX_VALUE;
        for(Size s:sizes){long pixels=(long)s.getWidth()*s.getHeight();if(pixels>13000000)continue;double next=Math.abs(s.getWidth()/(double)s.getHeight()-4d/3)*10+Math.abs(Math.log(13000000d/pixels))*.1;if(next<score){score=next;best=s;}}
        if(best==null)best=Collections.min(Arrays.asList(sizes),(a,b)->Long.compare((long)a.getWidth()*a.getHeight(),(long)b.getWidth()*b.getHeight()));return best;
    }
    private static Size atLeast(Size[] sizes,int width,int height,int maxEdge){
        Size best=null;if(sizes!=null)for(Size s:sizes)if(s.getWidth()>=width&&s.getHeight()>=height&&Math.max(s.getWidth(),s.getHeight())<=maxEdge&&Math.abs(s.getWidth()/(double)s.getHeight()-4d/3)<.02){if(best==null||(long)s.getWidth()*s.getHeight()<(long)best.getWidth()*best.getHeight())best=s;}
        if(best==null)throw new IllegalArgumentException("无法保留现有预览／拍照尺寸 "+width+"×"+height);return best;
    }
}
