package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import static com.silverroom.camera.Controls.*;

/** Horizontal film reel with live detents, inertial settling and a fixed selection gate. */
class FilmWheel extends View {
    interface Listener {void selected(int id);}
    final FilmWheelState state=new FilmWheelState();private final Paint paint=new Paint(3);
    private ValueAnimator animator;private VelocityTracker velocity;private int pointer=-1,notified=1;
    private float lastX,downX,downY;private boolean dragged;Listener listener;
    FilmWheel(Context c){super(c);setFocusable(true);setClickable(true);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private float sp(float n){return n*getResources().getDisplayMetrics().scaledDensity;}
    protected float pitch(){return Math.max(dp(94),Math.min(dp(116),getWidth()*.42f));}
    String title(int id){return FilmCatalog.reelName(id);}
    String subtitle(int id){return FilmCatalog.TAGS[id];}
    int color(int id){return FilmCatalog.COLORS[id];}
    boolean available(int id){return true;}
    String category(){return "胶片";}
    boolean compact(){return false;}
    int legalTarget(int target){return Math.max(0,Math.min(state.count()-1,target));}
    void tapped(int target){cancelAnimation();update(legalTarget(target));}
    boolean moving(){return pointer>=0||(animator!=null&&animator.isRunning());}
    void films(List<Integer> ids,int selected){cancelAnimation();releaseTouch();state.reset(ids,selected);notified=state.selected();describe();invalidate();}
    private void describe(){setContentDescription(category()+"滚轮，当前"+(compact()?title(state.selected()):FilmCatalog.NAMES[state.selected()])+"，"+subtitle(state.selected())+"，左右滑动或点击切换");}
    private void update(float position){if(position<0)return;state.position(position);int selected=state.selected();if(selected!=notified&&available(selected)){notified=selected;describe();performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);if(listener!=null)listener.selected(selected);sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);}invalidate();}
    private void cancelAnimation(){if(animator!=null){animator.cancel();animator=null;}}
    private void settle(int target){target=legalTarget(target);cancelAnimation();if(target<0)return;float start=state.position();if(Math.abs(target-start)<.001f){update(target);return;}animator=ValueAnimator.ofFloat(start,target);animator.setDuration(Math.min(420,180+Math.round(Math.abs(target-start)*70)));animator.setInterpolator(new DecelerateInterpolator(2));animator.addUpdateListener(a->update((float)a.getAnimatedValue()));animator.start();}
    void finish(){cancelAnimation();releaseTouch();update(legalTarget(state.index()));}
    private void releaseTouch(){pointer=-1;if(velocity!=null){velocity.recycle();velocity=null;}if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    @Override public void setEnabled(boolean enabled){if(!enabled&&state!=null)finish();super.setEnabled(enabled);invalidate();}
    @Override protected void onDetachedFromWindow(){cancelAnimation();releaseTouch();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){
        float cx=getWidth()/2f,cy=getHeight()/2f,slot=pitch();
        float half=dp(compact()?22:25);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff25271f);c.drawRoundRect(cx-slot*.49f,cy-half,cx+slot*.49f,cy+half,dp(13),dp(13),paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(.8f));paint.setColor(0xff86704a);c.drawRoundRect(cx-slot*.49f,cy-half,cx+slot*.49f,cy+half,dp(13),dp(13),paint);paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<state.count();i++){
            float distance=i-state.position(),x=cx+distance*slot;if(x< -slot||x>getWidth()+slot)continue;
            float depth=Math.min(1.8f,Math.abs(distance)),scale=1-depth*.19f;int id=state.id(i);int alpha=Math.round((1-Math.min(.82f,depth*.43f))*(isEnabled()&&available(id)?255:75));boolean selected=i==state.index()&&available(id);
            c.save();c.translate(x,cy+dp(depth*2));c.scale(scale,scale);c.clipRect(-slot*.48f,-dp(28),slot*.48f,dp(28));
            if(!compact()){paint.setColor(color(id));paint.setAlpha(alpha);c.drawRoundRect(-dp(11),-dp(19),dp(11),-dp(16),dp(1.5f),dp(1.5f),paint);}
            paint.setTypeface(Typeface.create("sans-serif-medium",0));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(sp(compact()?15:13));paint.setColor(selected?AMBER:CREAM);paint.setAlpha(alpha);c.drawText(title(id),0,dp(compact()?-1:1),paint);
            paint.setTypeface(Typeface.create("sans-serif",0));paint.setTextSize(sp(compact()?9:7));paint.setColor(MUTED);paint.setAlpha(alpha);c.drawText(subtitle(id),0,dp(compact()?14:16),paint);c.restore();
        }
        paint.setAlpha(255);paint.setShader(new LinearGradient(0,0,dp(22),0,INK,0x00121514,Shader.TileMode.CLAMP));c.drawRect(0,0,dp(22),getHeight(),paint);
        paint.setShader(new LinearGradient(getWidth()-dp(22),0,getWidth(),0,0x00121514,INK,Shader.TileMode.CLAMP));c.drawRect(getWidth()-dp(22),0,getWidth(),getHeight(),paint);paint.setShader(null);
        if(!compact()){paint.setColor(AMBER);Path marker=new Path();marker.moveTo(cx,cy+dp(28));marker.lineTo(cx-dp(2.5f),cy+dp(32));marker.lineTo(cx+dp(2.5f),cy+dp(32));marker.close();c.drawPath(marker,paint);}
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return false;int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){cancelAnimation();releaseTouch();pointer=e.getPointerId(0);lastX=downX=e.getX();downY=e.getY();dragged=false;velocity=VelocityTracker.obtain();velocity.addMovement(e);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;}
        if(pointer<0)return true;if(velocity!=null)velocity.addMovement(e);
        if(action==MotionEvent.ACTION_MOVE){int index=e.findPointerIndex(pointer);if(index<0)return true;float x=e.getX(index);if(!dragged&&Math.hypot(x-downX,e.getY(index)-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())dragged=true;if(dragged)update(state.position()+(lastX-x)/pitch());lastX=x;return true;}
        if(action==MotionEvent.ACTION_UP){int target=state.index();if(!dragged){target=Math.round(state.position()+(e.getX()-getWidth()/2f)/pitch());performClick();releaseTouch();tapped(Math.max(0,Math.min(state.count()-1,target)));}else{if(velocity!=null){velocity.computeCurrentVelocity(1000);target=state.target(-velocity.getXVelocity(pointer)/pitch());}releaseTouch();settle(target);}return true;}
        if(action==MotionEvent.ACTION_CANCEL||(action==MotionEvent.ACTION_POINTER_UP&&e.getPointerId(e.getActionIndex())==pointer)){releaseTouch();settle(state.index());return true;}return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int key,KeyEvent e){if(isEnabled()&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){settle(Math.max(0,Math.min(state.count()-1,state.index()+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1))));return true;}return super.onKeyDown(key,e);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(0,0,state.count()-1,state.index()));info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(isEnabled()&&(action==4096||action==8192)){settle(Math.max(0,Math.min(state.count()-1,state.index()+(action==4096?1:-1))));return true;}return super.performAccessibilityAction(action,args);}
}
