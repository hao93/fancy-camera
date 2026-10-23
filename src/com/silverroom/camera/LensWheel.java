package com.silverroom.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.Arrays;

/** Uses the same reel interaction as films, with optical availability gating. */
final class LensWheel extends FilmWheel {
    static final int[] MM={14,23,35,50,85,135,230};
    private boolean[] enabled=new boolean[7];private String[] labels={"检测中","检测中","检测中","检测中","检测中","检测中","检测中"};
    interface Blocked {void lens(int mm);}
    Blocked blocked;
    private float numberAngle;
    void numberAngle(float value){numberAngle=value;invalidate();}
    LensWheel(Context c){super(c);films(Arrays.asList(0,1,2,3,4,5,6),1);}
    void availability(boolean[] next,String[] text,int actual,boolean front){
        enabled=next.clone();labels=text.clone();setEnabled(!front);if(!moving()&&!available(state.selected())&&actual>=0&&enabled[actual])films(Arrays.asList(0,1,2,3,4,5,6),actual);invalidate();
    }
    @Override protected float pitch(){return 72*getResources().getDisplayMetrics().density;}
    @Override String title(int id){return MM[id]+"";}
    @Override void drawTitle(Canvas canvas,String text,float baseline,Paint brush,float slot){
        float d=getResources().getDisplayMetrics().density,original=brush.getTextSize();
        brush.setTextSize(9*getResources().getDisplayMetrics().scaledDensity);Paint.FontMetrics subtitle=brush.getFontMetrics();brush.setTextSize(original);Paint.FontMetrics fm=brush.getFontMetrics();
        float top=-23*d,bottom=Math.max(-17*d,Math.min(8*d,14*d+subtitle.ascent-2*d)),center=(top+bottom)/2;
        // Reserve a separate number region above the stationary lens-source subtitle.
        double radians=Math.toRadians(numberAngle);float sine=(float)Math.abs(Math.sin(radians)),cosine=(float)Math.abs(Math.cos(radians));
        float w=brush.measureText(text),h=fm.descent-fm.ascent;
        float scale=Math.min(1,Math.min((slot-16*d)/Math.max(1,w*cosine+h*sine),(bottom-top)/Math.max(1,w*sine+h*cosine)));
        brush.setTextSize(original*scale);fm=brush.getFontMetrics();
        canvas.save();canvas.rotate(numberAngle,0,center);canvas.drawText(text,0,center-(fm.ascent+fm.descent)/2,brush);canvas.restore();brush.setTextSize(original);
    }
    @Override String subtitle(int id){return labels[id];}
    @Override boolean available(int id){return enabled[id];}
    @Override String category(){return "焦段";}
    @Override boolean compact(){return true;}
    @Override int legalTarget(int index){return LensWheelPolicy.nearest(index,enabled);}
    @Override void tapped(int target){if(!available(target)){if(blocked!=null)blocked.lens(MM[target]);return;}super.tapped(target);}
}
