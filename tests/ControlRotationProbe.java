package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import java.util.*;

public final class ControlRotationProbe {
    private static final List<String> passed=new ArrayList<>();
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);passed.add(name);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.01f;}
    private static boolean sameAngle(float a,float b){return near(((a-b)%360+540)%360-180,0);}
    public static List<String> run(){
        check(near(ControlRotation.destination(0,90),-90)&&near(ControlRotation.destination(0,270),90),"Left and right landscape poses counter-rotate icons in the correct direction");
        check(sameAngle(ControlRotation.destination(0,180),180)&&near(ControlRotation.destination(0,0),0),"Portrait and upside-down poses use the expected drawing orientation");
        check(near(ControlRotation.destination(350,0),360)&&near(ControlRotation.destination(-350,0),-360),"Rotation crosses the 0/360 boundary along the short path");
        check(CaptureOrientation.update(-1,90)==90&&CaptureOrientation.update(45,0)==0&&CaptureOrientation.update(50,90)==90&&CaptureOrientation.update(60,0)==90,"Existing pose hysteresis rejects diagonal jitter and keeps the last orientation when flat");
        final List<Float> frames=new ArrayList<>();ControlRotation rotation=new ControlRotation(frames::add);
        rotation.turn(90,true);ValueAnimator.half();check(frames.size()==1&&near(frames.get(0),-45),"Icon orientation animates through an intermediate angle rather than rotating the entire layout");
        rotation.turn(270,true);ValueAnimator.finishAll();check(sameAngle(frames.get(frames.size()-1),90),"A rapid direction change cancels the previous animation and reaches the latest pose");
        int before=frames.size();rotation.turn(270,true);rotation.turn(-1,true);check(frames.size()==before,"Repeated or unknown orientation events do not restart animation");
        rotation.turn(180,true);rotation.finish();before=frames.size();ValueAnimator.finishAll();check(frames.size()==before&&sameAngle(frames.get(before-1),180),"Pause teardown cancels pending frame callbacks and settles the current target");
        rotation.turn(0,false);check(sameAngle(frames.get(frames.size()-1),0),"Returning to the finder can synchronize orientation without a stale animation");
        boolean[] available={true,true,true,true,true,true,true};String[] labels={"光学","光学","裁切","裁切","光学","裁切","裁切"};
        LensWheel lens=new LensWheel(new Context());lens.size(336,UiSpec.lensHeight(1));lens.availability(available,labels,1,false);final int[] changes={0};lens.listener=id->changes[0]++;
        lens.numberAngle(-90);Canvas landscape=new Canvas();lens.onDraw(landscape);boolean numbers=true,chinese=true;
        for(int i=0;i<landscape.texts.size();i++){String text=landscape.texts.get(i);if(text.matches("[0-9]+"))numbers&=sameAngle(landscape.textAngles.get(i),-90);else chinese&=near(landscape.textAngles.get(i),0);}
        check(numbers&&chinese&&changes[0]==0,"Shipping lens renderer rotates focal numbers while Chinese source labels stay portrait and selection stays unchanged");
        lens.onTouchEvent(new android.view.MotionEvent(0,lens.getWidth()/2f+72,26));lens.onTouchEvent(new android.view.MotionEvent(1,lens.getWidth()/2f+72,26));ValueAnimator.finishAll();
        check(changes[0]==1&&lens.state.selected()==2,"Rotated focal numbers retain the original horizontal tap coordinates and optical selection");
        boolean fits=true;for(float font:new float[]{1,1.3f,2})for(float angle:new float[]{0,45,90,135,180,270})for(int mm:LensWheel.MM){
            Context context=new Context();context.resources.metrics.scaledDensity=font;lens=new LensWheel(context);lens.size(336,UiSpec.lensHeight(font));lens.numberAngle(angle);
            Paint paint=new Paint(3);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(18*font);Canvas canvas=new Canvas();lens.drawTitle(canvas,""+mm,-1,paint,lens.pitch());float[] b=canvas.textBounds.get(0);
            float bottom=lens.getHeight()/2f-12*font*1.35f-10,top=-lens.getHeight()/2f+8;fits&=b[0]>=-(lens.pitch()-16)/2-.01f&&b[2]<=(lens.pitch()-16)/2+.01f&&b[1]>=top-.01f&&b[3]<=bottom+.01f&&near(paint.getTextSize(),18*font);
        }
        check(fits,"All seven focal lengths fit their isolated numeric region at portrait, landscape and animation angles with enlarged fonts");
        FilmWheel film=new FilmWheel(new Context());Canvas filmCanvas=new Canvas();film.onDraw(filmCanvas);
        check(filmCanvas.textAngles.stream().allMatch(v->near(v,0)),"Film names, percentages and Chinese descriptions retain their unrotated film renderer");
        boolean toolIcons=true,badges=true,backgrounds=true;for(int theme=0;theme<2;theme++){AppTheme.select(theme);for(int kind=0;kind<4;kind++)for(float angle:new float[]{-90,90,180}){
            Controls.ToolButton tool=new Controls.ToolButton(new Context(),kind);tool.size(76,48);tool.state(true,kind==3?"铺满":kind==2?"10s":kind==0?"A":"", "工具");tool.iconAngle(angle);Canvas draw=new Canvas();tool.onDraw(draw);
            backgrounds&=near(draw.shapeAngles.get(0),0);for(int i=1;i<draw.shapeAngles.size();i++)toolIcons&=sameAngle(draw.shapeAngles.get(i),angle);for(float path:draw.pathAngles)toolIcons&=sameAngle(path,angle);for(float text:draw.textAngles)badges&=near(text,0);
        }}
        check(toolIcons&&backgrounds,"Shipping flash, composition, timer and frame glyphs rotate inside stationary button backgrounds in both themes");
        check(badges,"The Chinese frame badge and tool status text remain portrait after the icon drawing transform is restored");
        boolean evBounds=true,captureSpace=true;for(float font:new float[]{1,1.3f,2})for(float ev:new float[]{-2,0,2}){Context context=new Context();context.resources.metrics.scaledDensity=font;Controls.ExposureButton button=new Controls.ExposureButton(context);button.size(86,96);button.set(ev);Canvas canvas=new Canvas();button.onDraw(canvas);for(float[] box:canvas.textBounds)evBounds&=box[0]>=0&&box[1]>=0&&box[2]<=86&&box[3]<=96;int height=UiSpec.captureHeight(font);captureSpace&=(height-60)/2f>=18*font;}check(evBounds,"Exposure digits and EV caption stay within their fixed centered button at 100/130/200 percent fonts");check(captureSpace,"Capture-row height reserves non-overlapping space below the centered histogram for enlarged frame counts");
        return passed;
    }
}
