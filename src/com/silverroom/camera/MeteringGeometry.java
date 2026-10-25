package com.silverroom.camera;

/** Region coordinates follow the visible viewport; zoomRatio is already in the logical FOV. */
public final class MeteringGeometry {
    private MeteringGeometry(){}
    private static float[] sensor(float x,float y,int angle,int bw,int bh,int vw,int vh,float crop,boolean front){
        float[] p=PreviewFraming.point(x,y,angle,bw,bh,vw,vh,crop);x=front?1-p[0]:p[0];y=p[1];
        return angle==90?new float[]{y,1-x}:angle==270?new float[]{1-y,x}:angle==180?new float[]{1-x,1-y}:new float[]{x,y};
    }
    public static int[] region(int mode,float x,float y,int angle,int bw,int bh,int vw,int vh,float crop,boolean front,int left,int top,int width,int height){
        if(width<=0||height<=0)throw new IllegalArgumentException("Empty metering sensor");
        float hx=mode==MeteringState.CENTER?.25f:Math.min(vw,vh)*.05f/Math.max(1,vw),hy=mode==MeteringState.CENTER?.25f:Math.min(vw,vh)*.05f/Math.max(1,vh);
        if(mode==MeteringState.CENTER)x=y=.5f;
        float[] a=sensor(Math.max(0,x-hx),Math.max(0,y-hy),angle,bw,bh,vw,vh,crop,front),b=sensor(Math.min(1,x+hx),Math.min(1,y+hy),angle,bw,bh,vw,vh,crop,front);
        int l=left+Math.max(0,Math.min(width-1,(int)Math.floor(Math.min(a[0],b[0])*width))),t=top+Math.max(0,Math.min(height-1,(int)Math.floor(Math.min(a[1],b[1])*height)));
        int r=left+Math.max(l-left+1,Math.min(width,(int)Math.ceil(Math.max(a[0],b[0])*width))),d=top+Math.max(t-top+1,Math.min(height,(int)Math.ceil(Math.max(a[1],b[1])*height)));
        return new int[]{l,t,r-l,d-t};
    }
    private static float[] marker(float x,float y,float width,float height,float density){
        // Shared edge inset keeps the animated AF brackets and AE ring together and touchable.
        float margin=Math.min(36*density,Math.min(width,height)/2);
        return new float[]{Math.max(margin,Math.min(width-margin,x*width)),Math.max(margin,Math.min(height-margin,y*height))};
    }
    public static float[] focusCenter(MeteringState.Snapshot p,float width,float height,float density){return marker(p.afX,p.afY,width,height,density);}
    public static float[] handle(MeteringState.Snapshot p,float width,float height,float density){
        return p.hasFocus&&!p.separated()?focusCenter(p,width,height,density):marker(p.aeX,p.aeY,width,height,density);
    }
    public static boolean hit(MeteringState.Snapshot p,boolean available,float x,float y,float width,float height,float density){
        if(p==null||!p.hasFocus||p.mode!=MeteringState.SPOT||!available||width<=0||height<=0)return false;float[] h=handle(p,width,height,density);return Math.abs(x-h[0])<=24*density&&Math.abs(y-h[1])<=24*density;
    }
}
