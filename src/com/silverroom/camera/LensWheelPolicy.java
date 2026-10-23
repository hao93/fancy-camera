package com.silverroom.camera;

/** A fling can pass a blocked lens, but must settle on an available optical route. */
public final class LensWheelPolicy {
    private LensWheelPolicy(){}
    public static int nearest(float position,boolean[] available){
        int best=-1;float distance=Float.MAX_VALUE;
        for(int i=0;i<available.length;i++)if(available[i]){float next=Math.abs(i-position);if(next<distance){distance=next;best=i;}}
        return best;
    }
}
