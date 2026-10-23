package com.silverroom.camera;

/** Blend the complete look, including spatial effects, while keeping exposure independent. */
public final class FilmStrength {
    private FilmStrength(){}
    public static float clamp(float n){return Float.isFinite(n)?Math.max(0,Math.min(1,n)):1;}
    public static int percent(float n){return Math.round(clamp(n)*100);}
    public static float exposure(float ev){return (float)Math.pow(2,ev);}
    public static int blend(int original,int developed,float amount,float gain){
        float a=clamp(amount);if(a==1)return developed;int out=0xff000000;
        for(int shift=0;shift<=16;shift+=8){float raw=Math.min(255,((original>>shift)&255)*gain),look=(developed>>shift)&255;out|=Math.max(0,Math.min(255,Math.round(raw+(look-raw)*a)))<<shift;}return out;
    }
}
