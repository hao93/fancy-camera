package com.silverroom.camera;

/** Balanced white-noise tile. No trigonometric hashes or floating-point amplification. */
public final class PreviewGrain {
    public static final int EDGE=512;
    private PreviewGrain(){}
    public static byte[] pixels(){
        byte[] pixels=new byte[EDGE*EDGE];for(int i=0;i<pixels.length;i++)pixels[i]=(byte)(i&255);
        int state=0x63d18a4f;for(int i=pixels.length-1;i>0;i--){state^=state<<13;state^=state>>>17;state^=state<<5;int j=(state&0x7fffffff)%(i+1);byte v=pixels[i];pixels[i]=pixels[j];pixels[j]=v;}return pixels;
    }
    public static float sample(byte[] pixels,int x,int y){if(pixels.length!=EDGE*EDGE)throw new IllegalArgumentException("Invalid grain tile");return (pixels[(y&(EDGE-1))*EDGE+(x&(EDGE-1))]&255)/255f-.5f;}
}
