package com.silverroom.camera;

/** Zoom relative to aspect-fit. Pan is constrained to keep the image covering its axes. */
public final class PhotoViewport {
    public float zoom=1,x,y,fit=1;private float vw,vh,iw,ih;
    public void size(float w,float h,float bw,float bh){vw=w;vh=h;iw=bw;ih=bh;fit=w>0&&h>0&&bw>0&&bh>0?Math.min(w/bw,h/bh):1;clamp();}
    public void reset(){zoom=1;x=y=0;}
    public void scale(float value,float focusX,float focusY){if(!Float.isFinite(value)||value<=0)return;float old=zoom;zoom=Math.max(1,Math.min(6,value));float px=focusX-vw/2,py=focusY-vh/2;x=px-(px-x)*zoom/old;y=py-(py-y)*zoom/old;clamp();}
    public void pan(float dx,float dy){if(!Float.isFinite(dx)||!Float.isFinite(dy))return;x+=dx;y+=dy;clamp();}
    private void clamp(){float mx=Math.max(0,(iw*fit*zoom-vw)/2),my=Math.max(0,(ih*fit*zoom-vh)/2);x=Math.max(-mx,Math.min(mx,x));y=Math.max(-my,Math.min(my,y));if(zoom==1)x=y=0;}
}
