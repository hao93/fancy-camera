package com.silverroom.camera;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import static com.silverroom.camera.Controls.*;

/** Small visual surface within a full 44dp touch target. No font-based ellipsis. */
final class FilmShelfButton extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean more;private int group;
    FilmShelfButton(Context context,boolean more){super(context);this.more=more;setClickable(true);setFocusable(true);if(more){setContentDescription("更多胶片，打开完整胶片库");setTooltipText("完整胶片库");}else group(0);}
    void group(int value){group=FilmGroups.safe(value);setContentDescription("胶片分类，当前"+FilmGroups.LABELS[group]+"，点击切换");setTooltipText("切换胶片分类");invalidate();}
    @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
    @Override protected void onDraw(Canvas canvas){
        float d=getResources().getDisplayMetrics().density,sp=getResources().getDisplayMetrics().scaledDensity,cx=getWidth()/2f,cy=getHeight()/2f;
        paint.setStyle(Paint.Style.FILL);paint.setColor(isPressed()?tone(AppTheme.PRESSED):PANEL);
        if(more)canvas.drawCircle(cx,cy,16*d,paint);else canvas.drawRoundRect(cx-22*d,cy-15*d,cx+22*d,cy+15*d,10*d,10*d,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d*.8f);paint.setColor(isFocused()?AMBER:tone(AppTheme.BORDER));
        if(more)canvas.drawCircle(cx,cy,16*d,paint);else canvas.drawRoundRect(cx-22*d,cy-15*d,cx+22*d,cy+15*d,10*d,10*d,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(more?CREAM:group==0?MUTED:AMBER);
        if(more){for(int i=-1;i<=1;i++)canvas.drawCircle(cx+i*5*d,cy,1.45f*d,paint);}
        else{paint.setTextSize(10*sp);paint.setTypeface(Typeface.create("sans-serif-medium",0));paint.setTextAlign(Paint.Align.CENTER);Paint.FontMetrics fm=paint.getFontMetrics();canvas.drawText(FilmGroups.SHORT_LABELS[group],cx-4*d,cy-(fm.ascent+fm.descent)/2,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.1f*d);paint.setStrokeCap(Paint.Cap.ROUND);canvas.drawLine(cx+11*d,cy-d,cx+14*d,cy+2*d,paint);canvas.drawLine(cx+14*d,cy+2*d,cx+17*d,cy-d,paint);}
    }
    @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.Button");info.setCanOpenPopup(true);}
}
