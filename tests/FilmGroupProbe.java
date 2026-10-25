package com.silverroom.camera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

/** Runs the shipping vertical View with the same controlled Android fixtures as the film reel. */
public final class FilmGroupProbe {
    private static final List<String> passed=new ArrayList<>();
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);passed.add(label);}
    private static final class Harness {
        final Context context=new Context();final FilmGroupWheel wheel=new FilmGroupWheel(context);
        final List<Integer> changes=new ArrayList<>();boolean favorites;int group,clicks;
        Harness(){wheel.size(48,64);wheel.listener=id->{group=id;changes.add(id);wheel.group(id,favorites);};wheel.setOnClickListener(v->clicks++);}
        void select(int id,boolean pins){favorites=pins;group=id;wheel.group(id,pins);}
        void event(int action,float x,float y){wheel.onTouchEvent(new MotionEvent(action,x,y));}
        void event(int action,float y){event(action,24,y);}
        void tap(){event(0,32);event(1,32);}
        void drag(float from,float to){event(0,from);event(2,to);event(1,to);ValueAnimator.finishAll();}
    }
    public static List<String> run(){
        Harness h=new Harness();h.event(0,32);h.event(2,0);
        check(h.group==FilmGroups.FUJI&&h.changes.equals(Arrays.asList(1))&&h.clicks==0,"Vertical category drag applies Fuji immediately before finger release without opening the menu");
        h.event(2,-24);h.event(2,-48);h.event(1,-48);ValueAnimator.finishAll();
        check(h.group==FilmGroups.RICOH&&h.changes.equals(Arrays.asList(1,2,3))&&h.clicks==0,"One continuous drag crosses Fuji, Leica and Ricoh while live refresh retains its pointer");
        h.drag(32,64);check(h.group==FilmGroups.LEICA&&h.changes.size()==4,"Downward category drag selects the preceding family");
        h=new Harness();h.tap();check(h.clicks==1&&h.changes.isEmpty(),"A stationary category tap retains the complete category menu");
        h.event(0,32);h.event(2,29);h.event(1,29);check(h.clicks==2&&h.changes.isEmpty(),"Finger jitter below Android touch slop remains a tap");
        h=new Harness();h.event(0,24,32);h.event(2,60,34);h.event(1,60,34);
        check(h.changes.isEmpty()&&h.clicks==0,"Horizontal movement on the category entry never changes families or opens the menu");
        h=new Harness();h.event(0,32);h.event(2,9);h.event(2,-7);h.event(1,-7);ValueAnimator.finishAll();
        check(h.changes.equals(Arrays.asList(1)),"Synchronous selection refresh preserves fractional drag distance instead of jumping to a second family");
        h=new Harness();h.event(0,32);h.event(2,19);h.event(1,19);ValueAnimator.half();h.tap();ValueAnimator.finishAll();
        check(h.clicks==0&&h.changes.isEmpty(),"Touching a partially settling category reel does not accidentally open the menu");
        h.tap();check(h.clicks==1,"A later stationary tap remains available after interrupted settling");
        h=new Harness();h.event(0,32);h.event(3,32);h.event(1,32);h.event(0,32);h.event(5,32);h.event(2,-64);h.event(1,-64);
        check(h.clicks==0&&h.changes.isEmpty(),"Cancellation and a second finger stop category selection without click-through");
        h=new Harness();h.select(FilmGroups.SPECIAL,false);h.event(0,32);h.event(2,-500);
        check(h.group==FilmGroups.SPECIAL&&h.changes.isEmpty(),"An empty favorites group is skipped at the scroll boundary instead of opening a modal");
        h.event(2,-476);h.event(1,-476);ValueAnimator.finishAll();
        check(h.group==FilmGroups.RETRO,"Reversing at the category boundary responds immediately without accumulated overscroll");
        h=new Harness();h.select(FilmGroups.SPECIAL,true);h.drag(32,0);
        check(h.group==FilmGroups.FAVORITES&&h.changes.equals(Arrays.asList(10)),"A populated favorites group participates in the vertical category reel");
        Harness added=new Harness();added.select(FilmGroups.RICOH,false);added.drag(32,0);
        check(added.group==FilmGroups.HASSELBLAD&&added.changes.equals(Arrays.asList(11)),"The production reel emits the stable Hasselblad ID rather than its new visual index");
        added.drag(32,0);added.wheel.performAccessibilityAction(8192,null);
        check(added.group==FilmGroups.HASSELBLAD&&added.changes.equals(Arrays.asList(11,4,11))&&added.wheel.description.contains("哈苏"),"Hasselblad and original families remain adjacent through touch and accessible scrolling without favorites");
        h.select(FilmGroups.ALL,false);h.drag(32,500);
        check(h.group==FilmGroups.ALL&&h.changes.size()==1,"The first category has a bounded endpoint and never wraps unexpectedly");
        h=new Harness();h.event(0,32);h.select(FilmGroups.RICOH,true);h.event(2,-64);h.event(1,-64);
        check(h.group==FilmGroups.RICOH&&h.changes.isEmpty()&&h.wheel.description.contains("理光"),"External category changes cancel stale gestures and synchronize the accessible label");
        h=new Harness();h.event(0,32);h.event(2,19);h.event(1,19);h.wheel.setEnabled(false);ValueAnimator.finishAll();h.tap();h.wheel.performClick();h.wheel.performAccessibilityAction(4096,null);
        check(h.changes.isEmpty()&&h.clicks==0,"Capture-state disabling cancels settling and rejects touch, menu and accessibility changes");
        h.wheel.setEnabled(true);h.drag(32,0);check(h.group==FilmGroups.FUJI,"Category selection resumes after capture-state controls are re-enabled");
        h=new Harness();h.event(0,32);h.event(2,19);h.event(1,19);h.wheel.onDetachedFromWindow();ValueAnimator.finishAll();h.event(1,19);
        check(h.changes.isEmpty()&&h.clicks==0,"Detaching the category reel cancels stale touch and animation work");
        h=new Harness();AccessibilityNodeInfo info=new AccessibilityNodeInfo();h.wheel.onInitializeAccessibilityNodeInfo(info);h.wheel.performAccessibilityAction(4096,null);h.wheel.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN,new KeyEvent());h.wheel.onKeyDown(KeyEvent.KEYCODE_DPAD_UP,new KeyEvent());h.wheel.onKeyDown(KeyEvent.KEYCODE_ENTER,new KeyEvent());
        check(info.actions.size()==2&&h.changes.equals(Arrays.asList(1,2,1))&&h.clicks==1,"TalkBack scroll and keyboard up/down select categories, while keyboard confirmation opens the menu");
        boolean fits=true;for(int theme=0;theme<2;theme++){AppTheme.select(theme);for(float font:new float[]{1,1.3f,2}){h=new Harness();h.context.resources.metrics.scaledDensity=font;for(int group=0;group<FilmGroups.LABELS.length;group++){h.select(group,true);Canvas canvas=new Canvas();h.wheel.onDraw(canvas);fits&=canvas.texts.contains(FilmGroups.SHORT_LABELS[group]);for(float width:canvas.widths)fits&=width<=40.01f;}}}
        check(fits,"All category labels fit the symmetric 48dp entry in both themes and enlarged text settings");
        return passed;
    }
}
