package com.silverroom.camera;

/** Physical-camera suggestions. Never changes the phone camera or the film renderer. */
public final class MeterMath {
    private MeterMath(){}
    public static double log2(double x){return Math.log(x)/Math.log(2);}
    private static void positive(double n){if(!Double.isFinite(n)||n<=0)throw new IllegalArgumentException("请输入大于零的有效参数");}
    public static double ev100(double aperture,double seconds,double sensorIso){positive(aperture);positive(seconds);positive(sensorIso);return log2(aperture*aperture/seconds)-log2(sensorIso/100);}
    public static double ei(double nominal,double push){positive(nominal);if(!Double.isFinite(push)||Math.abs(push)>6)throw new IllegalArgumentException("迫冲档数超出范围");return nominal*Math.pow(2,push);}
    public static double seconds(double ev100,double aperture,double ei,double compensation,double nd){positive(aperture);positive(ei);if(!Double.isFinite(ev100+compensation+nd))throw new IllegalArgumentException("测光数据不可用");return aperture*aperture/Math.pow(2,ev100+log2(ei/100)-compensation-nd);}
    public static double aperture(double ev100,double seconds,double ei,double compensation,double nd){positive(seconds);positive(ei);if(!Double.isFinite(ev100+compensation+nd))throw new IllegalArgumentException("测光数据不可用");return Math.sqrt(seconds*Math.pow(2,ev100+log2(ei/100)-compensation-nd));}
    public static double reciprocity(double seconds,double factor){positive(seconds);positive(factor);return seconds<=1?seconds:Math.pow(seconds,factor);}
    public static double hyperfocal(double focalMm,double fNumber,double cocMm){positive(focalMm);positive(fNumber);positive(cocMm);return (focalMm*focalMm/(fNumber*cocMm)+focalMm)/1000;}
    public static double[] depth(double focalMm,double fNumber,double distanceMetres,double cocMm){positive(distanceMetres);double h=hyperfocal(focalMm,fNumber,cocMm),f=focalMm/1000,d=distanceMetres;if(d<=f)throw new IllegalArgumentException("物距须大于镜头焦距");return new double[]{h*d/(h+d-f),h<=d-f?Double.POSITIVE_INFINITY:h*d/(h-d+f)};}
    public static double flashFraction(double gn100,double fNumber,double distance,double ei,double nd){positive(gn100);positive(fNumber);positive(distance);positive(ei);double a=fNumber*distance/(gn100*Math.sqrt(ei/100));return a*a*Math.pow(2,nd);}
    public static String time(double s){if(!Double.isFinite(s)||s<=0)return "—";return s<1?"1/"+Math.max(1,Math.round(1/s))+"s":String.format(java.util.Locale.US,s<10?"%.2fs":"%.1fs",s);}
    public static double reciprocityFactor(String model){if(model==null)return 1;switch(model){case "HP5+":case "XP2":return 1.31;case "FP4+":case "Delta 100":case "Kentmere 100":return 1.26;case "Delta 400":return 1.41;case "Delta 3200":case "Pan F+":return 1.33;case "SFX":return 1.43;case "Ortho+":return 1.25;case "Kentmere 400":return 1.30;default:return 1;}}
    public static final String RECIPROCITY_SOURCE="ILFORD / HARMAN · Dec 2023 · https://www.ilfordphoto.com/amfile/file/download/file/1964/product/698/";
}
