package com.silverroom.camera;

/** Inverse of the aspect-preserving viewport fill, before sensor rotation/mirror. */
public final class PreviewFraming {
    private PreviewFraming(){}
    public static float[] point(float x,float y,int sensor,int bw,int bh,int vw,int vh,float crop){
        if(bw<=0||bh<=0||vw<=0||vh<=0||crop<1)throw new IllegalArgumentException("Invalid preview frame");
        float aspect=sensor%180==0?bw/(float)bh:bh/(float)bw,view=vw/(float)vh;
        float sx=Math.max(1,aspect/view),sy=Math.max(1,view/aspect);
        return new float[]{.5f+(x-.5f)/(sx*crop),.5f+(y-.5f)/(sy*crop)};
    }
}
