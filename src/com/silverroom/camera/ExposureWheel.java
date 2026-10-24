package com.silverroom.camera;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import java.util.function.IntSupplier;
import static com.silverroom.camera.Controls.*;

/** Controlled vertical exposure reel. Only user changes emit; camera echoes retain fractional travel. */
class ExposureWheel extends View {
    interface Changed {void value(float ev);}
    Changed changed;IntSupplier orientation=()->0;
    float value;private float low=-3,high=3;private float position,downX,downY,lastAxis;private int pointer=-1,device,epoch;
    private boolean dragged,tapEligible;private VelocityTracker velocity;private ValueAnimator motion;private final Paint paint=new Paint(3);
    ExposureWheel(Context context){super(context);setClickable(true);setFocusable(true);describe();}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private float axis(float x,float y){return FinderGesture.projectAxis(x,y,device);}
    private void describe(){setContentDescription("曝光滚轮 "+ExposureValue.label(value)+"，上下滑动0.1细调，点击打开曝光面板");}
    void set(float next){next=ExposureValue.clamp(next);if(Math.abs(next-value)<.0001f){describe();invalidate();return;}finish();value=next;position=next*10;describe();invalidate();}
    void range(float min,float max){min=ExposureValue.clamp(min);max=ExposureValue.clamp(max);if(min>max){min=0;max=0;}if(low!=min||high!=max){finish();low=min;high=max;set(Math.max(low,Math.min(high,value)));}}
    private void move(float next){position=Math.max(low*10,Math.min(high*10,next));float selected=Math.round(position)/10f;if(Math.abs(value-selected)>.0001f){value=selected;describe();performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);if(changed!=null)changed.value(value);}invalidate();}
    private void stopMotion(){epoch++;if(motion!=null){motion.cancel();motion=null;}}
    private void release(){pointer=-1;if(velocity!=null){velocity.recycle();velocity=null;}if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    void finish(){stopMotion();release();dragged=false;tapEligible=false;position=value*10;invalidate();}
    @Override public void setEnabled(boolean enabled){if(!enabled)finish();super.setEnabled(enabled);invalidate();}
    @Override protected void onDetachedFromWindow(){finish();super.onDetachedFromWindow();}
    @Override public boolean performClick(){if(!isEnabled())return false;finish();return super.performClick();}
    private void settle(float target){
        stopMotion();target=Math.max(low*10,Math.min(high*10,Math.round(target)));if(!ValueAnimator.areAnimatorsEnabled()||Math.abs(target-position)<.001f){move(target);return;}
        int token=epoch;motion=ValueAnimator.ofFloat(position,target);motion.setDuration(200);motion.setInterpolator(new DecelerateInterpolator(2));motion.addUpdateListener(a->{if(token==epoch)move((float)a.getAnimatedValue());});motion.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(token==epoch){position=value*10;invalidate();}}});motion.start();
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return false;int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){tapEligible=motion==null||!motion.isRunning();stopMotion();release();pointer=e.getPointerId(0);device=orientation.getAsInt();if(device<0||device%90!=0)device=0;device%=360;downX=e.getX();downY=e.getY();lastAxis=axis(downX,downY);dragged=false;velocity=VelocityTracker.obtain();velocity.addMovement(e);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;}
        if(pointer<0)return true;if(velocity!=null)velocity.addMovement(e);
        if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_CANCEL){finish();return true;}
        if(action==MotionEvent.ACTION_MOVE){int i=e.findPointerIndex(pointer);if(i<0){finish();return true;}float x=e.getX(i),y=e.getY(i),current=axis(x,y),delta=current-axis(downX,downY),cross=device%180==0?Math.abs(x-downX):Math.abs(y-downY);int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if(!dragged){if(cross>slop&&cross>=Math.abs(delta)){finish();return true;}if(Math.abs(delta)>slop){dragged=true;tapEligible=false;move(position-(delta-Math.copySign(slop,delta))/dp(24));}}
            else move(position+(lastAxis-current)/dp(24));lastAxis=current;return true;}
        if(action==MotionEvent.ACTION_UP){boolean tap=!dragged&&tapEligible&&Math.hypot(e.getX()-downX,e.getY()-downY)<=ViewConfiguration.get(getContext()).getScaledTouchSlop();float travel=0;if(dragged&&velocity!=null){velocity.computeCurrentVelocity(1000);float speed=axis(velocity.getXVelocity(pointer),velocity.getYVelocity(pointer));if(Float.isFinite(speed))travel=Math.max(-6,Math.min(6,-speed/dp(24)*.12f));}boolean wasDragged=dragged;release();dragged=false;tapEligible=false;if(tap)performClick();else if(wasDragged)settle(position+travel);else finish();return true;}
        return true;
    }
    private boolean step(int direction){if(!isEnabled())return false;finish();float next=Math.max(low,Math.min(high,ExposureValue.clamp(value+direction*.1f)));if(next==value)return false;move(next*10);return true;}
    @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_DPAD_UP)return step(1);if(key==KeyEvent.KEYCODE_DPAD_DOWN)return step(-1);if(key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_DPAD_CENTER)return performClick();return super.onKeyDown(key,event);}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(action==4096)return step(1);if(action==8192)return step(-1);return super.performAccessibilityAction(action,args);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");info.setScrollable(true);info.setCanOpenPopup(true);info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(1,low,high,value));info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
    @Override protected void onDraw(Canvas canvas){
        float w=getWidth(),h=getHeight(),sp=getResources().getDisplayMetrics().scaledDensity,cx=w/2,cy=h/2,label=Math.max(dp(18),12*sp+dp(6)),bottom=h-label;if(w<=0||bottom<=0)return;
        paint.setShader(null);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setColor(tone(AppTheme.ACTIVE));canvas.drawRoundRect(dp(2),cy-dp(14),w-dp(2),cy+dp(14),dp(8),dp(8),paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(.7f));paint.setColor(isFocused()?AMBER:tone(AppTheme.SELECT_BORDER));canvas.drawRoundRect(dp(2),cy-dp(14),w-dp(2),cy+dp(14),dp(8),dp(8),paint);paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.create("monospace",0));paint.setFontFeatureSettings("tnum");paint.setTextAlign(Paint.Align.CENTER);
        canvas.save();canvas.clipRect(0,0,w,bottom);
        for(int tick=Math.max(Math.round(low*10),(int)Math.floor(position)-2);tick<=Math.min(Math.round(high*10),(int)Math.ceil(position)+2);tick++){
            float distance=tick-position,y=cy+distance*dp(24);if(y<=dp(2)||y>=bottom-dp(2))continue;boolean selected=tick==Math.round(position);String text=ExposureValue.label(tick/10f);float size=(selected?18:12)*sp;paint.setTextSize(size);float fit=Math.min(1,(w-dp(8))/Math.max(1,paint.measureText(text)));size=Math.min(size*fit,Math.min(y,bottom-y)*2-dp(2));if(size<dp(8))continue;paint.setTextSize(size);Paint.FontMetrics fm=paint.getFontMetrics();paint.setColor(selected?(value==0?CREAM:AMBER):MUTED);paint.setAlpha(isEnabled()?Math.round(255*(1-Math.min(.85f,Math.abs(distance)*.65f))):75);canvas.drawText(text,cx,y-(fm.ascent+fm.descent)/2,paint);
        }
        paint.setAlpha(255);paint.setShader(new LinearGradient(0,0,0,dp(8),INK,INK&0x00ffffff,Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,dp(8),paint);paint.setShader(new LinearGradient(0,bottom-dp(8),0,bottom,INK&0x00ffffff,INK,Shader.TileMode.CLAMP));canvas.drawRect(0,bottom-dp(8),w,bottom,paint);paint.setShader(null);canvas.restore();paint.setColor(MUTED);paint.setAlpha(isEnabled()?255:90);paint.setTextSize(12*sp);Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText("EV",cx,h-dp(4)-fm.descent,paint);paint.setAlpha(255);
    }
}
