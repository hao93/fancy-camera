package com.silverroom.camera;
import android.view.*;

/** A handle drag owns AE; the surrounding finder retains tap, hold and EV gestures. */
final class FinderTouch implements View.OnTouchListener {
    private final MainActivity a;private FinderGesture state;private int pointer=-1;
    private boolean aeCandidate,aeDragging,existingPoint;private float downX,downY,startAeX,startAeY;private int slop;
    private final Runnable hold=this::held;
    FinderTouch(MainActivity owner){a=owner;}
    private void held(){if(state!=null&&!a.busy&&!aeDragging&&state.hold()){a.guides.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);if(existingPoint)a.camera.lockCurrentMetering();else{a.guides.focus(state.x,state.y);a.camera.lockMetering(state.x/a.guides.getWidth(),state.y/a.guides.getHeight());}}}
    private void end(boolean released){a.guides.removeCallbacks(hold);if(aeDragging){if(released)a.camera.finishMeteringMove();else a.camera.cancelMeteringMove();}pointer=-1;aeDragging=aeCandidate=false;if(state!=null)state.cancel();if(released)a.guides.exposure(a.cameraEv,false);else a.guides.hideExposure();}
    void cancel(){end(false);}
    @Override public boolean onTouch(View v,MotionEvent e){
        int action=e.getActionMasked();if(a.busy){cancel();return true;}
        if(action==MotionEvent.ACTION_DOWN){cancel();a.evButton.finish();slop=ViewConfiguration.get(a).getScaledTouchSlop();downX=e.getX();downY=e.getY();state=new FinderGesture(slop,a.getResources().getDisplayMetrics().density,a.camera.exposureLimits());state.begin(downX,downY,a.cameraEv,a.camera.deviceOrientation());aeCandidate=a.guides.hitAE(downX,downY);existingPoint=aeCandidate||a.guides.hitAF(downX,downY);MeteringState.Snapshot p=a.camera.points();startAeX=p.aeX;startAeY=p.aeY;pointer=e.getPointerId(0);v.postDelayed(hold,ViewConfiguration.getLongPressTimeout());return true;}
        if(action==MotionEvent.ACTION_CANCEL||e.getPointerCount()>1||action==MotionEvent.ACTION_POINTER_UP){cancel();return true;}
        if(pointer<0||state==null)return true;int index=e.findPointerIndex(pointer);if(index<0){cancel();return true;}
        if(action==MotionEvent.ACTION_MOVE){
            if(aeCandidate){float dx=e.getX(index)-downX,dy=e.getY(index)-downY;if((aeDragging||Math.hypot(dx,dy)>slop)){aeDragging=true;a.guides.selectAE();v.removeCallbacks(hold);a.camera.meterAt(startAeX+dx/Math.max(1,a.guides.getWidth()),startAeY+dy/Math.max(1,a.guides.getHeight()));}return true;}
            boolean was=state.dragging;if(state.move(e.getX(index),e.getY(index))){v.removeCallbacks(hold);if(!was){a.guides.focus(state.x,state.y);a.camera.focus(state.x/a.guides.getWidth(),state.y/a.guides.getHeight());}a.applyExposure(state.value);a.guides.exposure(a.cameraEv,true);}else if(state.moved)v.removeCallbacks(hold);return true;
        }
        if(action==MotionEvent.ACTION_UP){if(Math.hypot(e.getX(index)-downX,e.getY(index)-downY)>slop)state.moved=true;if(aeCandidate&&state.tap())a.guides.selectAE();if(!aeCandidate&&state.tap()){a.resetExposureForFocus();a.camera.focusAndMeter(state.x/a.guides.getWidth(),state.y/a.guides.getHeight());a.guides.focus(state.x,state.y);v.performClick();}end(true);}return true;
    }
}
