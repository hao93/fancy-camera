package com.silverroom.camera;
import android.view.*;

/** Routes tap/hold/drag without allowing a completed hold to fire a second tap. */
final class FinderTouch implements View.OnTouchListener {
    private final MainActivity a;private FinderGesture state;private int pointer=-1;
    private final Runnable hold=this::held;
    private void held(){if(state!=null&&!a.busy&&state.hold()){a.guides.focus(state.x,state.y);a.guides.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);a.camera.lockMetering(state.x/a.guides.getWidth(),state.y/a.guides.getHeight());}}
    FinderTouch(MainActivity owner){a=owner;}
    void cancel(){a.guides.removeCallbacks(hold);pointer=-1;if(state!=null)state.cancel();a.guides.exposure(a.cameraEv,false);}
    @Override public boolean onTouch(View v,MotionEvent e){int action=e.getActionMasked();if(a.busy){cancel();return true;}if(action==MotionEvent.ACTION_DOWN){cancel();state=new FinderGesture(ViewConfiguration.get(a).getScaledTouchSlop(),a.getResources().getDisplayMetrics().density,a.camera.exposureLimits());state.begin(e.getX(),e.getY(),a.cameraEv,a.camera.deviceOrientation());pointer=e.getPointerId(0);v.postDelayed(hold,ViewConfiguration.getLongPressTimeout());return true;}if(action==MotionEvent.ACTION_CANCEL||e.getPointerCount()>1||action==MotionEvent.ACTION_POINTER_UP){cancel();return true;}if(pointer<0||state==null)return true;int index=e.findPointerIndex(pointer);if(index<0){cancel();return true;}if(action==MotionEvent.ACTION_MOVE){boolean was=state.dragging;if(state.move(e.getX(index),e.getY(index))){v.removeCallbacks(hold);if(!was){a.guides.focus(state.x,state.y);a.camera.focus(state.x/a.guides.getWidth(),state.y/a.guides.getHeight());}a.applyExposure(state.value);a.guides.exposure(a.cameraEv,true);}else if(state.moved)v.removeCallbacks(hold);return true;}if(action==MotionEvent.ACTION_UP){if(state.tap()){a.camera.focus(state.x/a.guides.getWidth(),state.y/a.guides.getHeight());a.guides.focus(state.x,state.y);v.performClick();}cancel();}return true;}
}
