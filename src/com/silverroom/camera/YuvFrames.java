package com.silverroom.camera;

import java.nio.ByteBuffer;

/** Converts camera planes, including padded rows and shared interleaved UV planes. */
public final class YuvFrames {
    private YuvFrames() {}
    public static byte[] nv21(int width,int height,int left,int top,ByteBuffer y,int ys,int yp,
                              ByteBuffer u,int us,int up,ByteBuffer v,int vs,int vp){
        if(width<=0||height<=0||((width|height|left|top)&1)!=0)throw new IllegalArgumentException("YUV dimensions must be even");
        byte[] output=new byte[width*height*3/2];int at=0;
        int y0=y.position()+top*ys+left*yp,u0=u.position()+top/2*us+left/2*up,v0=v.position()+top/2*vs+left/2*vp;
        if(yp==1){ByteBuffer rows=y.duplicate();for(int row=0;row<height;row++){rows.position(y0+row*ys);rows.get(output,at,width);at+=width;}}
        else for(int row=0;row<height;row++)for(int col=0;col<width;col++)output[at++]=y.get(y0+row*ys+col*yp);
        for(int row=0;row<height/2;row++)for(int col=0;col<width/2;col++){
            output[at++]=v.get(v0+row*vs+col*vp);output[at++]=u.get(u0+row*us+col*up);
        }return output;
    }
}
