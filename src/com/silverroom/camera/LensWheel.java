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
    private boolean dragged,disabling,emphasisTarget,captionVisible;private float emphasis,captionOpacity;private int captionEpoch;private VelocityTracker velocity;private ValueAnimator motion,emphasisMotion,captionMotion;private final Paint brush=new Paint(3);
    private final Runnable clearCaption=this::fadeCaption;
    private final Runnable held=()->{if(pointer>=0&&!dragged&&isLongClickable()){performLongClick();stop();hideCaption();}};
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
    private void release(){removeCallbacks(held);pointer=-1;if(velocity!=null){velocity.recycle();velocity=null;}if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();}
    private void stop(){if(motion!=null){motion.cancel();motion=null;}release();}
    @Override boolean moving(){return pointer>=0||(motion!=null&&motion.isRunning());}
    @Override void finish(){stop();hideCaption();if(disabling)return;float target=ZoomScale.snap(state.position(),enabled);if(Float.isFinite(target))updateZoom(target);}
    @Override public void setEnabled(boolean value){disabling=!value;if(disabling&&state!=null){stop();hideCaption();}super.setEnabled(value);disabling=false;}
    private void settle(float target){stop();removeCallbacks(clearCaption);if(!Float.isFinite(target)){scheduleCaption();return;}if(Math.abs(target-state.position())<.001f){updateZoom(target);scheduleCaption();return;}motion=ValueAnimator.ofFloat(state.position(),target);motion.setDuration(220);motion.setInterpolator(new DecelerateInterpolator(2));motion.addUpdateListener(a->updateZoom((float)a.getAnimatedValue()));motion.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){scheduleCaption();invalidate();}});motion.start();}
    @Override protected void onDetachedFromWindow(){stop();hideCaption();super.onDetachedFromWindow();}
    private void keepCaption(){captionEpoch++;removeCallbacks(clearCaption);if(captionMotion!=null){captionMotion.cancel();captionMotion=null;}captionVisible=true;captionOpacity=1;emphasis(true);invalidate();}
    private void scheduleCaption(){removeCallbacks(clearCaption);if(captionVisible&&pointer<0&&(motion==null||!motion.isRunning()))postDelayed(clearCaption,900);}
    private void fadeCaption(){if(!captionVisible||pointer>=0||(motion!=null&&motion.isRunning()))return;final int token=++captionEpoch;
        captionMotion=ValueAnimator.ofFloat(captionOpacity,0);captionMotion.setDuration(200);captionMotion.addUpdateListener(a->{captionOpacity=(float)a.getAnimatedValue();invalidate();});
        captionMotion.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){if(token!=captionEpoch)return;captionVisible=false;captionOpacity=0;emphasis(false);invalidate();}});captionMotion.start();}
    private void hideCaption(){captionEpoch++;removeCallbacks(clearCaption);if(captionMotion!=null){captionMotion.cancel();captionMotion=null;}if(emphasisMotion!=null){emphasisMotion.cancel();emphasisMotion=null;}captionVisible=false;captionOpacity=0;emphasis=0;emphasisTarget=false;invalidate();}
    @Override protected float pitch(){return dp(72)*Math.max(1,getResources().getConfiguration().fontScale);}
    @Override String title(int id){return MM[id]+"";}
    private static final class NumberLayout {
        final float size,width,height,center,numberHeight;
        NumberLayout(float s,float w,float h,float c,float n){size=s;width=w;height=h;center=c;numberHeight=n;}
    }
    private NumberLayout numberLayout(String text,Paint paint,float slot,float grow){
        float original=paint.getTextSize();Paint.FontMetrics fm=paint.getFontMetrics();float w=paint.measureText(text),h=fm.descent-fm.ascent;
        double radians=Math.toRadians(numberAngle);float sine=(float)Math.abs(Math.sin(radians)),cosine=(float)Math.abs(Math.cos(radians));
        float reference=paint.measureText("230.0"),layoutWidth=w+(reference-w)*grow;
        float rotatedWidth=layoutWidth*cosine+h*sine,rotatedHeight=layoutWidth*sine+h*cosine;
        paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);fm=paint.getFontMetrics();float caption=(fm.descent-fm.ascent+dp(4))*grow,captionWidth=paint.measureText("230.0mm");paint.setTextSize(original);
        float scale=Math.min(1,Math.min((slot-dp(16))/Math.max(1,rotatedWidth),(getHeight()-dp(16)-caption)/Math.max(1,rotatedHeight)));
        float width=Math.max(dp(32),Math.max(rotatedWidth*scale+dp(12),(captionWidth+dp(12))*grow));
        return new NumberLayout(original*scale,Math.min(slot*.94f,width),rotatedHeight*scale+dp(8)+caption,-caption/2,rotatedHeight*scale);
    }
    @Override void drawTitle(Canvas canvas,String text,float baseline,Paint paint,float slot){drawTitle(canvas,text,paint,numberLayout(text,paint,slot,0));}
    private void drawTitle(Canvas canvas,String text,Paint paint,NumberLayout layout){
        float original=paint.getTextSize();paint.setTextSize(layout.size);Paint.FontMetrics fm=paint.getFontMetrics();
        canvas.save();canvas.rotate(numberAngle,0,layout.center);canvas.drawText(text,0,layout.center-(fm.ascent+fm.descent)/2,paint);canvas.restore();paint.setTextSize(original);
    }
    private void emphasis(boolean scrolling){
        if(emphasisTarget==scrolling)return;emphasisTarget=scrolling;if(emphasisMotion!=null)emphasisMotion.cancel();
        emphasisMotion=ValueAnimator.ofFloat(emphasis,scrolling?1:0);emphasisMotion.setDuration(140);emphasisMotion.setInterpolator(new DecelerateInterpolator(2));
        emphasisMotion.addUpdateListener(a->{emphasis=(float)a.getAnimatedValue();invalidate();});emphasisMotion.start();
    }
    @Override String subtitle(int id){return labels[id];}
    @Override boolean available(int id){return enabled[id];}
    @Override String category(){return "焦段";}
    @Override boolean compact(){return true;}
    @Override int legalTarget(int index){return LensWheelPolicy.nearest(index,enabled);}
    @Override void tapped(int target){if(!isEnabled())return;if(!available(target)){if(blocked!=null)blocked.lens(MM[target]);return;}stop();updateZoom(target);}
    @Override public boolean onTouchEvent(MotionEvent e){
        if(!isEnabled())return false;int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){stop();if(captionVisible)keepCaption();pointer=e.getPointerId(0);downX=lastX=e.getX();downY=e.getY();dragged=false;velocity=VelocityTracker.obtain();velocity.addMovement(e);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);if(isLongClickable())postDelayed(held,ViewConfiguration.getLongPressTimeout());return true;}
        if(pointer<0)return true;if(velocity!=null)velocity.addMovement(e);
        if(action==MotionEvent.ACTION_MOVE){int i=e.findPointerIndex(pointer);if(i<0){stop();hideCaption();return true;}float x=e.getX(i),dx=x-downX,dy=e.getY(i)-downY;int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if(!dragged){if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){stop();hideCaption();return true;}if(Math.abs(dx)>slop){dragged=true;keepCaption();removeCallbacks(held);updateZoom(state.position()-(dx-Math.copySign(slop,dx))/pitch());}}
            else updateZoom(state.position()+(lastX-x)/pitch());lastX=x;return true;}
        if(action==MotionEvent.ACTION_UP){if(!dragged){int target=Math.max(0,Math.min(6,Math.round(state.position()+(e.getX()-getWidth()/2f)/pitch())));release();performClick();tapped(target);scheduleCaption();}else{velocity.computeCurrentVelocity(1000);float travel=Math.max(-1.2f,Math.min(1.2f,-velocity.getXVelocity(pointer)/pitch()*.12f));float target=ZoomScale.snap(state.position()+travel,enabled);settle(target);}return true;}
        if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP){stop();hideCaption();return true;}return true;
    }
    @Override protected void onDraw(Canvas c){
        float cx=getWidth()/2f,cy=getHeight()/2f,slot=pitch(),sp=getResources().getDisplayMetrics().scaledDensity;

        float mm=ZoomScale.focal(state.position());String number=ZoomScale.label(mm);brush.setTypeface(Typeface.create("sans-serif-medium",0));brush.setFontFeatureSettings("tnum");brush.setTextSize((18+2*emphasis)*sp);NumberLayout selected=numberLayout(number,brush,slot,emphasis);
        float left=cx-selected.width/2,top=cy-selected.height/2,right=cx+selected.width/2,bottom=cy+selected.height/2;
        brush.setStyle(Paint.Style.FILL);brush.setColor(tone(AppTheme.ACTIVE));c.drawRoundRect(left,top,right,bottom,dp(10),dp(10),brush);
        brush.setStyle(Paint.Style.STROKE);brush.setStrokeWidth(dp(.8f));brush.setColor(tone(AppTheme.SELECT_BORDER));c.drawRoundRect(left,top,right,bottom,dp(10),dp(10),brush);brush.setStyle(Paint.Style.FILL);
        for(int i=0;i<7;i++){float distance=i-state.position(),x=cx+distance*slot;if(Math.abs(distance)<.74f||x< -slot||x>getWidth()+slot)continue;drawNumber(c,title(i),"",x,cy,slot,sp,enabled[i]?CREAM:MUTED,enabled[i]?195:75,0);}
        drawNumber(c,number,captionVisible?number+"mm":"",cx,cy,slot,sp,ZoomScale.allowed(mm,enabled)?AMBER:MUTED,isEnabled()&&ZoomScale.allowed(mm,enabled)?255:75,emphasis);
        brush.setColor(MUTED);brush.setAlpha(110);brush.setStrokeWidth(dp(.7f));for(int tick=0;tick<=24;tick++){float x=cx+(tick/4f-state.position())*slot;if(x<dp(4)||x>getWidth()-dp(4))continue;c.drawLine(x,getHeight()-dp(tick%4==0?4:2),x,getHeight(),brush);}brush.setAlpha(255);
    }
    private void drawNumber(Canvas c,String title,String note,float x,float y,float slot,float sp,int color,int alpha,float grow){
        c.save();c.translate(x,y);brush.setTypeface(Typeface.create("sans-serif-medium",0));brush.setTextAlign(Paint.Align.CENTER);brush.setFontFeatureSettings("tnum");brush.setTextSize((18+2*grow)*sp);NumberLayout layout=numberLayout(title,brush,slot,grow);brush.setColor(color);brush.setAlpha(alpha);drawTitle(c,title,brush,layout);
        if(!note.isEmpty()){brush.setTextSize(20*sp);NumberLayout fixed=numberLayout("230.0",brush,slot,1);brush.setTextSize(12*sp);brush.setColor(MUTED);brush.setAlpha(Math.round(alpha*captionOpacity*Math.min(1,emphasis*1.2f)));Paint.FontMetrics fm=brush.getFontMetrics();c.drawText(note,0,fixed.center+fixed.numberHeight/2+dp(4)-fm.ascent,brush);}c.restore();
    }
    private boolean step(int direction){if(!isEnabled())return false;stop();int target=direction>0?(int)Math.floor(state.position()+.001f)+1:(int)Math.ceil(state.position()-.001f)-1;while(target>=0&&target<7){if(enabled[target]){tapped(target);return true;}target+=direction;}return false;}
    @Override public boolean onKeyDown(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)return step(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);return super.onKeyDown(key,e);}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(1,14,230,ZoomScale.focal(state.position())));}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(action==4096||action==8192)return step(action==4096?1:-1);return super.performAccessibilityAction(action,args);}
}
