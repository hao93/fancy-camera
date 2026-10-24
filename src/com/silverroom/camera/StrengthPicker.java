package com.silverroom.camera;
import android.app.*;
import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import static com.silverroom.camera.Controls.*;

final class StrengthPicker extends ThemedDialog {
    interface Change{void value(float value);}
    private final Activity owner;private final Change change;private final String film;private float value;private TextView number;private SeekBar slider;
    StrengthPicker(Activity a,String name,float strength,Change c){super(a);owner=a;film=name;value=FilmStrength.clamp(strength);change=c;}
    int dp(int n){return Math.round(n*owner.getResources().getDisplayMetrics().density);}
    int target(){return UiSpec.toolHeight(owner.getResources().getConfiguration().fontScale);}
    TextView text(String s,int size,int color){TextView t=new TextView(owner);t.setText(s);t.setTextSize(Math.max(UiSpec.CAPTION,size));t.setFontFeatureSettings("tnum");t.setTextColor(color);t.setGravity(Gravity.CENTER);return t;}
    void choose(int p){float next=Math.max(0,Math.min(100,p))/100f;if(next!=value){value=next;change.value(value);}number.setText(FilmStrength.percent(value)+"%");slider.setProgress(FilmStrength.percent(value));}
    @Override protected void onCreate(Bundle b){super.onCreate(b);requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout root=new LinearLayout(owner);root.setOrientation(1);root.setPadding(dp(20),dp(14),dp(20),dp(24));GradientDrawable bg=new GradientDrawable();bg.setColor(INK);bg.setCornerRadius(dp(UiSpec.RADIUS));root.setBackground(bg);
        LinearLayout head=new LinearLayout(owner);TextView title=text("胶片强度",20,CREAM);title.setGravity(Gravity.CENTER_VERTICAL);head.addView(title,new LinearLayout.LayoutParams(0,dp(target()),1));TextView done=text("完成",14,AMBER);head.addView(done,new LinearLayout.LayoutParams(dp(60),dp(target())));done.setOnClickListener(v->dismiss());root.addView(head);TextView name=text(film,12,MUTED);root.addView(name,new LinearLayout.LayoutParams(-1,-2));name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);number=text(FilmStrength.percent(value)+"%",38,CREAM);number.setTypeface(android.graphics.Typeface.MONOSPACE);root.addView(number,new LinearLayout.LayoutParams(-1,dp(Math.round(64*Math.max(1,owner.getResources().getConfiguration().fontScale)))));slider=new SeekBar(owner);slider.setMax(100);slider.setProgress(FilmStrength.percent(value));slider.setProgressTintList(ColorStateList.valueOf(AMBER));slider.setThumbTintList(ColorStateList.valueOf(CREAM));slider.setContentDescription("胶片强度，0至100百分比");root.addView(slider,new LinearLayout.LayoutParams(-1,dp(target())));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(user)choose(p);}});
        LinearLayout presets=new LinearLayout(owner);for(int p:new int[]{0,25,50,75,100}){TextView t=text(p==0?"原色":p+"%",12,p==100?AMBER:CREAM);presets.addView(t,new LinearLayout.LayoutParams(0,dp(target()),1));t.setOnClickListener(v->choose(p));}root.addView(presets);root.addView(text("实时生效 · 曝光保持独立",11,MUTED),new LinearLayout.LayoutParams(-1,-2));setContentView(root);getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().setGravity(Gravity.BOTTOM);getWindow().setLayout(-1,-2);
    }
}
