package com.silverroom.camera;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.content.res.ColorStateList;
import static com.silverroom.camera.Controls.*;

/** Scrollable settings/tools surface; common controls have at least 48dp targets. */
final class UtilitySheet extends ThemedDialog {
    interface Build{void build(UtilitySheet sheet);}interface Slide{void value(float value);}
    final MainActivity a;private final String title;private final Build build;LinearLayout content;private final java.util.List<Runnable> watchers=new java.util.ArrayList<>();private boolean watching;
    UtilitySheet(MainActivity owner,String name,Build body){super(owner);a=owner;title=name;build=body;}
    int dp(float n){return a.dp(n);}
    @Override protected void onCreate(Bundle state){super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout root=a.column();root.setPadding(dp(18),dp(10),dp(18),dp(18));root.setBackground(a.bg(INK,20,0));LinearLayout head=a.row();head.addView(a.text(title,20,CREAM),new LinearLayout.LayoutParams(0,dp(48),1));a.add(head,a.button("完成",this::dismiss),60,48);a.add(root,head,-1,48);ScrollView scroll=new ScrollView(a){@Override protected void onMeasure(int w,int h){int cap=(int)(a.getResources().getDisplayMetrics().heightPixels*.82f)-dp(78);super.onMeasure(w,MeasureSpec.makeMeasureSpec(Math.max(dp(80),cap),MeasureSpec.AT_MOST));}};scroll.setFillViewport(false);content=a.column();scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));build.build(this);setContentView(root);getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().setGravity(Gravity.BOTTOM);getWindow().setLayout(-1,-2);}
    void section(String text){a.gap(content,12);a.add(content,a.text(text,13,CREAM),-1,30);}
    TextView note(String text){TextView label=a.text(text,11,MUTED);label.setPadding(0,dp(5),0,dp(7));label.setMaxLines(5);a.add(content,label,-1,-2);return label;}
    void status(java.util.function.Supplier<String> text){TextView label=note(text.get());Runnable update=new Runnable(){public void run(){if(!watching)return;label.setText(text.get());a.main.postDelayed(this,700);}};watchers.add(update);}
    @Override protected void onStart(){super.onStart();watching=true;for(Runnable update:watchers)a.main.post(update);}
    @Override protected void onStop(){watching=false;for(Runnable update:watchers)a.main.removeCallbacks(update);super.onStop();}
    TextView action(String text,Runnable call){TextView button=a.button(text,call);a.add(content,button,-1,48);a.gap(content,6);return button;}
    void toggle(String label,boolean value,java.util.function.Consumer<Boolean> changed){Switch control=new Switch(a);control.setText(label);control.setTextSize(13);control.setTextColor(CREAM);control.setPadding(dp(12),0,dp(12),0);control.setSwitchMinWidth(dp(42));control.setSwitchPadding(dp(12));control.setBackground(a.bg(PANEL,12,0));ThemeUi.tintSwitch(control);control.setChecked(value);control.setOnCheckedChangeListener((v,on)->changed.accept(on));a.add(content,control,-1,52);a.gap(content,6);}
    void choices(String[] labels,int selected,java.util.function.IntConsumer changed){LinearLayout row=a.row();for(int i=0;i<labels.length;i++){final int id=i;TextView item=a.button(labels[i]+(selected==i?" ✓":""),()->{changed.accept(id);dismiss();});item.setTextColor(i==selected?AMBER:CREAM);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);if(i>0)lp.leftMargin=dp(6);row.addView(item,lp);}a.add(content,row,-1,48);a.gap(content,6);}
    void slider(String name,float lo,float hi,float value,Slide change){LinearLayout labels=a.row();TextView title=a.text(name,12,CREAM),number=a.text(String.format(java.util.Locale.US,"%.2f",value),11,AMBER);labels.addView(title,new LinearLayout.LayoutParams(0,dp(30),1));a.add(labels,number,60,30);a.add(content,labels,-1,30);SeekBar bar=new SeekBar(a);bar.setMax(100);bar.setProgress(Math.round((value-lo)/(hi-lo)*100));bar.setProgressTintList(ColorStateList.valueOf(AMBER));bar.setThumbTintList(ColorStateList.valueOf(CREAM));bar.setContentDescription(name);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean user){float v=lo+(hi-lo)*p/100;number.setText(String.format(java.util.Locale.US,"%.2f",v));if(user)change.value(v);}});a.add(content,bar,-1,48);}
}
