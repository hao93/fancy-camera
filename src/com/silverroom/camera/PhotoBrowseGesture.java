package com.silverroom.camera;

/** Lock an axis after deliberate movement; pinching or starting zoomed blocks page actions. */
public final class PhotoBrowseGesture {
    public enum Action {NONE,PREVIOUS,NEXT,CLOSE,DELETE}
    private float startX,startY;private boolean blocked=true;private int axis;
    public void begin(float x,float y,float zoom){startX=x;startY=y;blocked=!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(zoom)||zoom<1||zoom>1.01f;axis=0;}
    public void block(){blocked=true;axis=0;}
    public float[] drag(float x,float y,float slop){
        if(blocked||!Float.isFinite(x)||!Float.isFinite(y))return new float[]{0,0};float dx=x-startX,dy=y-startY;
        if(axis==0&&Math.max(Math.abs(dx),Math.abs(dy))>=slop){if(Math.abs(dx)>Math.abs(dy)*1.25f)axis=1;else if(Math.abs(dy)>Math.abs(dx)*1.25f)axis=2;}
        return axis==1?new float[]{dx,0}:axis==2?new float[]{0,dy}:new float[]{0,0};
    }
    public Action finish(float x,float y,float horizontal,float vertical,float slop){
        float[] distance=drag(x,y,slop);Action result=Action.NONE;
        float dx=Math.abs(x-startX),dy=Math.abs(y-startY);
        if(horizontal>0&&dx>=dy*1.1f&&Math.abs(distance[0])>=horizontal)result=distance[0]<0?Action.NEXT:Action.PREVIOUS;
        else if(vertical>0&&dy>=dx*1.1f&&Math.abs(distance[1])>=vertical)result=distance[1]>0?Action.CLOSE:Action.DELETE;
        block();return result;
    }
}
