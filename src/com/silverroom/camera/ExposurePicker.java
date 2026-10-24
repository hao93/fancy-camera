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
    final Activity owner;final Change change;final ExposureLimits limits;float value;SeekBar slider;TextView number;
    ExposurePicker(Activity a,float ev,ExposureLimits range,Change listener){super(a);owner=a;limits=range;value=ExposureScale.snap(ev,limits);change=listener;}
    int dp(float n){return Math.round(n*owner.getResources().getDisplayMetrics().density);}
    int target(){return UiSpec.toolHeight(owner.getResources().getConfiguration().fontScale);}
    TextView text(String value,int size,int color){TextView t=new TextView(owner);t.setText(value);t.setTextSize(Math.max(UiSpec.CAPTION,size));t.setFontFeatureSettings("tnum");t.setTextColor(color);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER);return t;}
    GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    void choose(float ev){float next=ExposureScale.snap(ev,limits);if(next!=value){value=next;change.value(value);number.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);}number.setText(ExposureValue.label(value));slider.setProgress(ExposureScale.progress(value,limits));}
    @Override protected boolean keepPreviewVisible(){return true;}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);setCanceledOnTouchOutside(true);LinearLayout root=new LinearLayout(owner);root.setOrientation(1);root.setPadding(dp(20),dp(14),dp(20),dp(22));root.setBackground(bg(INK,UiSpec.RADIUS));
        LinearLayout header=new LinearLayout(owner);TextView title=text("曝光补偿",20,CREAM);title.setGravity(Gravity.CENTER_VERTICAL);header.addView(title,new LinearLayout.LayoutParams(0,dp(target()),1));TextView done=text("完成",14,AMBER);header.addView(done,new LinearLayout.LayoutParams(dp(52),dp(target())));done.setOnClickListener(v->dismiss());root.addView(header);
        number=text(ExposureValue.label(value),38,CREAM);number.setTypeface(Typeface.MONOSPACE);root.addView(number,new LinearLayout.LayoutParams(-1,dp(Math.round(58*Math.max(1,owner.getResources().getConfiguration().fontScale)))));root.addView(text("柔和曝光 · 0.1细调\n±3.0 保留原 ±2.0 的效果",11,MUTED),new LinearLayout.LayoutParams(-1,-2));
        LinearLayout controls=new LinearLayout(owner);controls.setGravity(Gravity.CENTER_VERTICAL);TextView minus=text("−",25,CREAM),plus=text("+",25,CREAM);minus.setContentDescription("减少0.1曝光刻度");plus.setContentDescription("增加0.1曝光刻度");controls.addView(minus,new LinearLayout.LayoutParams(dp(target()),dp(target())));slider=new SeekBar(owner);slider.setMax(ExposureScale.count(limits));slider.setProgress(ExposureScale.progress(value,limits));slider.setProgressTintList(ColorStateList.valueOf(AMBER));slider.setThumbTintList(ColorStateList.valueOf(CREAM));slider.setContentDescription("柔和曝光补偿，0.1细调刻度，最亮最暗效果保持");controls.addView(slider,new LinearLayout.LayoutParams(0,dp(target()),1));controls.addView(plus,new LinearLayout.LayoutParams(dp(target()),dp(target())));root.addView(controls);minus.setOnClickListener(v->choose(value-ExposureScale.STEP));plus.setOnClickListener(v->choose(value+ExposureScale.STEP));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(user)choose(ExposureScale.value(p,limits));}});
        LinearLayout footer=new LinearLayout(owner);footer.setGravity(Gravity.CENTER_VERTICAL);TextView low=text(ExposureValue.label(ExposureScale.min(limits))+" EV",10,MUTED);footer.addView(low,new LinearLayout.LayoutParams(0,dp(target()),1));TextView reset=text("重置为 0",12,AMBER);reset.setBackground(bg(PANEL,12));footer.addView(reset,new LinearLayout.LayoutParams(dp(110),dp(target())));reset.setOnClickListener(v->choose(0));TextView high=text(ExposureValue.label(ExposureScale.max(limits))+" EV",10,MUTED);footer.addView(high,new LinearLayout.LayoutParams(0,dp(target()),1));root.addView(footer);
        setContentView(root);getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().setGravity(Gravity.BOTTOM);getWindow().setLayout(-1,-2);
    }
}
