package com.silverroom.camera;
import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.view.View;

/** Width determines the preview square; caption height never stretches its image region. */
final class SquarePreview extends FrameLayout {
    final ImageView image;
    SquarePreview(Context context){super(context);image=new ImageView(context);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);addView(image,new FrameLayout.LayoutParams(-1,-1));}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int edge=MeasureSpec.getSize(widthSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(edge,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(edge,MeasureSpec.EXACTLY));
    }
}
