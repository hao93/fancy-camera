package com.silverroom.camera;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import static com.silverroom.camera.Controls.*;

/** A compact frame library with miniature format silhouettes and one-tap selection. */
final class FramePicker extends Dialog {
    interface Listener {void choose(int id);}
    final Activity owner;final int selected;final Listener listener;
    FramePicker(Activity a,int id,Listener l){super(a);owner=a;selected=id;listener=l;}
    int dp(float v){return Math.round(v*owner.getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color){TextView t=new TextView(owner);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    GradientDrawable bg(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root=new LinearLayout(owner);root.setOrientation(1);root.setPadding(dp(18),dp(10),dp(18),dp(18));root.setBackground(bg(INK,0));
        View handle=new View(owner);handle.setBackground(bg(0xff50574c,0));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(32),dp(3));hp.gravity=Gravity.CENTER;hp.bottomMargin=dp(10);root.addView(handle,hp);
        LinearLayout head=new LinearLayout(owner);head.setGravity(Gravity.CENTER_VERTICAL);head.addView(text("选择画幅",21,CREAM),new LinearLayout.LayoutParams(0,dp(44),1));TextView close=text("完成",13,AMBER);close.setGravity(Gravity.CENTER);head.addView(close,new LinearLayout.LayoutParams(dp(52),dp(44)));close.setOnClickListener(v->dismiss());root.addView(head);
        TextView subtitle=text("取景框与成片同步 · 横拍自动交换长宽",11,MUTED);root.addView(subtitle,new LinearLayout.LayoutParams(-1,dp(28)));
        ScrollView scroll=new ScrollView(owner);scroll.setFillViewport(false);LinearLayout list=new LinearLayout(owner);list.setOrientation(1);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        for(int i=0;i<FrameFormat.LABELS.length;i++){
            final int id=i;boolean on=i==selected;LinearLayout card=new LinearLayout(owner);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(12),0,dp(12),0);card.setBackground(bg(on?0xff302b20:PANEL,on?AMBER:0xff343b33));
            View icon=new View(owner){final Paint p=new Paint(3);@Override protected void onDraw(Canvas c){float a=id==0?.66f:FrameFormat.aspect(id,1),h=dp(31),w=h*a;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.2f));p.setColor(on?AMBER:MUTED);float x=(getWidth()-w)/2,y=(getHeight()-h)/2;c.drawRoundRect(x,y,x+w,y+h,dp(2),dp(2),p);p.setAlpha(70);c.drawLine(x,y+h/3,x+w,y+h/3,p);c.drawLine(x+w/3,y,x+w/3,y+h,p);p.setAlpha(255);}};
            card.addView(icon,new LinearLayout.LayoutParams(dp(42),-1));LinearLayout words=new LinearLayout(owner);words.setOrientation(1);words.setGravity(Gravity.CENTER_VERTICAL);TextView name=text(FrameFormat.LABELS[i]+"  ·  "+FrameFormat.NAMES[i],14,on?AMBER:CREAM);words.addView(name,new LinearLayout.LayoutParams(-1,dp(25)));TextView note=text(FrameFormat.NOTES[i],10,MUTED);words.addView(note,new LinearLayout.LayoutParams(-1,dp(20)));card.addView(words,new LinearLayout.LayoutParams(0,-1,1));TextView check=text(on?"●":"",14,AMBER);card.addView(check,new LinearLayout.LayoutParams(dp(18),-1));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(66));cp.bottomMargin=dp(8);list.addView(card,cp);card.setFocusable(true);card.setContentDescription(FrameFormat.LABELS[i]+"，"+FrameFormat.NAMES[i]+(on?"，已选中":""));card.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);listener.choose(id);dismiss();});
        }
        TextView note=text("原始底片留存；所选画幅用于保存成片。",11,MUTED);root.addView(note,new LinearLayout.LayoutParams(-1,dp(28)));
        setContentView(root);Window window=getWindow();window.setBackgroundDrawableResource(android.R.color.transparent);window.setGravity(Gravity.BOTTOM);window.setLayout(-1,Math.min(dp(540),Math.round(owner.getResources().getDisplayMetrics().heightPixels*.82f)));
    }
}
