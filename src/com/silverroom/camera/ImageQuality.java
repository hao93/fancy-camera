package com.silverroom.camera;

/** Shared bounded math for CPU, GL uniform preparation and camera calibration. */
public final class ImageQuality {
    private ImageQuality(){}
    public static float clamp(float value,float min,float max){return Float.isFinite(value)?Math.max(min,Math.min(max,value)):min;}
    public static void adjust(float[] p,float red,float green,float blue,float contrast,float ev,float highlights,boolean adaptive,int iso){
        float gain=(float)Math.pow(2,clamp(ev,-.5f,.5f));p[0]*=clamp(red,.85f,1.15f)*gain;p[1]*=clamp(green,.85f,1.15f)*gain;p[2]*=clamp(blue,.85f,1.15f)*gain;p[7]*=clamp(contrast,.9f,1.1f);
        p[16]+=clamp(highlights,0,1)*1.5f;if(adaptive)p[9]*=isoScale(iso);
    }
    public static float isoScale(int iso){return iso<=400?1:Math.max(.35f,1-(iso-400)/4000f);}
    public static float sizeScale(int edge){return clamp(edge/4096f,.55f,1);}
    public static int outputEdge(int w,int h,float crop,float portraitAspect){float scale=Math.min(1,4096f/Math.max(w,h));int[] size=LensPlan.cropSize(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)),crop);int[] rect=FrameFormat.crop(Math.min(size[0],size[1]),Math.max(size[0],size[1]),portraitAspect);return Math.max(rect[2],rect[3]);}
    public static int editEdge(int w,int h,float aspect){float scale=Math.min(1,4096f/Math.max(w,h));int[] rect=FrameFormat.crop(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)),aspect);return Math.max(rect[2],rect[3]);}
    public static float grainShade(float l){float t=clamp((l-.08f)/.42f,0,1);return .35f+.65f*t*t*(3-2*t);}
    public static float skin(float r,float g,float b){float spread=r-b;if(r<=g||g<=b||r-g>.35f||g-b>.30f||spread<.03f)return 0;return clamp((Math.min(r,1)-.12f)/.28f,0,1)*clamp((.65f-spread)/.3f,0,1);}
    public static float[] grey(float r,float g,float b){if(Math.min(r,Math.min(g,b))<.12f||Math.max(r,Math.max(g,b))>.88f)throw new IllegalArgumentException("灰卡过暗或过亮，请调整光线");float mean=(r+g+b)/3;return new float[]{clamp(mean/r,.85f,1.15f),clamp(mean/g,.85f,1.15f),clamp(mean/b,.85f,1.15f)};}
    public static long previewBudget(float mm,int profile,int mode){long budget=PreviewQuality.budget(mm,profile);return mode==1?Math.min(budget,1920L*1080):budget;}
    public static String optical(float mm){int base=LensPlan.base(mm);return ((int)mm)+"mm · "+(LensPlan.crop(mm)<1.01?"原生光学":base+"mm 光学 + "+String.format(java.util.Locale.US,"%.2f× 裁切",LensPlan.crop(mm)));}
}
