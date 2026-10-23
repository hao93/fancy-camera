package com.silverroom.camera;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.silverroom.camera.Controls.*;

/** Compact and expanded charts share the same camera sample and latest-only UI commit. */
final class HistogramView extends View implements CameraEngine.HistogramSink {
    private final Paint paint=new Paint(3);private final AtomicBoolean posted=new AtomicBoolean();private volatile int[][] newest;private int[][] displayed;
    private final boolean expanded;private final PreviewChannel<HistogramView> mirrors=new PreviewChannel<>();
    HistogramView(Context context){this(context,false);}
    HistogramView(Context context,boolean large){super(context);expanded=large;setContentDescription(large?"放大的实时亮度与RGB直方图，左暗右亮":"实时亮度与RGB直方图，点击放大");if(!large){setClickable(true);setFocusable(true);setTooltipText("放大直方图");}}
    void attach(HistogramView chart){mirrors.set(chart);if(newest!=null)chart.histogram(newest);}
    void detach(HistogramView chart){mirrors.remove(chart);}
    @Override public void histogram(int[][] bins){newest=bins;schedule();}
    private void schedule(){if(posted.compareAndSet(false,true))post(()->{displayed=newest;posted.set(false);invalidate();HistogramView mirror=mirrors.get();if(mirror!=null)mirror.histogram(displayed);if(newest!=displayed)schedule();});}
    @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
    @Override protected void onDraw(Canvas c){
        float d=getResources().getDisplayMetrics().density,sp=getResources().getDisplayMetrics().scaledDensity,w=getWidth(),h=getHeight(),left=(expanded?14:5)*d,right=w-(expanded?14:5)*d,top=(expanded?14:5)*d,bottom=h-(expanded?30:16)*d;
        paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setColor(isPressed()?tone(AppTheme.PRESSED):PANEL);c.drawRoundRect(0,0,w,h,(expanded?12:8)*d,(expanded?12:8)*d,paint);paint.setStrokeWidth(d*(expanded?1:.6f));paint.setColor(tone(AppTheme.BORDER));for(int i=1;i<4;i++)c.drawLine(left+(right-left)*i/4,top,left+(right-left)*i/4,bottom,paint);if(expanded)for(int i=1;i<=4;i++)c.drawLine(left,top+(bottom-top)*i/4,right,top+(bottom-top)*i/4,paint);
        if(displayed!=null){int max=1;for(int[] bins:displayed)for(int n:bins)max=Math.max(max,n);int[] colors={tone(AppTheme.RED),tone(AppTheme.GREEN),tone(AppTheme.BLUE),CREAM};for(int channel=0;channel<4;channel++){Path path=new Path();path.moveTo(left,bottom);for(int i=0;i<Histogram.BINS;i++){float x=left+(right-left)*i/(Histogram.BINS-1),y=bottom-(bottom-top)*displayed[channel][i]/max;path.lineTo(x,y);}paint.setColor(colors[channel]);if(channel<3){path.lineTo(right,bottom);path.close();paint.setStyle(Paint.Style.FILL);paint.setAlpha(expanded?72:80);}else{paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d*(expanded?1.8f:1));paint.setAlpha(255);}c.drawPath(path,paint);}}
        paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize((expanded?10:8)*sp);paint.setColor(MUTED);
        if(expanded){for(int i=0;i<=4;i++){paint.setTextAlign(i==0?Paint.Align.LEFT:i==4?Paint.Align.RIGHT:Paint.Align.CENTER);c.drawText(String.valueOf(Math.round(255*i/4f)),left+(right-left)*i/4,h-10*d,paint);}if(displayed==null){paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(13*sp);c.drawText("等待实时画面",w/2,(top+bottom)/2,paint);}}
        else{c.drawText(displayed==null?"等待画面":"直方图",w/2,h-4*d,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);paint.setColor(isFocused()?AMBER:MUTED);float x=w-5*d,y=5*d;c.drawLine(x-4*d,y,x,y,paint);c.drawLine(x,y,x,y+4*d,paint);if(isFocused()){paint.setColor(AMBER);c.drawRoundRect(d,d,w-d,h-d,8*d,8*d,paint);}}
    }
    @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);if(!expanded){info.setClassName("android.widget.Button");info.setCanOpenPopup(true);}}
}
