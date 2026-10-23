package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;

/** Shared drawing-only rotation. Never rotates Views, touch coordinates or Chinese labels. */
final class ControlRotation {
    interface Change {void angle(float value);}
    private final Change change;private ValueAnimator animator;private float angle;private int device;
    ControlRotation(Change callback){change=callback;}
    static float destination(float from,int device){
        // OrientationEventListener: left edge at the top is 90 degrees; counter-rotate content.
        float target=(360-device)%360;float delta=((target-from)%360+540)%360-180;
        return from+delta;
    }
    void turn(int degrees,boolean animate){
        if(degrees<0||degrees%90!=0)return;degrees%=360;
        if(device==degrees){if(!animate)finish();return;}
        if(animator!=null){animator.cancel();animator=null;}device=degrees;
        float target=destination(angle,device);
        if(!animate){angle=target;change.angle(angle);return;}
        animator=ValueAnimator.ofFloat(angle,target);animator.setDuration(200);animator.setInterpolator(new DecelerateInterpolator(2));
        animator.addUpdateListener(a->{angle=(float)a.getAnimatedValue();change.angle(angle);});animator.start();
    }
    void finish(){if(animator!=null){animator.cancel();animator=null;}angle=destination(angle,device);angle=(angle%360+360)%360;change.angle(angle);}
}
