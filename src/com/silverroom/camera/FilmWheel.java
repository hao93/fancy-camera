package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import static com.silverroom.camera.Controls.*;

/** Horizontal film reel with live detents, inertial settling and a integrated selection outline. */
class FilmWheel extends View {
    interface Listener {void selected(int id);}
    final FilmWheelState state=new FilmWheelState();private final Paint paint=new Paint(3);
    private ValueAnimator animator;private VelocityTracker velocity;private int pointer=-1,notified=1;
    private float lastX,downX,downY,visual=Float.NaN,strength=1;private boolean dragged,repeatEligible;private int downIndex;private long lastHaptic;Listener listener;Runnable reselected;
    static final int ACTION_ADJUST_STRENGTH=0x7f0a0001;
    private final Runnable held=()->{if(pointer>=0&&!dragged&&isLongClickable()){performLongClick();releaseTouch();}};
    FilmWheel(Context c){super(c);setFocusable(true);setClickable(true);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private float sp(float n){return n*getResources().getDisplayMetrics().scaledDensity;}
    protected float pitch(){return Math.max(dp(80),Math.min(dp(88),getWidth()/3.5f));}
    void strength(float value){strength=FilmStrength.clamp(value);describe();invalidate();}
    String title(int id){return FilmCatalog.reelName(id);}
    void drawTitle(Canvas canvas,String text,float baseline,Paint brush,float slot){canvas.drawText(fit(text,slot-dp(12)),0,baseline,brush);}
    String subtitle(int id){return FilmCatalog.TAGS[id];}
    int color(int id){return FilmCatalog.COLORS[id];}
    boolean available(int id){return true;}
    String category(){return "胶片";}
    boolean compact(){return false;}
    int legalTarget(int target){return Math.max(0,Math.min(state.count()-1,target));}
    void tapped(int target){float start=Float.isFinite(visual)?visual:state.position();cancelAnimation();int end=legalTarget(target);if(end<0)return;update(end);if(Math.abs(start-end)<.01f)return;visual=start;animator=ValueAnimator.ofFloat(start,end);animator.setDuration(180);animator.setInterpolator(new DecelerateInterpolator(2));animator.addUpdateListener(a->{visual=(float)a.getAnimatedValue();invalidate();});animator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){visual=Float.NaN;invalidate();}});animator.start();}
    boolean moving(){return pointer>=0||(animator!=null&&animator.isRunning());}
    void films(List<Integer> ids,int selected){cancelAnimation();releaseTouch();state.reset(ids,selected);notified=state.selected();describe();invalidate();}
    private void describe(){setContentDescription(category()+"滚轮，当前"+(compact()?title(state.selected()):FilmCatalog.NAMES[state.selected()])+"，"+subtitle(state.selected())+(compact()?"":"，强度 "+FilmStrength.percent(strength)+"%，再次点击调整强度")+"，左右滑动或点击切换");}
    private void update(float position){if(position<0)return;state.position(position);int selected=state.selected();if(selected!=notified&&available(selected)){notified=selected;describe();long now=android.os.SystemClock.uptimeMillis();if(now-lastHaptic>=35){lastHaptic=now;performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);}if(listener!=null)listener.selected(selected);sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);}invalidate();}
    private void cancelAnimation(){if(animator!=null){animator.cancel();animator=null;}visual=Float.NaN;}
    private void settle(int target){target=legalTarget(target);cancelAnimation();if(target<0)return;float start=state.position();if(Math.abs(target-start)<.001f){update(target);return;}animator=ValueAnimator.ofFloat(start,target);animator.setDuration(Math.min(420,180+Math.round(Math.abs(target-start)*70)));animator.setInterpolator(new DecelerateInterpolator(2));animator.addUpdateListener(a->update((float)a.getAnimatedValue()));animator.start();}
    void finish(){cancelAnimation();releaseTouch();update(legalTarget(state.index()));}
    private void releaseTouch(){removeCallbacks(held);pointer=-1;if(velocity!=null){velocity.recycle();velocity=null;}if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    @Override public void setEnabled(boolean enabled){if(!enabled&&state!=null)finish();super.setEnabled(enabled);invalidate();}
    @Override protected void onDetachedFromWindow(){cancelAnimation();releaseTouch();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){
        float cx=getWidth()/2f,cy=getHeight()/2f,slot=pitch(),font=Math.max(1,getResources().getDisplayMetrics().scaledDensity/getResources().getDisplayMetrics().density);
        float half=getHeight()/2f-dp(7);paint.setStyle(Paint.Style.FILL);paint.setColor(tone(AppTheme.ACTIVE));if(compact())c.drawRoundRect(cx-slot*.49f,cy-half,cx+slot*.49f,cy+half,dp(13),dp(13),paint);
        if(compact()){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(.8f));paint.setColor(tone(AppTheme.SELECT_BORDER));c.drawRoundRect(cx-slot*.49f,cy-half,cx+slot*.49f,cy+half,dp(13),dp(13),paint);}paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<state.count();i++){
            float distance=i-(Float.isFinite(visual)?visual:state.position()),x=cx+distance*slot;if(x< -slot||x>getWidth()+slot)continue;
            float depth=Math.min(1.8f,Math.abs(distance)),scale=1-depth*.19f;int id=state.id(i);int alpha=Math.round((1-Math.min(.82f,depth*.43f))*(isEnabled()&&available(id)?255:75));boolean selected=i==state.index()&&available(id);
            c.save();c.translate(x,cy+dp(depth*2));c.scale(scale,scale);c.clipRect(-slot*.48f,-getHeight()/2f+dp(4),slot*.48f,getHeight()/2f-dp(4));
            if(!compact()){c.save();Path mask=new Path();mask.addRoundRect(-slot*.44f,-half,slot*.44f,half,dp(9),dp(9),Path.Direction.CW);c.clipPath(mask);FilmLabelStyle label=FilmLabelStyle.of(id);paint.setColor(label.paper);paint.setAlpha(alpha);c.drawRoundRect(-slot*.44f,-half,slot*.44f,half,dp(9),dp(9),paint);paint.setColor(label.band);c.drawRect(-slot*.44f,dp(7)*font,slot*.44f,half,paint);paint.setColor(label.accent);c.drawRoundRect(-dp(11),-dp(19)*font,dp(11),-dp(16)*font,dp(1.5f),dp(1.5f),paint);}
            paint.setTypeface(Typeface.create("sans-serif-medium",0));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(sp(compact()?18:14));paint.setColor(compact()?(selected?AMBER:CREAM):FilmLabelStyle.of(id).ink);paint.setAlpha(alpha);drawTitle(c,title(id),dp(compact()?-1:1)*font,paint,slot);
            paint.setTypeface(Typeface.create("sans-serif",0));paint.setFontFeatureSettings("tnum");paint.setTextSize(sp(12));paint.setColor(compact()?MUTED:FilmLabelStyle.of(id).bandInk);paint.setAlpha(alpha);c.drawText(fit(selected&&!compact()?FilmStrength.percent(strength)+"%":subtitle(id),slot-dp(12)),0,dp(compact()?14:18)*font,paint);if(!compact()){c.restore();if(selected){float stroke=dp(1.4f),inset=stroke/2;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(AMBER);paint.setAlpha(alpha);c.drawRoundRect(-slot*.44f+inset,-half+inset,slot*.44f-inset,half-inset,dp(9)-inset,dp(9)-inset,paint);paint.setStyle(Paint.Style.FILL);}}c.restore();
        }
        drawEdges(c);
        if(!compact()){paint.setColor(AMBER);Path marker=new Path();marker.moveTo(cx,getHeight()-dp(4));marker.lineTo(cx-dp(2.5f),getHeight());marker.lineTo(cx+dp(2.5f),getHeight());marker.close();c.drawPath(marker,paint);}
    }
    /** Shared edge veil for both reels; keep the centered selection clear in either theme. */
    protected void drawEdges(Canvas canvas){
        if(getWidth()<=0||getHeight()<=0)return;float edge=Math.min(dp(22),getWidth()/4f);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
        paint.setShader(new LinearGradient(0,0,edge,0,INK,INK&0x00ffffff,Shader.TileMode.CLAMP));canvas.drawRect(0,0,edge,getHeight(),paint);
        paint.setShader(new LinearGradient(getWidth()-edge,0,getWidth(),0,INK&0x00ffffff,INK,Shader.TileMode.CLAMP));canvas.drawRect(getWidth()-edge,0,getWidth(),getHeight(),paint);paint.setShader(null);
    }
    private String fit(String text,float width){if(paint.measureText(text)<=width)return text;int end=text.length();while(end>0&&paint.measureText(text.substring(0,end)+"…")>width)end--;return text.substring(0,end)+"…";}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return false;int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){boolean settled=!moving()&&Math.abs(state.position()-state.index())<.001f;cancelAnimation();releaseTouch();repeatEligible=settled;downIndex=state.index();pointer=e.getPointerId(0);lastX=downX=e.getX();downY=e.getY();dragged=false;velocity=VelocityTracker.obtain();velocity.addMovement(e);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);if(isLongClickable())postDelayed(held,ViewConfiguration.getLongPressTimeout());return true;}
        if(pointer<0)return true;if(velocity!=null)velocity.addMovement(e);
        if(action==MotionEvent.ACTION_MOVE){int index=e.findPointerIndex(pointer);if(index<0)return true;float x=e.getX(index);if(!dragged&&Math.hypot(x-downX,e.getY(index)-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())dragged=true;if(dragged)update(state.position()+(lastX-x)/pitch());lastX=x;return true;}
        if(action==MotionEvent.ACTION_UP){int target=state.index();if(!dragged){target=Math.round(state.position()+(e.getX()-getWidth()/2f)/pitch());boolean repeat=repeatEligible&&downIndex==state.index()&&target==downIndex&&available(state.id(downIndex))&&!compact()&&reselected!=null;super.performClick();releaseTouch();if(repeat){finish();reselected.run();}else tapped(Math.max(0,Math.min(state.count()-1,target)));}else{if(velocity!=null){velocity.computeCurrentVelocity(1000);target=state.target(-velocity.getXVelocity(pointer)/pitch());}releaseTouch();settle(target);}return true;}
        if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||(action==MotionEvent.ACTION_POINTER_UP&&e.getPointerId(e.getActionIndex())==pointer)){releaseTouch();settle(state.index());return true;}return true;
    }
    @Override public boolean performClick(){super.performClick();if(isEnabled()&&!compact()&&reselected!=null){finish();reselected.run();}return true;}
    @Override public boolean onKeyDown(int key,KeyEvent e){if(isEnabled()&&!compact()&&reselected!=null&&(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER))return performClick();if(isEnabled()&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){settle(Math.max(0,Math.min(state.count()-1,state.index()+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1))));return true;}return super.onKeyDown(key,e);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(0,0,state.count()-1,state.index()));info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);if(!compact()&&reselected!=null)info.addAction(new AccessibilityNodeInfo.AccessibilityAction(ACTION_ADJUST_STRENGTH,"调整当前胶片强度"));}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(isEnabled()&&action==ACTION_ADJUST_STRENGTH&&!compact()&&reselected!=null)return performClick();if(isEnabled()&&(action==4096||action==8192)){settle(Math.max(0,Math.min(state.count()-1,state.index()+(action==4096?1:-1))));return true;}return super.performAccessibilityAction(action,args);}
}
