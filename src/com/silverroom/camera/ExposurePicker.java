package com.silverroom.camera;

import android.app.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import static com.silverroom.camera.Controls.*;

/** Exposure changes stay live; Done, Back and outside taps all keep the chosen value. */
final class ExposurePicker extends ThemedDialog {
    final Activity owner;final Change change;float value;SeekBar slider;TextView number;
    ExposurePicker(Activity a,float ev,Change listener){super(a);owner=a;value=ExposureValue.clamp(ev);change=listener;}
    int dp(float n){return Math.round(n*owner.getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color){TextView t=new TextView(owner);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER);return t;}
    GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    void choose(float ev){float next=ExposureValue.clamp(ev);if(next!=value){value=next;change.value(value);number.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);}number.setText(ExposureValue.label(value));slider.setProgress(ExposureValue.progress(value));}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);setCanceledOnTouchOutside(true);LinearLayout root=new LinearLayout(owner);root.setOrientation(1);root.setPadding(dp(20),dp(14),dp(20),dp(22));root.setBackground(bg(INK,20));
        LinearLayout header=new LinearLayout(owner);TextView title=text("曝光补偿",20,CREAM);title.setGravity(Gravity.CENTER_VERTICAL);header.addView(title,new LinearLayout.LayoutParams(0,dp(44),1));TextView done=text("完成",14,AMBER);header.addView(done,new LinearLayout.LayoutParams(dp(52),dp(44)));done.setOnClickListener(v->dismiss());root.addView(header);
        number=text(ExposureValue.label(value),38,CREAM);number.setTypeface(Typeface.MONOSPACE);root.addView(number,new LinearLayout.LayoutParams(-1,dp(58)));root.addView(text("EV · 向左压暗，向右提亮",11,MUTED),new LinearLayout.LayoutParams(-1,dp(28)));
        LinearLayout controls=new LinearLayout(owner);controls.setGravity(Gravity.CENTER_VERTICAL);TextView minus=text("−",25,CREAM),plus=text("+",25,CREAM);minus.setContentDescription("减少0.1EV");plus.setContentDescription("增加0.1EV");controls.addView(minus,new LinearLayout.LayoutParams(dp(44),dp(48)));slider=new SeekBar(owner);slider.setMax(40);slider.setProgress(ExposureValue.progress(value));slider.setProgressTintList(ColorStateList.valueOf(AMBER));slider.setThumbTintList(ColorStateList.valueOf(CREAM));slider.setContentDescription("曝光补偿，范围负2到正2EV");controls.addView(slider,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(plus,new LinearLayout.LayoutParams(dp(44),dp(48)));root.addView(controls);minus.setOnClickListener(v->choose(value-.1f));plus.setOnClickListener(v->choose(value+.1f));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(user)choose(ExposureValue.value(p));}});
        LinearLayout footer=new LinearLayout(owner);footer.setGravity(Gravity.CENTER_VERTICAL);TextView low=text("−2 EV",10,MUTED);footer.addView(low,new LinearLayout.LayoutParams(0,dp(44),1));TextView reset=text("重置为 0",12,AMBER);reset.setBackground(bg(PANEL,12));footer.addView(reset,new LinearLayout.LayoutParams(dp(110),dp(44)));reset.setOnClickListener(v->choose(0));TextView high=text("+2 EV",10,MUTED);footer.addView(high,new LinearLayout.LayoutParams(0,dp(44),1));root.addView(footer);
        setContentView(root);getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().setGravity(Gravity.BOTTOM);getWindow().setLayout(-1,-2);
    }
}
