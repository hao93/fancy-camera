package com.silverroom.camera;

import android.content.Context;
import android.graphics.*;
import android.widget.ImageView;
import static com.silverroom.camera.Controls.*;

/** The thumbnail keeps its own neutral, theme-aware empty state. */
final class QuickPreview extends ImageView {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    QuickPreview(Context owner){super(owner);setScaleType(ScaleType.CENTER_CROP);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(getDrawable()!=null)return;float d=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f;p.setColor(MUTED);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.3f*d);canvas.drawRoundRect(cx-11*d,cy-9*d,cx+11*d,cy+9*d,3*d,3*d,p);Path hills=new Path();hills.moveTo(cx-8*d,cy+6*d);hills.lineTo(cx-2*d,cy-d);hills.lineTo(cx+2*d,cy+3*d);hills.lineTo(cx+5*d,cy);hills.lineTo(cx+9*d,cy+6*d);canvas.drawPath(hills,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(cx+5*d,cy-4*d,1.7f*d,p);}
}
