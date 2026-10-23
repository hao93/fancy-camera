package com.silverroom.camera;

/** Geometry for unrotated YUV frames. Texture rows are flipped separately in GLSL. */
public final class YuvGeometry {
    private YuvGeometry(){}
    public static void model(float[] m,int sensor,int display,boolean front,int bw,int bh,int vw,int vh){
        int relative=(sensor+(front?display:-display)+360)%360;
        if(relative%90!=0||bw<=0||bh<=0||vw<=0||vh<=0)throw new IllegalArgumentException("Invalid YUV geometry");
        float aspect=relative%180==0?bw/(float)bh:bh/(float)bw,view=vw/(float)vh;
        float sx=aspect>view?aspect/view:1,sy=aspect<view?view/aspect:1;
        if(front)sx=-sx;double angle=Math.toRadians(-relative);float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        java.util.Arrays.fill(m,0);m[0]=sx*c;m[1]=sy*s;m[4]=-sx*s;m[5]=sy*c;m[10]=m[15]=1;
    }
}
