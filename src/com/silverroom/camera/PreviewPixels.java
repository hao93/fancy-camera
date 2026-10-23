package com.silverroom.camera;

import java.nio.ByteBuffer;

/** GL RGBA bottom-up rows to Android ARGB top-down pixels, independent of byte order. */
public final class PreviewPixels {
    private PreviewPixels(){}
    public static final int DEFAULT_EDGE=320,MAX_EDGE=512;
    public static int[] size(int width,int height){
        return size(width,height,DEFAULT_EDGE);
    }
    public static int[] size(int width,int height,int edge){
        if(width<=0||height<=0)throw new IllegalArgumentException("Empty preview");
        if(edge<1||edge>MAX_EDGE)throw new IllegalArgumentException("Invalid preview edge");float scale=Math.min(1,edge/(float)Math.max(width,height));return new int[]{Math.max(1,Math.round(width*scale)),Math.max(1,Math.round(height*scale))};
    }
    public static int[] argb(ByteBuffer rgba,int width,int height){
        if(width<=0||height<=0||width>MAX_EDGE||height>MAX_EDGE||rgba.remaining()<width*height*4)throw new IllegalArgumentException("Invalid preview pixels");
        ByteBuffer b=rgba.duplicate();int start=b.position();int[] out=new int[width*height];
        for(int y=0;y<height;y++){b.position(start+(height-1-y)*width*4);for(int x=0;x<width;x++){int r=b.get()&255,g=b.get()&255,blue=b.get()&255;b.get();out[y*width+x]=0xff000000|(r<<16)|(g<<8)|blue;}}
        return out;
    }
}
