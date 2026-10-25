package com.silverroom.camera;

import java.util.*;

/** Proof of a logical frame, not a prediction based on the requested zoom. */
final class LogicalZoomState {
    static final int STALE=0, VALID=1, INVALID=2;
    static final class Tag {
        final long zoom,meter; final float focal;
        Tag(long z,long m,float f){zoom=z;meter=m;focal=f;}
    }
    static final class Proof {
        final long timestamp,epoch; final float focal; final String physical;
        Proof(long t,long e,float f,String p){timestamp=t;epoch=e;focal=f;physical=p;}
    }
    private final Map<String,Integer> bases=new HashMap<>();
    private final LinkedHashMap<Long,Proof> results=new LinkedHashMap<>();
    private long epoch,imageTimestamp; private float focal=23; private String reason="";
    synchronized void reset(Map<String,Integer> physical){epoch++;bases.clear();bases.putAll(physical);results.clear();imageTimestamp=0;reason="";}
    synchronized long begin(float mm){LensPlan.base(mm);epoch++;focal=mm;results.clear();imageTimestamp=0;reason="";return epoch;}
    synchronized long epoch(){return epoch;}
    synchronized Tag tag(long meter){return new Tag(epoch,meter,focal);}
    static long meter(Object tag){return tag instanceof Tag?((Tag)tag).meter:tag instanceof Long?(Long)tag:Long.MIN_VALUE;}
    static float ratio(float mm){LensPlan.base(mm);return mm/23f;}
    synchronized String validate(Object value,Long timestamp,String physical,Float ratio){
        if(!(value instanceof Tag))return "缺少变焦请求标记";
        Tag tag=(Tag)value;
        if(tag.zoom!=epoch||Math.abs(tag.focal-focal)>.049f)return "过期变焦请求";
        if(timestamp==null||timestamp<=0)return "缺少图像时间戳";
        Integer base=bases.get(physical);
        if(base==null||base!=LensPlan.base(tag.focal))return "实际物理镜头不符："+physical;
        float desired=ratio(tag.focal);
        if(ratio==null||!Float.isFinite(ratio)||Math.abs(ratio-desired)>Math.max(.01f,desired*.01f))return "实际变焦倍率不符："+ratio;
        return "";
    }
    synchronized int result(Object tag,Long timestamp,String physical,Float ratio){
        if(!(tag instanceof Tag)||((Tag)tag).zoom!=epoch)return STALE;
        reason=validate(tag,timestamp,physical,ratio);
        if(!reason.isEmpty())return INVALID;
        results.put(timestamp,new Proof(timestamp,epoch,focal,physical));
        while(results.size()>16)results.remove(results.keySet().iterator().next());
        return VALID;
    }
    synchronized void image(long timestamp){imageTimestamp=timestamp;}
    synchronized Proof proof(){Proof p=results.get(imageTimestamp);return p!=null&&p.epoch==epoch?p:null;}
    synchronized String reason(){return reason;}
    synchronized void clear(){epoch++;results.clear();imageTimestamp=0;}
}
