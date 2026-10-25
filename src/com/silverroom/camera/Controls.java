package com.silverroom.camera;

import android.content.Context;
import android.graphics.*;
import android.view.*;

final class Controls {
    static int INK,PANEL,CREAM,MUTED,AMBER,CYAN,MAGENTA;
    static {theme();}
    static void theme(){INK=tone(AppTheme.BG);PANEL=tone(AppTheme.SURFACE);CREAM=tone(AppTheme.TEXT);MUTED=tone(AppTheme.SECONDARY);AMBER=tone(AppTheme.ACCENT);CYAN=tone(AppTheme.TEAL);MAGENTA=tone(AppTheme.PINK);}
    static int tone(int role){return AppTheme.color(role);}
    interface Change {void value(float value);}
    /** Borderless camera toolbar: status lives inside each glyph, never in a side badge. */
    static class ToolButton extends View {
        static final int FLASH=0,GRID=1,TIMER=2,FRAME=3,SETTINGS=4,HISTOGRAM=5;
        final Paint p=new Paint(3);final int kind;boolean active;String badge="";float iconAngle;
        ToolButton(Context c,int type){super(c);kind=type;setClickable(true);setFocusable(true);}
        void state(boolean on,String value,String description){active=on;badge=value;setContentDescription(description);setTooltipText(description);invalidate();}
        void iconAngle(float value){iconAngle=value;invalidate();}
        @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
        private void inside(Canvas c,String text,float x,float y,float width,float height,float scale,float sp){p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextAlign(Paint.Align.CENTER);p.setFontFeatureSettings("tnum");p.setTextSize(Math.min(10*sp/scale,height));float fit=Math.min(1,width/Math.max(1,p.measureText(text)));p.setTextSize(p.getTextSize()*fit);Paint.FontMetrics fm=p.getFontMetrics();c.drawText(text,x,y-(fm.ascent+fm.descent)/2,p);p.setStyle(Paint.Style.STROKE);}
        @Override protected void onDraw(Canvas c){
            float d=getResources().getDisplayMetrics().density,sp=getResources().getDisplayMetrics().scaledDensity,cx=getWidth()/2f,cy=getHeight()/2f,scale=d*22/24f;
            p.setColor(active?AMBER:(kind==SETTINGS?CREAM:MUTED));p.setAlpha(isEnabled()?255:90);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            c.save();c.rotate(iconAngle,cx,cy);c.translate(cx-12*scale,cy-12*scale);c.scale(scale,scale);
            if(kind==FLASH){Path bolt=new Path();bolt.moveTo(14,2);bolt.lineTo(5,14);bolt.lineTo(11,14);bolt.lineTo(9,22);bolt.lineTo(19,9);bolt.lineTo(13,9);bolt.close();c.drawPath(bolt,p);if(!active)c.drawLine(3,3,21,21,p);else if("A".equals(badge))inside(c,"A",20,4,6,6,scale,sp);}
            if(kind==GRID){c.drawRoundRect(3,3,21,21,2,2,p);c.drawLine(9,3,9,21,p);c.drawLine(15,3,15,21,p);c.drawLine(3,9,21,9,p);c.drawLine(3,15,21,15,p);}
            if(kind==TIMER){c.drawCircle(12,13,8,p);c.drawLine(9,2,15,2,p);c.drawLine(12,5,12,2,p);c.drawLine(18,6,20,4,p);if(active){String seconds=badge.endsWith("s")?badge.substring(0,badge.length()-1):badge;inside(c,seconds,12,13,12,10,scale,sp);}else{c.drawLine(12,13,12,8,p);c.drawLine(12,13,16,14,p);}}
            if(kind==FRAME){float ratio="1:1".equals(badge)?1:"3:2".equals(badge)?1.5f:"16:9".equals(badge)?16f/9:"4:3".equals(badge)?4f/3:.7f,w=ratio<1?14:ratio==1?18:20,h=w/ratio;c.drawRoundRect(12-w/2,12-h/2,12+w/2,12+h/2,1.5f,1.5f,p);}
            if(kind==HISTOGRAM){Path plot=new Path();plot.moveTo(3,20);plot.lineTo(3,15);plot.lineTo(6,15);plot.lineTo(6,9);plot.lineTo(9,9);plot.lineTo(9,4);plot.lineTo(12,4);plot.lineTo(12,11);plot.lineTo(15,11);plot.lineTo(15,7);plot.lineTo(18,7);plot.lineTo(18,16);plot.lineTo(21,16);plot.lineTo(21,20);c.drawPath(plot,p);c.drawLine(3,20,21,20,p);if(!active)c.drawLine(3,3,21,21,p);}
            if(kind==SETTINGS){Path gear=new Path();for(int i=0;i<32;i++){double a=Math.toRadians(i*11.25);float r=i%4<2?10:8,x=12+(float)Math.cos(a)*r,y=12+(float)Math.sin(a)*r;if(i==0)gear.moveTo(x,y);else gear.lineTo(x,y);}gear.close();c.drawPath(gear,p);c.drawCircle(12,12,3,p);}
            c.restore();if(isFocused()||isPressed()){p.setStrokeWidth(d*1.5f);c.drawLine(cx-5*d,cy+18*d,cx+5*d,cy+18*d,p);}
        }
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.ImageButton");info.setCheckable(kind!=FRAME&&kind!=SETTINGS);info.setChecked(active);}
    }
    static class ExposureButton extends ExposureWheel {ExposureButton(Context c){super(c);}}
    static class Dial extends View {
        final Paint p=new Paint(3);float value,min=-2,max=2,step=.1f;int accent=AMBER,pointer=-1;String label="EV";Change change;
        RotaryGesture rotation=new RotaryGesture(min,max,step);
        Dial(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);set(0);}
        void configure(String l,float lo,float hi,float st,int color){label=l;min=lo;max=hi;step=st;accent=color;rotation=new RotaryGesture(min,max,step);set(value);}
        void set(float v){value=Math.max(min,Math.min(max,v));setContentDescription(label+" "+String.format(java.util.Locale.US,max<=3?"%+.1f":"%+.0f",value)+"，顺时针旋转增加，逆时针旋转减少");invalidate();}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight(),cx=w/2,cy=h/2,r=Math.min(w,h)*.43f;
            p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(cx-r*.3f,cy-r*.4f,r*1.7f,new int[]{tone(AppTheme.DIAL_HI),tone(AppTheme.DIAL_MID),tone(AppTheme.DIAL_LOW)},null,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,r,p);p.setShader(null);
            p.setColor(tone(AppTheme.DIAL_BORDER));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);c.drawCircle(cx,cy,r,p);p.setColor(tone(AppTheme.DIAL_INNER));c.drawCircle(cx,cy,r*.76f,p);
            c.save();c.rotate(value/(max-min)*270,cx,cy);
            for(int i=0;i<48;i++){double a=i*Math.PI/24;float rr=r*(i%4==0?.78f:.86f);p.setColor(i%4==0?CREAM:tone(AppTheme.TICK));p.setStrokeWidth(i%4==0?2:1);c.drawLine(cx+(float)Math.sin(a)*rr,cy-(float)Math.cos(a)*rr,cx+(float)Math.sin(a)*r*.96f,cy-(float)Math.cos(a)*r*.96f,p);}c.restore();
            p.setStyle(Paint.Style.FILL);p.setColor(accent);Path mark=new Path();mark.moveTo(cx,cy-r+2);mark.lineTo(cx-4,cy-r-6);mark.lineTo(cx+4,cy-r-6);mark.close();c.drawPath(mark,p);
            p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(r*.38f);p.setColor(CREAM);String v=max<=3?String.format(java.util.Locale.US,"%+.1f",value):String.format(java.util.Locale.US,"%+.0f",value);c.drawText(v,cx,cy+2,p);p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(Math.max(r*.21f,12*getResources().getDisplayMetrics().scaledDensity));p.setColor(accent);c.drawText(label.contains(" ")?label.substring(0,label.indexOf(" ")):label,cx,cy+r*.7f,p);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){pointer=e.getPointerId(0);rotation.begin(e.getX()-getWidth()/2f,e.getY()-getHeight()/2f,Math.min(getWidth(),getHeight())*.43f,value);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);setPressed(true);return true;}
            if(action==MotionEvent.ACTION_MOVE){int index=e.findPointerIndex(pointer);if(index>=0)adjust(rotation.move(e.getX(index)-getWidth()/2f,e.getY(index)-getHeight()/2f));return true;}
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||(action==MotionEvent.ACTION_POINTER_UP&&e.getPointerId(e.getActionIndex())==pointer)){pointer=-1;setPressed(false);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);if(action==MotionEvent.ACTION_UP)performClick();return true;}return true;
        }
        void adjust(float next){next=Math.max(min,Math.min(max,Math.round(next/step)*step));if(Math.abs(next-value)<step*.1f)return;performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);set(next);if(change!=null)change.value(value);}
        @Override public boolean performClick(){super.performClick();return true;}
        @Override public boolean onKeyDown(int key,android.view.KeyEvent e){if(key==android.view.KeyEvent.KEYCODE_DPAD_LEFT||key==android.view.KeyEvent.KEYCODE_DPAD_RIGHT){adjust(value+(key==KeyEvent.KEYCODE_DPAD_RIGHT?step:-step));return true;}return super.onKeyDown(key,e);}
        @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");info.setRangeInfo(android.view.accessibility.AccessibilityNodeInfo.RangeInfo.obtain(1,min,max,value));info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
        @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(action==4096||action==8192){adjust(value+(action==4096?step:-step));return true;}return super.performAccessibilityAction(action,args);}
    }
    static class Shutter extends View {
        Paint p=new Paint(3);boolean busy;Shutter(Context c){super(c);setContentDescription("快门，拍摄照片");setFocusable(true);}
        @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
        @Override protected void onDraw(Canvas c){float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)*.83f,d=getResources().getDisplayMetrics().density;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d*2.6f);p.setColor(tone(AppTheme.SHUTTER));c.drawCircle(cx,cy,r,p);p.setStyle(Paint.Style.FILL);p.setColor(tone(busy?AppTheme.SHUTTER_BUSY:isPressed()?AppTheme.SHUTTER_PRESSED:AppTheme.SHUTTER));c.drawCircle(cx,cy,r-d*(isPressed()?7:5.5f),p);if(busy){p.setColor(INK);float side=r*.37f;c.drawRoundRect(cx-side,cy-side,cx+side,cy+side,d*4,d*4,p);}}
    }
    static class Guides extends View {
        Paint p=new Paint(3);boolean locked;CompositionState composition;LevelMath.Reading level;boolean exposureVisible,exposureDragging;float exposureEv;final Runnable clearExposure=()->{exposureVisible=false;exposureDragging=false;invalidate();};float fx=-1,fy=-1;long focusAt;final Runnable clearFocus=()->{fx=-1;invalidate();};
        Guides(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        void hideExposure(){removeCallbacks(clearExposure);exposureVisible=false;exposureDragging=false;invalidate();}
        void exposure(float ev,boolean active){exposureEv=ev;if(active){removeCallbacks(clearExposure);exposureVisible=exposureDragging=true;focusAt=android.os.SystemClock.uptimeMillis()-200;removeCallbacks(clearFocus);}else if(exposureDragging){exposureDragging=false;if(exposureVisible)postDelayed(clearExposure,1000);if(fx>=0&&!locked){removeCallbacks(clearFocus);postDelayed(clearFocus,3000);}}invalidate();}
        void focus(float x,float y){hideExposure();locked=false;removeCallbacks(clearFocus);fx=x;fy=y;focusAt=android.os.SystemClock.uptimeMillis();invalidate();postDelayed(clearFocus,3000);}
        void lock(boolean active){if(active)hideExposure();boolean was=locked;locked=active;removeCallbacks(clearFocus);if(!active&&was)fx=-1;invalidate();}
        @Override protected void onDetachedFromWindow(){removeCallbacks(clearFocus);hideExposure();locked=false;fx=-1;super.onDetachedFromWindow();}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density;p.setStrokeWidth(1);p.setStyle(Paint.Style.FILL);p.setColor(0x60fff7dc);GuideOverlay.draw(c,p,w,h,d,composition,level);p.setColor(0x99e8ab4f);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);float len=18;for(int i=0;i<4;i++){float x=(i%2==0?10:w-10),y=(i<2?10:h-10),dx=i%2==0?len:-len,dy=i<2?len:-len;c.drawLine(x,y,x+dx,y,p);c.drawLine(x,y,x,y+dy,p);}if(fx>=0)drawFocus(c,w,h,d);if(exposureVisible&&fx>=0){float x=Math.max(54*d,Math.min(w-54*d,fx+70*d)),y=Math.max(20*d,Math.min(h-45*d,fy));p.setStyle(Paint.Style.FILL);p.setColor(0xcc121514);c.drawRoundRect(x-48*d,y-17*d,x+48*d,y+17*d,8*d,8*d,p);p.setColor(0xffffd365);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",0));p.setTextSize(13*d);c.drawText("☀ "+ExposureValue.label(exposureEv)+" EV",x,y+5*d,p);}p.setAlpha(255);p.setStyle(Paint.Style.FILL);}
        void drawFocus(Canvas c,float w,float h,float d){
            long age=android.os.SystemClock.uptimeMillis()-focusAt;float fade=locked||age<2700?1:Math.max(0,(3000-age)/300f);float half=Math.min(26*d*(1+.18f*Math.max(0,1-age/180f)),Math.min(w,h)/2-4*d);if(half<=0)return;
            float cx=Math.max(half+4*d,Math.min(w-half-4*d,fx)),cy=Math.max(half+4*d,Math.min(h-half-4*d,fy));float left=cx-half,top=cy-half,right=cx+half,bottom=cy+half,arm=13*d;
            Path bracket=new Path();bracket.moveTo(left,top+arm);bracket.lineTo(left,top);bracket.lineTo(left+arm,top);bracket.moveTo(right-arm,top);bracket.lineTo(right,top);bracket.lineTo(right,top+arm);bracket.moveTo(right,bottom-arm);bracket.lineTo(right,bottom);bracket.lineTo(right-arm,bottom);bracket.moveTo(left+arm,bottom);bracket.lineTo(left,bottom);bracket.lineTo(left,bottom-arm);
            p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0xff10120b);p.setAlpha(Math.round(230*fade));p.setStrokeWidth(6*d);c.drawPath(bracket,p);p.setColor(0xffffe365);p.setAlpha(Math.round(255*fade));p.setStrokeWidth(3*d);c.drawPath(bracket,p);
            // The box stays fully visible at the edges; the cross marks the actual tap.
            float px=Math.max(4*d,Math.min(w-4*d,fx)),py=Math.max(4*d,Math.min(h-4*d,fy));Path cross=new Path();cross.moveTo(px-5*d,py);cross.lineTo(px+5*d,py);cross.moveTo(px,py-5*d);cross.lineTo(px,py+5*d);p.setColor(0xff10120b);p.setAlpha(Math.round(230*fade));p.setStrokeWidth(4*d);c.drawPath(cross,p);p.setColor(0xffffe365);p.setAlpha(Math.round(255*fade));p.setStrokeWidth(1.6f*d);c.drawPath(cross,p);p.setStrokeCap(Paint.Cap.BUTT);p.setAlpha(255);if(locked){if(age<180)postInvalidateOnAnimation();}else if(age<180||age>=2700)postInvalidateOnAnimation();else postInvalidateDelayed(2700-age);
        }
    }
}

