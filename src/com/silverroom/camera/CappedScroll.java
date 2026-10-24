package com.silverroom.camera;
import android.content.Context;
import android.widget.ScrollView;

/** Shrinks to collapsed content, with a 280dp ceiling for advanced darkroom controls. */
final class CappedScroll extends ScrollView {
    CappedScroll(Context c){super(c);setFillViewport(false);}
    @Override protected void onMeasure(int w,int h){int cap=Math.round(280*getResources().getDisplayMetrics().density);if(MeasureSpec.getMode(h)!=MeasureSpec.UNSPECIFIED)cap=Math.min(cap,MeasureSpec.getSize(h));super.onMeasure(w,MeasureSpec.makeMeasureSpec(cap,MeasureSpec.AT_MOST));}
}
