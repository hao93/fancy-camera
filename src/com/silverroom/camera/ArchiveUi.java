package com.silverroom.camera;
import android.graphics.Color;import android.graphics.drawable.InsetDrawable;import android.util.TypedValue;import android.view.*;import android.widget.*;import static com.silverroom.camera.Controls.*;

/** Compact visual surfaces with accessible targets and content-driven text height. */
final class ArchiveUi {
    private ArchiveUi(){}
    static TextView icon(MainActivity a,String glyph,String description,Runnable action){TextView v=a.button(glyph,action);v.setBackgroundColor(Color.TRANSPARENT);v.setPadding(0,0,0,0);v.setTextSize(TypedValue.COMPLEX_UNIT_DIP,28);v.setMaxLines(1);v.setContentDescription(description);v.setTooltipText(description);return v;}
    static TextView command(MainActivity a,String label,String description,Runnable action){TextView v=a.button(label,action);v.setTextSize(13);v.setPadding(a.dp(6),a.dp(4),a.dp(6),a.dp(4));v.setMaxLines(1);v.setBackground(new InsetDrawable(a.bg(PANEL,10,tone(AppTheme.BORDER)),0,a.dp(6),0,a.dp(6)));v.setPadding(a.dp(6),a.dp(4),a.dp(6),a.dp(4));v.setContentDescription(description);v.setTooltipText(description);return v;}
    static TextView tab(MainActivity a,String label,boolean selected,Runnable action){TextView v=command(a,label,label+(selected?"，已选中":"，点击切换"),action);v.setTextColor(selected?AMBER:MUTED);v.setSelected(selected);v.setBackground(new InsetDrawable(a.bg(selected?tone(AppTheme.ACTIVE):INK,12,selected?tone(AppTheme.SELECT_BORDER):0),a.dp(2),a.dp(4),a.dp(2),a.dp(4)));v.setPadding(a.dp(6),a.dp(4),a.dp(6),a.dp(4));return v;}
    static void append(MainActivity a,LinearLayout row,View child,int height){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,a.dp(height),1);if(row.getChildCount()>0)lp.leftMargin=a.dp(8);row.addView(child,lp);}
    static ListView body(MainActivity a,LinearLayout root,LinearLayout header){ListView list=new ListView(a);list.setDivider(null);list.setDividerHeight(a.dp(8));list.setItemsCanFocus(true);list.setClipToPadding(false);list.setPadding(0,0,0,a.dp(16));list.addHeaderView(header,null,false);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));return list;}
    static TextView label(MainActivity a,String text,int sp,int color,int lines){TextView v=a.text(text,sp,color);v.setMaxLines(lines);v.setEllipsize(android.text.TextUtils.TruncateAt.END);return v;}
}
