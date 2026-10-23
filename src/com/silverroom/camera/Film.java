package com.silverroom.camera;

import android.graphics.*;

public final class Film {
    public static final String[] NAMES=FilmCatalog.NAMES;
    public static final String[] TAGS=FilmCatalog.TAGS;
    public static class Settings {
        public int stock=1; public float cyan,magenta,yellow,exposure,grain=.20f,fade=.10f,strength=1;
        public float skin,highlights,red=1,green=1,blue=1,contrast=1,calibrationEv;public boolean adaptiveGrain;public int iso,grainEdge=4096;
        public Settings copy(){ Settings s=new Settings();s.stock=stock;s.cyan=cyan;s.magenta=magenta;s.yellow=yellow;s.exposure=exposure;s.grain=grain;s.fade=fade;s.strength=strength;s.skin=skin;s.highlights=highlights;s.red=red;s.green=green;s.blue=blue;s.contrast=contrast;s.calibrationEv=calibrationEv;s.adaptiveGrain=adaptiveGrain;s.iso=iso;s.grainEdge=grainEdge;return s; }
    }
    // Same per-channel tone curve is used in the live GPU preview and JPEG processing.
    public static float[] parameters(Settings s){
        float[] p=FilmCatalog.parameters(s.stock,s.cyan,s.magenta,s.yellow,s.exposure,s.grain,s.fade);ImageQuality.adjust(p,s.red,s.green,s.blue,s.contrast,s.calibrationEv,s.highlights,s.adaptiveGrain,s.iso);if(s.adaptiveGrain)p[9]*=ImageQuality.sizeScale(s.grainEdge);return p;
    }
    public static Bitmap develop(Bitmap input,Settings s){
        int w=input.getWidth(),h=input.getHeight(); Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        int[] row=new int[w];float[] p=parameters(s); float[][] lut=new float[3][256];
        for(int c=0;c<3;c++)for(int i=0;i<256;i++)lut[c][i]=FilmCatalog.channel(i/255f,c,p)*255;
        float strength=FilmStrength.clamp(s.strength),baseGain=FilmStrength.exposure(s.exposure);
        int seed=2397;boolean tilt=p[21]>0&&strength>0,finish=p[20]>0||p[23]>0;int rx=FilmEffects.radius(w),ry=FilmEffects.radius(h);int[] top=tilt?new int[w]:null,mid=tilt?new int[w]:null,bottom=tilt?new int[w]:null;
        for(int y=0;y<h;y++){
            input.getPixels(row,0,w,0,y,w,1);float ny=((y+.5f)/h-.5f)*2,fy=(y+.5f)/h,soft=tilt?FilmEffects.tilt(fy)*p[21]:0;
            if(soft>0){input.getPixels(top,0,w,0,Math.max(0,y-ry),w,1);System.arraycopy(row,0,mid,0,w);input.getPixels(bottom,0,w,0,Math.min(h-1,y+ry),w,1);}
            for(int x=0;x<w;x++){
                int original=row[x];if(strength==0){row[x]=FilmStrength.blend(original,original,0,baseGain);continue;}int v=original;if(soft>0)v=FilmEffects.blend(v,FilmEffects.blurred(top,mid,bottom,x,rx),soft);float r=lut[0][(v>>16)&255],g=lut[1][(v>>8)&255],b=lut[2][v&255];
                float l=r*.299f+g*.587f+b*.114f;
                float nx=((x+.5f)/w-.5f)*2;float vignette=1-p[22]*(nx*nx+ny*ny);
                r=(l+(r-l)*p[6])*p[17]*vignette;g=(l+(g-l)*p[6])*p[18]*vignette;b=(l+(b-l)*p[6])*p[19]*vignette;
                float sr=((original>>16)&255),sg=((original>>8)&255),sb=original&255;float protect=p[6]==0?0:ImageQuality.clamp(s.skin,0,1)*ImageQuality.skin(sr/255,sg/255,sb/255);r+=protect*(sr-r);g+=protect*(sg-g);b+=protect*(sb-b);
                seed=seed*1664525+1013904223;float noise=(((seed>>>16)&255)/255f-.5f)*p[9]*40*(s.adaptiveGrain?ImageQuality.grainShade((r*.299f+g*.587f+b*.114f)/255):1);
                r=Math.max(0,Math.min(255,r+noise));g=Math.max(0,Math.min(255,g+noise));b=Math.max(0,Math.min(255,b+noise));
                if(finish){float fx=(x+.5f)/w;r=FilmEffects.finish(r/255f,0,fx,fy,p[20],p[23])*255;g=FilmEffects.finish(g/255f,1,fx,fy,p[20],p[23])*255;b=FilmEffects.finish(b/255f,2,fx,fy,p[20],p[23])*255;}
                row[x]=FilmStrength.blend(original,0xff000000|(clip(r)<<16)|(clip(g)<<8)|clip(b),strength,baseGain);
            }out.setPixels(row,0,w,0,y,w,1);
        }return out;
    }
    private static int clip(float n){return Math.max(0,Math.min(255,Math.round(n)));}
}
