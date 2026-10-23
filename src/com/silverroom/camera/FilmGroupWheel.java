package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import static com.silverroom.camera.Controls.*;

/** A compact vertical category reel. Taps retain the complete category menu. */
final class FilmGroupWheel extends View {
    interface Listener {void selected(int group);}
    Listener listener;
    private final Paint paint=new Paint(3);
    private int count=FilmGroups.LABELS.length-1,pointer=-1;
    private float position,downX,downY,lastY;
    private boolean dragging,tapEligible;
    private ValueAnimator animator;

    FilmGroupWheel(Context context){super(context);setClickable(true);setFocusable(true);describe();}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private float pitch(){return dp(24);}
    private int selected(){return Math.round(position);}
    void group(int value,boolean hasFavorites){
        int nextCount=FilmGroups.LABELS.length-(hasFavorites?0:1);
        int next=FilmGroups.safe(value);if(next>=nextCount)next=FilmGroups.ALL;
        // A live detent callback refreshes both reels; retain this finger's fractional offset.
        if(nextCount==count&&next==selected())return;
        finish();count=nextCount;position=next;describe();invalidate();
    }
    private void describe(){setContentDescription("胶片分类滚轮，当前"+FilmGroups.LABELS[selected()]+"，上下滑动切换，点击展开分类");}
    private void update(float value){
        int previous=selected();position=Math.max(0,Math.min(count-1,value));int next=selected();
        if(next!=previous){describe();performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);if(listener!=null)listener.selected(next);sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);}
        invalidate();
    }
    private void cancelAnimation(){if(animator!=null){animator.cancel();animator=null;}}
    private void release(){pointer=-1;tapEligible=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    void finish(){cancelAnimation();release();position=selected();invalidate();}
    private void settle(){
        cancelAnimation();float target=selected();if(Math.abs(position-target)<.001f){position=target;invalidate();return;}
        animator=ValueAnimator.ofFloat(position,target);animator.setDuration(160);animator.setInterpolator(new DecelerateInterpolator(2));
        animator.addUpdateListener(a->{position=(float)a.getAnimatedValue();invalidate();});animator.start();
    }
    @Override public void setEnabled(boolean enabled){if(!enabled)finish();super.setEnabled(enabled);invalidate();}
    @Override protected void onDetachedFromWindow(){finish();super.onDetachedFromWindow();}
    @Override public boolean performClick(){if(!isEnabled())return false;finish();super.performClick();return true;}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled())return false;int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){boolean settled=animator==null||!animator.isRunning();cancelAnimation();release();pointer=event.getPointerId(0);downX=event.getX();lastY=downY=event.getY();dragging=false;tapEligible=settled;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);return true;}
        if(pointer<0)return true;
        if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_UP){finish();return true;}
        if(action==MotionEvent.ACTION_MOVE){
            int index=event.findPointerIndex(pointer);if(index<0){finish();return true;}
            float y=event.getY(index),dx=event.getX(index)-downX,dy=y-downY;int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if(!dragging){
                if(Math.abs(dx)>slop&&Math.abs(dx)>=Math.abs(dy)){finish();return true;}
                if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)*1.15f){dragging=true;tapEligible=false;update(position-(dy-Math.copySign(slop,dy))/pitch());}
            }else update(position+(lastY-y)/pitch());
            lastY=y;return true;
        }
        if(action==MotionEvent.ACTION_UP){boolean click=!dragging&&tapEligible&&Math.hypot(event.getX()-downX,event.getY()-downY)<=ViewConfiguration.get(getContext()).getScaledTouchSlop();release();if(click)performClick();else settle();return true;}
        return true;
    }
    @Override protected void onDraw(Canvas canvas){
        float cx=getWidth()/2f,cy=getHeight()/2f,half=Math.min(dp(22),cx-dp(2));
        paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setColor(tone(AppTheme.ACTIVE));canvas.drawRoundRect(cx-half,cy-dp(13),cx+half,cy+dp(13),dp(9),dp(9),paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(.8f));paint.setColor(isFocused()?AMBER:tone(AppTheme.SELECT_BORDER));canvas.drawRoundRect(cx-half,cy-dp(13),cx+half,cy+dp(13),dp(9),dp(9),paint);
        paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",0));
        canvas.save();canvas.clipRect(dp(2),dp(1),getWidth()-dp(2),getHeight()-dp(1));
        for(int i=Math.max(0,selected()-2);i<=Math.min(count-1,selected()+2);i++){
            float distance=i-position,y=cy+distance*pitch();boolean current=i==selected();
            paint.setColor(current?AMBER:MUTED);paint.setAlpha(isEnabled()?Math.round(255*(1-Math.min(.85f,Math.abs(distance)*.65f))):75);
            paint.setTextSize((current?11:9)*getResources().getDisplayMetrics().scaledDensity);
            // Fit two-character labels without crossing the adjacent horizontal film reel.
            String label=FilmGroups.SHORT_LABELS[i];float width=paint.measureText(label);if(width>half*2-dp(4))paint.setTextSize(paint.getTextSize()*(half*2-dp(4))/width);
            Paint.FontMetrics fm=paint.getFontMetrics();float baseline=y-(fm.ascent+fm.descent)/2;
            canvas.save();canvas.clipRect(0,y-dp(11),getWidth(),y+dp(11));canvas.drawText(label,cx,baseline,paint);canvas.restore();
        }
        canvas.restore();paint.setAlpha(255);
    }
    private boolean step(int direction){if(!isEnabled())return false;finish();int target=Math.max(0,Math.min(count-1,selected()+direction));if(target==selected())return false;update(target);return true;}
    @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)return step(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1);if(key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_DPAD_CENTER)return performClick();return super.onKeyDown(key,event);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");info.setScrollable(count>1);info.setCanOpenPopup(true);info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(0,0,count-1,selected()));info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(action==4096||action==8192)return step(action==4096?1:-1);return super.performAccessibilityAction(action,args);}
}
