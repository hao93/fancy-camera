package com.silverroom.camera;

import android.app.*;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import static com.silverroom.camera.Controls.*;

final class PhotoViewer extends ThemedDialog {
    interface Actions{void share(String id);void raw(String id);void edit(String id);void delete(String id);void load(PhotoViewer viewer,String id,long token);}
    private final PhotoPager pager;private final Activity owner;private final Actions actions;private ZoomPhotoView photo;private TextView title,position,meta,rawButton;private boolean started;private String details="";
    PhotoViewer(Activity owner,java.util.List<String> ids,String selected,Actions actions){super(owner,ThemeUi.activityTheme());this.owner=owner;pager=new PhotoPager(ids,selected);this.actions=actions;}
    String currentId(){return pager.current();}
    boolean matches(String id,long token){return pager.matches(id,token);}
    int dp(float v){return Math.round(v*owner.getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color){TextView t=new TextView(owner);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    TextView button(String value,int color,Runnable action){TextView t=text(value,13,color);t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setContentDescription(value);GradientDrawable bg=new GradientDrawable();bg.setColor(PANEL);bg.setCornerRadius(dp(12));t.setBackground(bg);t.setOnClickListener(v->action.run());return t;}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);Window window=getWindow();window.setStatusBarColor(INK);window.setNavigationBarColor(INK);window.setDecorFitsSystemWindows(false);
        LinearLayout root=new LinearLayout(owner);root.setOrientation(1);root.setBackgroundColor(INK);root.setPadding(dp(14),dp(8),dp(14),dp(12));root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(14)+i.left,dp(8)+i.top,dp(14)+i.right,dp(12)+i.bottom);return insets;});
        LinearLayout header=new LinearLayout(owner);title=text("底片",17,CREAM);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));position=text("",11,MUTED);position.setGravity(Gravity.CENTER);header.addView(position,new LinearLayout.LayoutParams(dp(62),dp(48)));TextView close=button("完成",AMBER,this::dismiss);header.addView(close,new LinearLayout.LayoutParams(dp(58),dp(48)));root.addView(header);
        meta=text("",11,MUTED);meta.setMaxLines(2);meta.setEllipsize(android.text.TextUtils.TruncateAt.END);meta.setContentDescription("拍摄参数，点击查看完整信息");meta.setOnClickListener(v->new AlertDialog.Builder(owner,ThemeUi.alertTheme()).setTitle("拍摄与配方参数").setMessage(details).setPositiveButton("完成",null).show());root.addView(meta,new LinearLayout.LayoutParams(-1,dp(48)));
        photo=new ZoomPhotoView(owner);photo.delete=()->actions.delete(currentId());photo.exit=this::dismiss;photo.navigate=this::navigate;root.addView(photo,new LinearLayout.LayoutParams(-1,0,1));
        TextView hint=text("左右切换 · 下滑返回 · 上滑移入回收站\n双指缩放，放大后拖动查看",10,MUTED);hint.setGravity(Gravity.CENTER);root.addView(hint,new LinearLayout.LayoutParams(-1,dp(42)));
        LinearLayout tools=new LinearLayout(owner);String[] labels={"分享","DNG","再次显影","删除"};int[] colors={CREAM,CREAM,AMBER,tone(AppTheme.DANGER)};Runnable[] calls={()->actions.share(currentId()),()->actions.raw(currentId()),()->actions.edit(currentId()),()->actions.delete(currentId())};for(int i=0;i<4;i++){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);if(i<3)lp.rightMargin=dp(6);TextView item=button(labels[i],colors[i],calls[i]);if(i==1){rawButton=item;item.setVisibility(View.GONE);}tools.addView(item,lp);}root.addView(tools);
        setContentView(root);window.setLayout(-1,-1);
    }
    @Override protected void onStart(){super.onStart();if(!started){started=true;showCurrent();}}
    private void navigate(int direction){if(pager.move(direction))showCurrent();else Toast.makeText(owner,direction>0?"已是最后一张底片":"已是第一张底片",Toast.LENGTH_SHORT).show();}
    private void showCurrent(){String id=currentId();title.setText("底片 · "+id.substring(0,8));position.setText(pager.position()+" / "+pager.count());position.setContentDescription("第 "+pager.position()+" 张，共 "+pager.count()+" 张底片");meta.setText("读取底片信息…");photo.loading();photo.previous=pager.canMove(-1);photo.next=pager.canMove(1);actions.load(this,id,pager.revision());}
    void metadata(String id,long token,String description,boolean raw){if(matches(id,token)){details=description;meta.setText("拍摄参数 ⓘ · "+description);rawButton.setVisibility(raw?View.VISIBLE:View.GONE);}}
    void image(String id,long token,Bitmap value){if(matches(id,token)&&photo!=null)photo.image(value);else value.recycle();}
    void failed(String id,long token){if(matches(id,token)&&photo!=null)photo.error();}
    @Override public void dismiss(){pager.close();super.dismiss();if(photo!=null)photo.image(null);}
}
