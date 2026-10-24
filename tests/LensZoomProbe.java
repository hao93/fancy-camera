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
        Harness(){VelocityTracker.speed=0;wheel.size(336,52);wheel.availability(new boolean[]{true,true,true,true,true,true,true},new String[]{"光学","光学","裁切","裁切","光学","裁切","裁切"},1,false);wheel.zoomed=values::add;}
        void event(int action,float x){wheel.onTouchEvent(new MotionEvent(action,x,26));}
        void drag(float slots){event(0,168);event(2,168-8-slots*72);event(1,168-8-slots*72);ValueAnimator.finishAll();}
        float last(){return values.get(values.size()-1);}
    }
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
        return passed;
    }
}
