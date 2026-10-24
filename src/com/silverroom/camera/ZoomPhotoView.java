package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import static com.silverroom.camera.Controls.*;

final class ZoomPhotoView extends View {
    final PhotoViewport viewport=new PhotoViewport();private final PhotoBrowseGesture swipe=new PhotoBrowseGesture();
    private final Paint imagePaint=new Paint(3),label=new Paint(3);private final ScaleGestureDetector scale;private final GestureDetector taps;
    private Bitmap bitmap;private String placeholder="正在读取底片…";private float lastX,lastY,downX,downY,swipeX,swipeY,opacity=1;private int pointer=-1;private boolean tapBlocked;private ValueAnimator returning,appearing;Runnable delete,exit,confirmedTap;java.util.function.IntConsumer navigate;boolean previous,next;
    ZoomPhotoView(Context context){
        super(context);setFocusable(true);setContentDescription("底片照片，双指缩放，放大后拖动查看，原始比例下左右切换底片，下滑返回列表，上滑删除");
        scale=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScaleBegin(ScaleGestureDetector detector){tapBlocked=true;swipe.block();swipeX=swipeY=0;return true;}
            @Override public boolean onScale(ScaleGestureDetector detector){viewport.scale(viewport.zoom*detector.getScaleFactor(),detector.getFocusX(),detector.getFocusY());invalidate();return true;}
        });scale.setQuickScaleEnabled(false);
        taps=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(MotionEvent e){return true;}@Override public boolean onSingleTapConfirmed(MotionEvent e){if(bitmap!=null&&!tapBlocked&&!scale.isInProgress()&&(pointer<0||e.getActionMasked()==MotionEvent.ACTION_UP))performClick();return true;}@Override public boolean onDoubleTap(MotionEvent e){tapBlocked=true;swipe.block();swipeX=swipeY=0;viewport.scale(viewport.zoom>1.01f?1:3,e.getX(),e.getY());invalidate();return true;}});
    }
    void image(Bitmap value){tapBlocked=true;stopAnimations();bitmap=value;viewport.reset();swipe.block();swipeX=swipeY=0;opacity=1;resize();if(value!=null){appearing=ValueAnimator.ofFloat(.3f,1);appearing.setDuration(160);appearing.addUpdateListener(a->{opacity=(float)a.getAnimatedValue();invalidate();});appearing.start();}invalidate();}
    void loading(){image(null);placeholder="正在读取底片…";invalidate();}
    void error(){placeholder="无法读取照片";invalidate();}
    private void resize(){if(bitmap!=null)viewport.size(getWidth(),getHeight(),bitmap.getWidth(),bitmap.getHeight());}
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){resize();}
    private float threshold(){float d=getResources().getDisplayMetrics().density;return Math.max(96*d,Math.min(160*d,getHeight()*.2f));}
    private float horizontalThreshold(){float d=getResources().getDisplayMetrics().density;return Math.max(56*d,Math.min(96*d,getWidth()*.2f));}
    @Override protected void onDraw(Canvas c){
        c.drawColor(tone(AppTheme.IMAGE_BG));float d=getResources().getDisplayMetrics().density,sp=getResources().getDisplayMetrics().scaledDensity;
        if(bitmap==null){label.setColor(MUTED);label.setTextSize(13*sp);label.setTextAlign(Paint.Align.CENTER);c.drawText(placeholder,getWidth()/2f,getHeight()/2f,label);return;}
        float progress=Math.min(1,Math.abs(swipeY)/threshold());if(swipeY<0){label.setColor(tone(AppTheme.DANGER_BG));c.drawRect(0,getHeight()-Math.max(62*d,-swipeY),getWidth(),getHeight(),label);label.setColor(tone(AppTheme.DANGER));label.setTextAlign(Paint.Align.CENTER);label.setTextSize(13*sp);c.drawText(progress>=1?"↑ 松手删除":"↑ 继续上滑删除",getWidth()/2f,getHeight()-25*d,label);}else if(swipeY>0){label.setColor(MUTED);label.setTextAlign(Paint.Align.CENTER);label.setTextSize(12*sp);c.drawText(progress>=1?"↓ 松手返回底片列表":"↓ 下滑返回底片列表",getWidth()/2f,32*d,label);}
        if(swipeX!=0){label.setColor(MUTED);label.setTextAlign(Paint.Align.CENTER);label.setTextSize(12*sp);c.drawText(swipeX<0?(next?"下一张 →":"已是最后一张"):(previous?"← 上一张":"已是第一张"),getWidth()/2f,getHeight()-25*d,label);}
        c.save();c.translate(getWidth()/2f+viewport.x+swipeX,getHeight()/2f+viewport.y+swipeY);c.scale(viewport.fit*viewport.zoom,viewport.fit*viewport.zoom);imagePaint.setAlpha(Math.round(255*opacity*(1-.3f*progress)));c.drawBitmap(bitmap,-bitmap.getWidth()/2f,-bitmap.getHeight()/2f,imagePaint);c.restore();
        if(viewport.zoom>1.01f){String value=String.format(java.util.Locale.US,"%.1f×",viewport.zoom);label.setTypeface(Typeface.create("monospace",0));label.setTextSize(12*sp);Paint.FontMetrics fm=label.getFontMetrics();float width=label.measureText(value)+16*d,height=fm.descent-fm.ascent+16*d;label.setColor(AppTheme.OVERLAY_BG);c.drawRoundRect(10*d,10*d,10*d+width,10*d+height,8*d,8*d,label);label.setColor(AppTheme.OVERLAY_TEXT);label.setTextAlign(Paint.Align.CENTER);c.drawText(value,10*d+width/2,18*d-fm.ascent,label);}
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){tapBlocked=false;if(returning!=null)returning.cancel();swipeX=swipeY=0;pointer=e.getPointerId(0);downX=lastX=e.getX();downY=lastY=e.getY();swipe.begin(lastX,lastY,viewport.zoom);getParent().requestDisallowInterceptTouchEvent(true);}
        if(action==MotionEvent.ACTION_POINTER_DOWN){tapBlocked=true;swipe.block();swipeX=swipeY=0;}
        if(action==MotionEvent.ACTION_UP&&Math.hypot(e.getX()-downX,e.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())tapBlocked=true;
        if(bitmap!=null){scale.onTouchEvent(e);taps.onTouchEvent(e);}
        if(action==MotionEvent.ACTION_MOVE){int i=e.findPointerIndex(pointer);if(i>=0){float x=e.getX(i),y=e.getY(i);if(Math.hypot(x-downX,y-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())tapBlocked=true;if(!scale.isInProgress()){if(viewport.zoom>1.01f)viewport.pan(x-lastX,y-lastY);else{float[] offset=swipe.drag(x,y,ViewConfiguration.get(getContext()).getScaledTouchSlop());swipeX=offset[0];swipeY=offset[1];if((swipeX<0&&!next)||(swipeX>0&&!previous))swipeX*=.22f;}}lastX=x;lastY=y;invalidate();}}
        else if(action==MotionEvent.ACTION_POINTER_UP){int removed=e.getActionIndex();if(e.getPointerId(removed)==pointer){int replacement=removed==0?1:0;pointer=e.getPointerId(replacement);lastX=e.getX(replacement);lastY=e.getY(replacement);}}
        else if(action==MotionEvent.ACTION_UP){PhotoBrowseGesture.Action result=swipe.finish(e.getX(),e.getY(),horizontalThreshold(),threshold(),ViewConfiguration.get(getContext()).getScaledTouchSlop());if(result!=PhotoBrowseGesture.Action.NONE)tapBlocked=true;pointer=-1;getParent().requestDisallowInterceptTouchEvent(false);if(result==PhotoBrowseGesture.Action.NEXT||result==PhotoBrowseGesture.Action.PREVIOUS){if(navigate!=null)navigate.accept(result==PhotoBrowseGesture.Action.NEXT?1:-1);}else if(result==PhotoBrowseGesture.Action.CLOSE&&exit!=null){exit.run();return true;}else if(result==PhotoBrowseGesture.Action.DELETE&&delete!=null){performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);delete.run();}restoreSwipe();}
        else if(action==MotionEvent.ACTION_CANCEL){tapBlocked=true;swipe.block();pointer=-1;restoreSwipe();getParent().requestDisallowInterceptTouchEvent(false);}
        return true;
    }
    private void restoreSwipe(){if(swipeX==0&&swipeY==0)return;final float x=swipeX,y=swipeY;returning=ValueAnimator.ofFloat(1,0);returning.setDuration(180);returning.addUpdateListener(a->{float v=(float)a.getAnimatedValue();swipeX=x*v;swipeY=y*v;invalidate();});returning.start();}
    private void stopAnimations(){if(returning!=null)returning.cancel();if(appearing!=null)appearing.cancel();}
    @Override public boolean performClick(){super.performClick();if(bitmap!=null&&confirmedTap!=null)confirmedTap.run();return true;}
    @Override protected void onDetachedFromWindow(){tapBlocked=true;stopAnimations();super.onDetachedFromWindow();}
}
