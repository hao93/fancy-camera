package com.silverroom.camera;

/** Logarithmic travel between stable anchors; unavailable optical ranges are never substituted. */
public final class ZoomScale {
    private ZoomScale(){}
    public static final int[] ANCHORS={14,23,35,50,85,135,230};
    public static float focal(float position){float p=Math.max(0,Math.min(6,position));int i=Math.min(5,(int)p);return Math.round((float)(ANCHORS[i]*Math.pow(ANCHORS[i+1]/(double)ANCHORS[i],p-i))*10)/10f;}
    public static float position(float mm){LensPlan.base(mm);for(int i=0;i<6;i++)if(mm<=ANCHORS[i+1])return i+(float)(Math.log(mm/ANCHORS[i])/Math.log(ANCHORS[i+1]/(double)ANCHORS[i]));return 6;}
    public static boolean allowed(float mm,boolean[] enabled){int base=LensPlan.base(mm);return enabled[base==14?0:base==23?1:4];}
    public static float constrain(float position,boolean[] enabled){
        float best=Float.NaN,distance=Float.MAX_VALUE;
        float[] starts={0,1,4},ends={position(22.9f),position(84.9f),6};int[] bases={0,1,4};
        for(int i=0;i<3;i++)if(enabled[bases[i]]){float p=Math.max(starts[i],Math.min(ends[i],position)),d=Math.abs(p-position);if(d<distance){best=p;distance=d;}}
        return best;
    }
    public static float snap(float position,boolean[] enabled){float p=constrain(position,enabled);if(!Float.isFinite(p))return p;int i=Math.round(p);return enabled[i]&&Math.abs(p-i)<=.18f?i:p;}
    public static float sessionFocal(float mm){int base=LensPlan.base(mm);return base==14?22.9f:base==23?84.9f:230;}
    public static String label(float mm){return mm==Math.round(mm)?Integer.toString(Math.round(mm)):String.format(java.util.Locale.US,"%.1f",mm);}
}
