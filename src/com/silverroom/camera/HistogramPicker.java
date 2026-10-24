package com.silverroom.camera;

import android.app.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import static com.silverroom.camera.Controls.*;

/** Enlarges the compact chart without starting another camera readback. */
final class HistogramPicker extends ThemedDialog {
    private final Activity owner;private final HistogramView source;private final String film;private HistogramView chart;
    HistogramPicker(Activity owner,HistogramView source,String film){super(owner);this.owner=owner;this.source=source;this.film=film;}
    int dp(float n){return Math.round(n*owner.getResources().getDisplayMetrics().density);}
    int target(){return UiSpec.toolHeight(owner.getResources().getConfiguration().fontScale);}
    TextView text(String value,int size,int color){TextView t=new TextView(owner);t.setText(value);t.setTextSize(Math.max(UiSpec.CAPTION,size));t.setFontFeatureSettings("tnum");t.setTextColor(color);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    @Override protected boolean keepPreviewVisible(){return true;}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);setCanceledOnTouchOutside(true);
        LinearLayout root=new LinearLayout(owner);root.setOrientation(1);root.setPadding(dp(20),dp(14),dp(20),dp(22));GradientDrawable background=new GradientDrawable();background.setColor(INK);background.setCornerRadius(dp(UiSpec.RADIUS));root.setBackground(background);
        LinearLayout header=new LinearLayout(owner);TextView title=text("实时直方图",20,CREAM);header.addView(title,new LinearLayout.LayoutParams(0,dp(target()),1));TextView done=text("完成",14,AMBER);done.setGravity(Gravity.CENTER);done.setFocusable(true);done.setContentDescription("关闭放大直方图");header.addView(done,new LinearLayout.LayoutParams(dp(52),dp(target())));done.setOnClickListener(v->dismiss());root.addView(header);
        TextView caption=text(film+" · 左暗右亮",11,MUTED);root.addView(caption,new LinearLayout.LayoutParams(-1,-2));
        chart=new HistogramView(owner,true);int height=Math.min(dp(220),Math.round(owner.getResources().getDisplayMetrics().heightPixels*.28f));root.addView(chart,new LinearLayout.LayoutParams(-1,height));
        LinearLayout legend=new LinearLayout(owner);legend.setGravity(Gravity.CENTER);String[] labels={"● 红","● 绿","● 蓝","─ 亮度"};int[] colors={tone(AppTheme.RED),tone(AppTheme.GREEN),tone(AppTheme.BLUE),CREAM};for(int i=0;i<labels.length;i++){TextView label=text(labels[i],11,colors[i]);label.setGravity(Gravity.CENTER);legend.addView(label,new LinearLayout.LayoutParams(0,dp(target()),1));}root.addView(legend);
        TextView hint=text("随取景实时更新 · 显示当前胶片处理后的画面",10,MUTED);hint.setGravity(Gravity.CENTER);root.addView(hint,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().setGravity(Gravity.BOTTOM);getWindow().setLayout(-1,-2);
    }
    @Override protected void onStart(){super.onStart();source.attach(chart);}
    @Override protected void onStop(){source.detach(chart);super.onStop();}
}
