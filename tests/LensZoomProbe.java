package com.silverroom.camera;
import android.content.Context;
import android.view.*;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import java.util.*;

public final class LensZoomProbe {
    static final List<String> passed=new ArrayList<>();static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);passed.add(label);}
    static class Harness {
        LensWheel wheel=new LensWheel(new Context());List<Float> values=new ArrayList<>();
        Harness(){VelocityTracker.speed=0;wheel.size(336,UiSpec.lensHeight(1));wheel.availability(new boolean[]{true,true,true,true,true,true,true},new String[]{"光学","光学","裁切","裁切","光学","裁切","裁切"},1,false);wheel.zoomed=values::add;}
        void event(int action,float x){wheel.onTouchEvent(new MotionEvent(action,x,26));}
        void drag(float slots){event(0,168);event(2,168-8-slots*72);event(1,168-8-slots*72);ValueAnimator.finishAll();}
        float last(){return values.get(values.size()-1);}
    }
    static Canvas draw(Harness h){Canvas c=new Canvas();h.wheel.onDraw(c);return c;}
    static boolean millimetres(Canvas c){return c.texts.stream().anyMatch(t->t.endsWith("mm"));}
    public static List<String> run(){
        boolean[] all={true,true,true,true,true,true,true},main={false,true,true,true,false,false,false};
        boolean monotonic=true,inverse=true;float previous=0;for(int i=0;i<=600;i++){float p=i/100f,mm=ZoomScale.focal(p);monotonic&=mm>=previous;previous=mm;inverse&=Math.abs(ZoomScale.position(mm)-p)<.009;}
        check(monotonic&&inverse,"Continuous zoom maps every fractional wheel position monotonically and returns the same position within 0.1mm precision");
        check(LensPlan.base(22.9f)==14&&LensPlan.base(23)==23&&LensPlan.base(84.9f)==23&&LensPlan.base(85)==85&&LensPlan.base(170.5f)==85,"Continuous focal ranges keep ultrawide, main and tele optical bases at their real boundaries");
        check(ZoomScale.snap(2.12f,all)==2&&Math.abs(ZoomScale.snap(2.5f,all)-2.5f)<.001,"Release near 35mm snaps while a middle focal remains continuously selectable");
        check(ZoomScale.focal(ZoomScale.constrain(6,main))==84.9f&&ZoomScale.focal(ZoomScale.constrain(0,main))==23,"Missing auxiliary lenses constrain the continuous range without pretending main-camera crop is telephoto");
        check(Float.isNaN(ZoomScale.constrain(3,new boolean[7]))&&ZoomScale.sessionFocal(23)==84.9f&&ZoomScale.sessionFocal(85)==230,"Missing all routes stays unavailable and session detail covers the whole optical group");
        Harness h=new Harness();h.drag(1.5f);check(Math.abs(h.last()-41.8f)<.05&&Math.abs(h.wheel.state.position()-2.5f)<.001,"Shipping lens touch handlers retain an intermediate 41.8mm after release instead of forcing 35 or 50");
        h=new Harness();h.drag(1.12f);check(h.last()==35&&h.values.stream().anyMatch(v->v>35),"Near-anchor release applies intermediate live values then magnetically settles to 35mm");
        h=new Harness();h.event(0,168);h.event(2,168-8-36);check(h.values.size()==1&&h.last()>23&&h.last()<35,"Live zoom changes before finger release even without crossing a preset focal length");
        int count=h.values.size();h.wheel.focal(85);check(h.values.size()==count&&h.wheel.state.position()<2,"Camera UI refresh cannot reset a finger's active continuous drag");h.event(3,124);h.event(2,50);h.event(1,50);check(h.values.size()==count,"Cancelled lens gesture cannot dispatch late zoom events");
        h=new Harness();h.event(0,168);h.event(2,168-8-80);h.wheel.setEnabled(false);count=h.values.size();ValueAnimator.finishAll();h.event(2,0);h.event(1,0);check(h.values.size()==count,"Disabling the focal reel during capture stops dragging without a new snap request");
        h=new Harness();h.wheel.availability(main,new String[7],1,false);h.drag(20);check(h.last()==84.9f&&LensPlan.base(h.last())==23,"A long fling on a main-only phone stops below the unavailable 85mm optical boundary");
        h=new Harness();h.wheel.focal(135);h.wheel.tapped(6);check(h.values.equals(Arrays.asList(230f)),"Clicking 230mm jumps directly without opening every intermediate route");
        h=new Harness();h.event(0,168);h.event(5,168);h.event(2,0);h.event(1,0);check(h.values.isEmpty(),"A second finger cancels focal scrolling without selecting another lens");
        h=new Harness();h.drag(1.5f);Canvas canvas=new Canvas();h.wheel.onDraw(canvas);check(canvas.texts.contains("41.8")&&h.wheel.description.contains("41.8"),"The central readout and accessible label announce the actual intermediate focal length");
        h=new Harness();check(draw(h).texts.stream().allMatch(t->t.matches("[0-9]+(?:\\.[0-9])?")),"Idle focal reel draws only numbers without source, magnification or heading captions");
        h.event(0,168);check(!millimetres(draw(h)),"Holding a stationary finger does not expose the scrolling caption");h.event(2,168-8-108);Canvas during=draw(h);check(during.texts.contains("41.8mm")&&during.texts.stream().filter(t->t.endsWith("mm")).count()==1&&during.texts.stream().noneMatch(t->t.contains("光学")||t.contains("裁切")||t.contains("主摄")),"Dragging shows one exact current millimetre caption without optical-source text");h.event(3,52);check(!millimetres(draw(h)),"Cancelling an active drag immediately removes its millimetre caption");
        h=new Harness();h.event(0,168);h.event(2,168-8-72*1.12f);h.event(1,168-8-72*1.12f);boolean settling=millimetres(draw(h));ValueAnimator.finishAll();boolean held=millimetres(draw(h));h.wheel.fireHeld();ValueAnimator.finishAll();check(settling&&held&&!millimetres(draw(h)),"Snap settling retains the caption through the hold interval before its delayed fade ends");
        h=new Harness();h.wheel.tapped(6);boolean quiet=!millimetres(draw(h));h.event(0,168);h.event(2,100);h.event(5,100);quiet&=!millimetres(draw(h));h.event(0,168);h.event(2,100);h.wheel.setEnabled(false);ValueAnimator.finishAll();quiet&=!millimetres(draw(h));check(quiet,"Direct focal taps, multi-finger cancellation and capture disabling leave the reel caption hidden");
        h=new Harness();Canvas idle=draw(h);float[] frame=idle.roundRects.get(0),digits=idle.textBounds.get(idle.texts.indexOf("23"));float idleHeight=frame[3]-frame[1];check(idleHeight<=32&&Math.abs(frame[3]-digits[3]-4)<.01&&Math.abs(digits[1]-frame[1]-4)<.01,"Idle selection frame hugs the number with four-dp top/bottom padding and no caption-sized blank area");
        h.event(0,168);h.event(2,168-8-108);draw(h);ValueAnimator.half();Canvas halfway=draw(h);ValueAnimator.finishAll();Canvas expanded=draw(h);float halfHeight=halfway.roundRects.get(0)[3]-halfway.roundRects.get(0)[1],expandedHeight=expanded.roundRects.get(0)[3]-expanded.roundRects.get(0)[1];float expandedDigits=expanded.textBounds.get(expanded.texts.indexOf("41.8"))[3]-expanded.textBounds.get(expanded.texts.indexOf("41.8"))[1];check(halfHeight>idleHeight&&expandedHeight>halfHeight&&expandedDigits>digits[3]-digits[1],"Scrolling smoothly expands the frame and digits rather than starting with an enlarged selection");
        h.event(3,52);Canvas shrinking=draw(h);ValueAnimator.finishAll();Canvas restored=draw(h);float[] closed=restored.roundRects.get(0);check(!millimetres(shrinking)&&Math.abs(closed[3]-closed[1]-idleHeight)<.01,"Cancelling hides the readout immediately and restores compact frame height after shrinking");
        boolean enclosed=true;for(float font:new float[]{1,1.3f,2})for(float angle:new float[]{0,45,90,180,270}){Context context=new Context();context.resources.metrics.scaledDensity=font;LensWheel reel=new LensWheel(context);reel.size(336,UiSpec.lensHeight(font));reel.availability(all,new String[7],1,false);reel.focal(229.9f);reel.numberAngle(angle);Canvas rendered=new Canvas();reel.onDraw(rendered);float[] box=rendered.roundRects.get(0),bounds=rendered.textBounds.get(rendered.texts.indexOf("229.9"));enclosed&=bounds[0]>=box[0]&&bounds[2]<=box[2]&&bounds[1]>=box[1]&&bounds[3]<=box[3]&&box[1]>=0&&box[3]<=reel.getHeight();}check(enclosed,"Compact frames contain rotated decimal focal numbers at normal and enlarged font scales without clipping");
        h=new Harness();h.event(0,168);h.event(2,120);ValueAnimator.finishAll();Canvas locked=draw(h);float[] fixedFrame=locked.roundRects.get(0);float captionY=locked.textBounds.get(locked.texts.size()-1)[3];boolean stable=true;for(float x:new float[]{160,20,-100,-260,-180,80,140}){h.event(2,x);Canvas sample=draw(h);float[] gate=sample.roundRects.get(0),cap=sample.textBounds.get(sample.texts.size()-1);stable&=Math.abs(gate[2]-gate[0]-(fixedFrame[2]-fixedFrame[0]))<.02&&Math.abs(gate[3]-gate[1]-(fixedFrame[3]-fixedFrame[1]))<.02&&Math.abs(cap[3]-captionY)<.02&&sample.textAlphas.get(sample.textAlphas.size()-1)==255;}check(stable,"Changing digit counts during a continuous drag keeps caption baseline, font height, opacity and expanded gate size stable");
        h.event(1,140);ValueAnimator.finishAll();check(millimetres(draw(h))&&h.wheel.callbackDelays.get(h.wheel.callbackDelays.size()-1)==900,"Finger release and completed settling schedule a 900ms readable hold instead of hiding immediately");
        h.wheel.fireHeld();ValueAnimator.half();Canvas fading=draw(h);int alpha=fading.textAlphas.get(fading.textAlphas.size()-1);float[] fadingBox=fading.roundRects.get(0);check(alpha>0&&alpha<255&&Math.abs(fadingBox[3]-fadingBox[1]-(fixedFrame[3]-fixedFrame[1]))<.02,"Delayed dismissal fades caption opacity while keeping its geometry fixed");
        h.event(0,168);h.event(2,100);ValueAnimator.finishAll();Canvas continued=draw(h);check(millimetres(continued)&&continued.textAlphas.get(continued.textAlphas.size()-1)==255,"Resuming a drag cancels an old fade without a late callback shrinking or hiding the caption");
        h.wheel.finish();ValueAnimator.finishAll();check(!millimetres(draw(h))&&h.wheel.callbacks.isEmpty(),"Leaving the reel clears delayed caption work and resets emphasis for the next visit");
        return passed;
    }
}
