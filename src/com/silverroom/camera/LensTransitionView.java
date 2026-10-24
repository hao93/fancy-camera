package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

/** Mechanical aperture overlay over the retained preview; never controls camera readiness. */
final class LensTransitionView extends View {
    private final Paint paint=new Paint(3);private boolean running;private long started,ending=-1;private float focal=23;
    private final Runnable tick=()->{if(!running)return;if(ending>=0&&SystemClock.uptimeMillis()-ending>=160){stop();return;}invalidate();if(ValueAnimator.areAnimatorsEnabled())postDelayed(this.tick,33);};
    LensTransitionView(Context context){super(context);setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setVisibility(GONE);}
    void state(CameraUiState value){
        focal=value.focal;
        if(value.phase==CameraUiState.ERROR||value.phase==CameraUiState.PAUSED){stop();return;}
        if(value.phase==CameraUiState.SWITCHING){ending=-1;if(!running){running=true;started=SystemClock.uptimeMillis();setVisibility(VISIBLE);}removeCallbacks(tick);if(ValueAnimator.areAnimatorsEnabled())postDelayed(tick,33);invalidate();}
        else if(value.phase==CameraUiState.READY&&running&&ending<0){if(!ValueAnimator.areAnimatorsEnabled()){stop();return;}ending=SystemClock.uptimeMillis();removeCallbacks(tick);postDelayed(tick,33);}
    }
    void stop(){running=false;ending=-1;removeCallbacks(tick);setVisibility(GONE);}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas){
        if(!running)return;long now=SystemClock.uptimeMillis();boolean animate=ValueAnimator.areAnimatorsEnabled();float fade=ending<0?1:Math.max(0,1-(now-ending)/160f),entry=animate?Math.min(1,(now-started)/160f):1;
        float d=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(34*d,Math.min(getWidth(),getHeight())*.22f),angle=animate?(now-started)%1800/5f:0;
        if(r<=0)return;float opacity=fade*entry;paint.setStyle(Paint.Style.FILL);paint.setColor(0xff101713);paint.setAlpha(Math.round(22*opacity));canvas.drawRect(0,0,getWidth(),getHeight(),paint);
        paint.setColor(AppTheme.OVERLAY_BG);paint.setAlpha(Math.round(135*opacity));canvas.drawCircle(cx,cy,r+12*d,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(1.8f*d);paint.setColor(AppTheme.OVERLAY_ACCENT);paint.setAlpha(Math.round(230*opacity));
        canvas.drawArc(cx-r,cy-r,cx+r,cy+r,angle,115,false,paint);canvas.drawArc(cx-r,cy-r,cx+r,cy+r,angle+180,35,false,paint);
        paint.setColor(0xff7ed3c8);paint.setAlpha(Math.round(170*opacity));float inner=r-5*d;canvas.drawArc(cx-inner,cy-inner,cx+inner,cy+inner,-angle+40,80,false,paint);
        for(int i=0;i<6;i++){double a=Math.toRadians(i*60+angle*.18f);float x=(float)Math.cos(a),y=(float)Math.sin(a);canvas.drawLine(cx+x*(r+5*d),cy+y*(r+5*d),cx+x*(r+8*d),cy+y*(r+8*d),paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(AppTheme.OVERLAY_TEXT);paint.setAlpha(Math.round(255*opacity));paint.setTypeface(Typeface.create("monospace",0));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);String label=ZoomScale.label(focal)+"mm";float w=paint.measureText(label);if(w>r*1.65f)paint.setTextSize(paint.getTextSize()*r*1.65f/w);Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText(label,cx,cy-(fm.ascent+fm.descent)/2,paint);
    }
}
