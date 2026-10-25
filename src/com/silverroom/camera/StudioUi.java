package com.silverroom.camera;
import android.widget.*;import android.text.*;import java.util.*;import static com.silverroom.camera.Controls.*;

final class StudioUi {
    private StudioUi(){}
    static EditText field(MainActivity a,UtilitySheet s,String name,String value,boolean numeric){s.note(name);EditText e=new EditText(a);e.setSingleLine(true);e.setTextSize(15);e.setTextColor(CREAM);e.setHintTextColor(MUTED);e.setPadding(a.dp(12),a.dp(8),a.dp(12),a.dp(8));e.setBackground(a.bg(PANEL,12,0));e.setText(value);e.setSelectAllOnFocus(true);e.setContentDescription(name);if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);a.add(s.content,e,-1,52);return e;}
    static double number(EditText e,double lo,double hi){double n;try{n=Double.parseDouble(e.getText().toString().trim());}catch(Exception failure){throw new IllegalArgumentException("请填写有效数字");}if(!Double.isFinite(n)||n<lo||n>hi)throw new IllegalArgumentException("数值范围 "+lo+" 至 "+hi);return n;}
    static String required(EditText e){String s=e.getText().toString().trim();if(s.isEmpty()||s.length()>120)throw new IllegalArgumentException("名称需要 1–120 个字");return s;}
    static void search(EditText e,java.util.function.Consumer<String> changed){e.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){changed.accept(s.toString());}public void afterTextChanged(Editable x){}});}
    static String wb(int mode){String[] names={"关闭","自动","白炽灯","荧光灯","暖荧光","日光","多云","黄昏","阴影"};return mode>=0&&mode<names.length?names[mode]:"自动";}
    static void work(MainActivity a,Runnable task,Runnable done){a.worker.execute(()->{try{task.run();a.main.post(()->{if(!a.destroyed)done.run();});}catch(Exception e){a.main.post(()->a.toast(e.getMessage()==null?"操作未完成":e.getMessage()));}});}
    static void append(LinearLayout row,MainActivity a,android.view.View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,a.dp(48),1);if(row.getChildCount()>0)lp.leftMargin=a.dp(8);row.addView(v,lp);}
}
