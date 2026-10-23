package com.silverroom.camera;

/** Normalized spatial recipe operations shared by export and the GPU shader formulas. */
public final class FilmEffects {
    private FilmEffects(){}
    public static float tilt(float y){float t=Math.max(0,Math.min(1,(Math.abs(y-.5f)-.12f)/.30f));return t*t*(3-2*t);}
    public static int radius(int length){return Math.max(1,Math.round(length*.012f));}
    public static float leak(float x,float y,float strength){float left=Math.max(0,1-x/.42f),right=Math.max(0,1-(1-x)/.20f);return strength*(left*left*left+.30f*right*right*right)*(.8f+.2f*(1-2*Math.abs(y-.5f)));}
    public static float finish(float value,int channel,float x,float y,float leak,float steps){
        float glow=leak(x,y,leak)*(channel==0?.85f:channel==1?.28f:.09f);float v=1-(1-value)*(1-glow);
        if(steps>0)v=(float)Math.floor(Math.max(0,Math.min(1,v))*steps+.5f)/steps;return v;
    }
    /** Three separable 1:2:1 taps, clamped to image edges; no full-size scratch bitmap. */
    public static int blurred(int[] top,int[] mid,int[] bottom,int x,int radius){
        int l=Math.max(0,x-radius),r=Math.min(mid.length-1,x+radius),out=0xff000000;
        for(int shift=0;shift<=16;shift+=8){int sum=((top[l]>>shift)&255)+2*((top[x]>>shift)&255)+((top[r]>>shift)&255)+2*((mid[l]>>shift)&255)+4*((mid[x]>>shift)&255)+2*((mid[r]>>shift)&255)+((bottom[l]>>shift)&255)+2*((bottom[x]>>shift)&255)+((bottom[r]>>shift)&255);out|=((sum+8)/16)<<shift;}return out;
    }
    public static int blend(int sharp,int blurred,float amount){int out=0xff000000;for(int shift=0;shift<=16;shift+=8){float a=(sharp>>shift)&255,b=(blurred>>shift)&255;out|=Math.round(a+(b-a)*amount)<<shift;}return out;}
}
