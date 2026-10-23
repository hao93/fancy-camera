package com.silverroom.camera;

import android.graphics.*;

public final class Film {
    public static final String[] NAMES=FilmCatalog.NAMES;
    public static final String[] TAGS=FilmCatalog.TAGS;
    public static class Settings {
        public int stock=1; public float cyan,magenta,yellow,exposure,grain=.20f,fade=.10f,strength=1;
        public Settings copy(){ Settings s=new Settings();s.stock=stock;s.cyan=cyan;s.magenta=magenta;s.yellow=yellow;s.exposure=exposure;s.grain=grain;s.fade=fade;s.strength=strength;return s; }
    }
    // Same per-channel tone curve is used in the live GPU preview and JPEG processing.
    public static float[] parameters(Settings s){
        return FilmCatalog.parameters(s.stock,s.cyan,s.magenta,s.yellow,s.exposure,s.grain,s.fade);
    }
    public static Bitmap develop(Bitmap input,Settings s){
        int w=input.getWidth(),h=input.getHeight(); Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        int[] row=new int[w];float[] p=parameters(s); int[][] lut=new int[3][256];
        for(int c=0;c<3;c++)for(int i=0;i<256;i++)lut[c][i]=Math.round(FilmCatalog.channel(i/255f,c,p)*255);
        float strength=FilmStrength.clamp(s.strength),baseGain=FilmStrength.exposure(s.exposure);
        int seed=2397;boolean tilt=p[21]>0&&strength>0,finish=p[20]>0||p[23]>0;int rx=FilmEffects.radius(w),ry=FilmEffects.radius(h);int[] top=tilt?new int[w]:null,mid=tilt?new int[w]:null,bottom=tilt?new int[w]:null;
        for(int y=0;y<h;y++){
            input.getPixels(row,0,w,0,y,w,1);float ny=(y/(float)h-.5f)*2,fy=(y+.5f)/h,soft=tilt?FilmEffects.tilt(fy)*p[21]:0;
            if(soft>0){input.getPixels(top,0,w,0,Math.max(0,y-ry),w,1);System.arraycopy(row,0,mid,0,w);input.getPixels(bottom,0,w,0,Math.min(h-1,y+ry),w,1);}
            for(int x=0;x<w;x++){
                int original=row[x];if(strength==0){row[x]=FilmStrength.blend(original,original,0,baseGain);continue;}int v=original;if(soft>0)v=FilmEffects.blend(v,FilmEffects.blurred(top,mid,bottom,x,rx),soft);int r=lut[0][(v>>16)&255],g=lut[1][(v>>8)&255],b=lut[2][v&255];
                float l=r*.299f+g*.587f+b*.114f;
                float nx=(x/(float)w-.5f)*2;float vignette=1-p[22]*(nx*nx+ny*ny);
                seed=seed*1664525+1013904223;float noise=(((seed>>>16)&255)/255f-.5f)*p[9]*40;
                r=clip((l+(r-l)*p[6])*p[17]*vignette+noise);g=clip((l+(g-l)*p[6])*p[18]*vignette+noise);b=clip((l+(b-l)*p[6])*p[19]*vignette+noise);
                if(finish){float fx=(x+.5f)/w;r=clip(FilmEffects.finish(r/255f,0,fx,fy,p[20],p[23])*255);g=clip(FilmEffects.finish(g/255f,1,fx,fy,p[20],p[23])*255);b=clip(FilmEffects.finish(b/255f,2,fx,fy,p[20],p[23])*255);}
                row[x]=FilmStrength.blend(original,0xff000000|(r<<16)|(g<<8)|b,strength,baseGain);
            }out.setPixels(row,0,w,0,y,w,1);
        }return out;
    }
    private static int clip(float n){return Math.max(0,Math.min(255,Math.round(n)));}
}
