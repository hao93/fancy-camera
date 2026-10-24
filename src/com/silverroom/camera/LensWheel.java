package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Arrays;
import static com.silverroom.camera.Controls.*;

/** Continuous focal reel with optional anchor snapping and unchanged portrait touch coordinates. */
final class LensWheel extends FilmWheel {
    static final int[] MM=ZoomScale.ANCHORS;
    private boolean[] enabled=new boolean[7];private String[] labels={"检测中","检测中","检测中","检测中","检测中","检测中","检测中"};
    interface Blocked {void lens(int mm);}interface Zoomed {void focal(float mm);}
    Blocked blocked;Zoomed zoomed;
    private float numberAngle,downX,downY,lastX,lastDispatched=23;private int pointer=-1,lastAnchor=-1;
    private boolean dragged,disabling;private VelocityTracker velocity;private ValueAnimator motion;private final Paint brush=new Paint(3);
    private final Runnable held=()->{if(pointer>=0&&!dragged&&isLongClickable()){performLongClick();stop();}};
    void numberAngle(float value){numberAngle=value;invalidate();}
    LensWheel(Context c){super(c);films(Arrays.asList(0,1,2,3,4,5,6),1);describeZoom();}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    void focal(float mm){if(moving())return;state.position(ZoomScale.position(mm));lastDispatched=mm;describeZoom();invalidate();}
    void availability(boolean[] next,String[] text,int actual,boolean front){
        enabled=next.clone();labels=text.clone();setEnabled(!front);
        if(!moving()&&!ZoomScale.allowed(ZoomScale.focal(state.position()),enabled)&&actual>=0&&enabled[actual])focal(MM[actual]);invalidate();
    }
    private void describeZoom(){setContentDescription("焦段 "+ZoomScale.label(ZoomScale.focal(state.position()))+"毫米，左右滑动连续变焦，靠近常用焦段时吸附，点击数字直达");}
    private void updateZoom(float position){
        float legal=ZoomScale.constrain(position,enabled);if(!Float.isFinite(legal))return;int old=state.selected();state.position(legal);float mm=ZoomScale.focal(legal);
        int anchor=Math.round(legal);if(Math.abs(legal-anchor)<.10f&&enabled[anchor]&&anchor!=lastAnchor){lastAnchor=anchor;performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);}
        if(Math.abs(mm-lastDispatched)>.049f){lastDispatched=mm;describeZoom();if(zoomed!=null)zoomed.focal(mm);else if(old!=state.selected()&&listener!=null)listener.selected(state.selected());}
        invalidate();
    }
    private void release(){removeCallbacks(held);pointer=-1;if(velocity!=null){velocity.recycle();velocity=null;}if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    private void stop(){if(motion!=null){motion.cancel();motion=null;}release();}
    @Override boolean moving(){return pointer>=0||(motion!=null&&motion.isRunning());}
    @Override void finish(){stop();if(disabling)return;float target=ZoomScale.snap(state.position(),enabled);if(Float.isFinite(target))updateZoom(target);}
    @Override public void setEnabled(boolean value){disabling=!value;if(disabling&&state!=null)stop();super.setEnabled(value);disabling=false;}
    private void settle(float target){stop();if(!Float.isFinite(target))return;if(Math.abs(target-state.position())<.001f){updateZoom(target);return;}motion=ValueAnimator.ofFloat(state.position(),target);motion.setDuration(220);motion.setInterpolator(new DecelerateInterpolator(2));motion.addUpdateListener(a->updateZoom((float)a.getAnimatedValue()));motion.start();}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override protected float pitch(){return dp(72);}
    @Override String title(int id){return MM[id]+"";}
    @Override void drawTitle(Canvas canvas,String text,float baseline,Paint brush,float slot){
        float d=getResources().getDisplayMetrics().density,original=brush.getTextSize();
        brush.setTextSize(9*getResources().getDisplayMetrics().scaledDensity);Paint.FontMetrics subtitle=brush.getFontMetrics();brush.setTextSize(original);Paint.FontMetrics fm=brush.getFontMetrics();
        float top=-23*d,bottom=Math.max(-17*d,Math.min(8*d,14*d+subtitle.ascent-2*d)),center=(top+bottom)/2;
        // Reserve a separate number region above the stationary lens-source subtitle.
        double radians=Math.toRadians(numberAngle);float sine=(float)Math.abs(Math.sin(radians)),cosine=(float)Math.abs(Math.cos(radians));
        float w=brush.measureText(text),h=fm.descent-fm.ascent;
        float scale=Math.min(1,Math.min((slot-16*d)/Math.max(1,w*cosine+h*sine),(bottom-top)/Math.max(1,w*sine+h*cosine)));
        brush.setTextSize(original*scale);fm=brush.getFontMetrics();
        canvas.save();canvas.rotate(numberAngle,0,center);canvas.drawText(text,0,center-(fm.ascent+fm.descent)/2,brush);canvas.restore();brush.setTextSize(original);
    }
    @Override String subtitle(int id){return labels[id];}
    @Override boolean available(int id){return enabled[id];}
    @Override String category(){return "焦段";}
    @Override boolean compact(){return true;}
    @Override int legalTarget(int index){return LensWheelPolicy.nearest(index,enabled);}
    @Override void tapped(int target){if(!isEnabled())return;if(!available(target)){if(blocked!=null)blocked.lens(MM[target]);return;}stop();updateZoom(target);}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return false;int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){stop();pointer=e.getPointerId(0);downX=lastX=e.getX();downY=e.getY();dragged=false;velocity=VelocityTracker.obtain();velocity.addMovement(e);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);if(isLongClickable())postDelayed(held,ViewConfiguration.getLongPressTimeout());return true;}
        if(pointer<0)return true;if(velocity!=null)velocity.addMovement(e);
        if(action==MotionEvent.ACTION_MOVE){int i=e.findPointerIndex(pointer);if(i<0){stop();return true;}float x=e.getX(i),dx=x-downX,dy=e.getY(i)-downY;int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if(!dragged){if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){stop();return true;}if(Math.abs(dx)>slop){dragged=true;removeCallbacks(held);updateZoom(state.position()-(dx-Math.copySign(slop,dx))/pitch());}}
            else updateZoom(state.position()+(lastX-x)/pitch());lastX=x;return true;}
        if(action==MotionEvent.ACTION_UP){if(!dragged){int target=Math.max(0,Math.min(6,Math.round(state.position()+(e.getX()-getWidth()/2f)/pitch())));release();performClick();tapped(target);}else{velocity.computeCurrentVelocity(1000);float travel=Math.max(-1.2f,Math.min(1.2f,-velocity.getXVelocity(pointer)/pitch()*.12f));float target=ZoomScale.snap(state.position()+travel,enabled);settle(target);}return true;}
        if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP){stop();return true;}return true;
    }
    @Override protected void onDraw(Canvas c){
        float cx=getWidth()/2f,cy=getHeight()/2f,slot=pitch(),sp=getResources().getDisplayMetrics().scaledDensity;
        brush.setStyle(Paint.Style.FILL);brush.setColor(tone(AppTheme.ACTIVE));c.drawRoundRect(cx-slot*.47f,cy-dp(22),cx+slot*.47f,cy+dp(22),dp(13),dp(13),brush);
        brush.setStyle(Paint.Style.STROKE);brush.setStrokeWidth(dp(.8f));brush.setColor(tone(AppTheme.SELECT_BORDER));c.drawRoundRect(cx-slot*.47f,cy-dp(22),cx+slot*.47f,cy+dp(22),dp(13),dp(13),brush);brush.setStyle(Paint.Style.FILL);
        for(int i=0;i<7;i++){float distance=i-state.position(),x=cx+distance*slot;if(Math.abs(distance)<.74f||x< -slot||x>getWidth()+slot)continue;drawNumber(c,title(i),labels[i],x,cy,slot,sp,enabled[i]?CREAM:MUTED,enabled[i]?195:75);}
        float mm=ZoomScale.focal(state.position());int base=LensPlan.base(mm);String note=ZoomScale.allowed(mm,enabled)?(Math.abs(mm-base)<.05f?"光学":"×"+String.format(java.util.Locale.US,"%.2f",mm/base)):labels[state.index()];
        drawNumber(c,ZoomScale.label(mm),note,cx,cy,slot,sp,ZoomScale.allowed(mm,enabled)?AMBER:MUTED,isEnabled()&&ZoomScale.allowed(mm,enabled)?255:75);
        brush.setColor(MUTED);brush.setAlpha(110);brush.setStrokeWidth(dp(.7f));for(int tick=0;tick<=24;tick++){float x=cx+(tick/4f-state.position())*slot;if(x<dp(4)||x>getWidth()-dp(4))continue;c.drawLine(x,getHeight()-dp(tick%4==0?4:2),x,getHeight(),brush);}brush.setAlpha(255);
    }
    private void drawNumber(Canvas c,String title,String note,float x,float y,float slot,float sp,int color,int alpha){
        c.save();c.translate(x,y);brush.setTypeface(Typeface.create("sans-serif-medium",0));brush.setTextAlign(Paint.Align.CENTER);brush.setTextSize(15*sp);brush.setColor(color);brush.setAlpha(alpha);drawTitle(c,title,-dp(1),brush,slot);
        brush.setTextSize(9*sp);brush.setColor(MUTED);while(brush.measureText(note)>slot-dp(10)&&brush.getTextSize()>dp(6))brush.setTextSize(brush.getTextSize()*.95f);c.drawText(note,0,dp(14),brush);c.restore();
    }
    private boolean step(int direction){if(!isEnabled())return false;stop();int target=direction>0?(int)Math.floor(state.position()+.001f)+1:(int)Math.ceil(state.position()-.001f)-1;while(target>=0&&target<7){if(enabled[target]){tapped(target);return true;}target+=direction;}return false;}
    @Override public boolean onKeyDown(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)return step(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);return super.onKeyDown(key,e);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(1,14,230,ZoomScale.focal(state.position())));}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(action==4096||action==8192)return step(action==4096?1:-1);return super.performAccessibilityAction(action,args);}
}
