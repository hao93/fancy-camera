package com.silverroom.camera;

import android.app.*;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import static com.silverroom.camera.Controls.*;

final class PhotoViewer extends ThemedDialog {
    interface Actions{void share(String id);void raw(String id);void print(String id);void edit(String id);void delete(String id);void load(PhotoViewer viewer,String id,long token);}
    private final PhotoPager pager;private final Activity owner;private final Actions actions;private ZoomPhotoView photo;private TextView title,position,meta,rawButton;private boolean started,chromeVisible=true;private LinearLayout topChrome,bottomChrome;private HintBubble browseHint;private String details="";
    PhotoViewer(Activity owner,java.util.List<String> ids,String selected,Actions actions){super(owner,ThemeUi.activityTheme());this.owner=owner;pager=new PhotoPager(ids,selected);this.actions=actions;}
    String currentId(){return pager.current();}
    boolean matches(String id,long token){return pager.matches(id,token);}
    int dp(float v){return Math.round(v*owner.getResources().getDisplayMetrics().density);}
    TextView text(String value,int size,int color){TextView t=new TextView(owner);t.setText(value);t.setTextSize(Math.max(UiSpec.CAPTION,size));t.setTextColor(color);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    TextView button(String value,int color,Runnable action){TextView t=text(value,UiSpec.BODY,color);t.setGravity(Gravity.CENTER);t.setFocusable(true);t.setMinHeight(dp(48));t.setMaxLines(2);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setContentDescription(value);GradientDrawable bg=new GradientDrawable();bg.setColor(PANEL);bg.setCornerRadius(dp(12));t.setBackground(bg);t.setOnClickListener(v->action.run());return t;}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);requestWindowFeature(Window.FEATURE_NO_TITLE);Window window=getWindow();window.setStatusBarColor(INK);window.setNavigationBarColor(INK);window.setDecorFitsSystemWindows(false);
        FrameLayout root=new FrameLayout(owner);root.setBackgroundColor(tone(AppTheme.IMAGE_BG));root.setPadding(dp(12),dp(8),dp(12),dp(12));root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(dp(12)+i.left,dp(8)+i.top,dp(12)+i.right,dp(12)+i.bottom);return insets;});
        // The photo owns one fixed viewport. Chrome never takes space away from it.
        photo=new ZoomPhotoView(owner);photo.delete=()->actions.delete(currentId());photo.exit=this::dismiss;photo.navigate=this::navigate;photo.confirmedTap=()->chrome(!chromeVisible);root.addView(photo,new FrameLayout.LayoutParams(-1,-1));
        float font=owner.getResources().getConfiguration().fontScale;int target=dp(UiSpec.toolHeight(font));
        topChrome=new LinearLayout(owner);topChrome.setOrientation(1);topChrome.setPadding(dp(8),0,dp(8),dp(8));topChrome.setBackground(ThemeUi.surface(owner,PANEL,12,0));
        LinearLayout header=new LinearLayout(owner);title=text("底片",20,CREAM);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);header.addView(title,new LinearLayout.LayoutParams(0,target,1));position=text("",12,MUTED);position.setTypeface(android.graphics.Typeface.MONOSPACE);position.setGravity(Gravity.CENTER);header.addView(position,new LinearLayout.LayoutParams(dp(64*Math.max(1,font)),target));TextView close=button("完成",AMBER,this::dismiss);header.addView(close,new LinearLayout.LayoutParams(dp(Math.max(56,30*font+8)),target));topChrome.addView(header);
        meta=text("",12,MUTED);meta.setMaxLines(2);meta.setEllipsize(android.text.TextUtils.TruncateAt.END);meta.setContentDescription("拍摄参数，点击查看完整信息");meta.setFocusable(true);meta.setOnClickListener(v->new AlertDialog.Builder(owner,ThemeUi.alertTheme()).setTitle("拍摄与配方参数").setMessage(details).setPositiveButton("完成",null).show());topChrome.addView(meta,new LinearLayout.LayoutParams(-1,target));root.addView(topChrome,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        bottomChrome=new LinearLayout(owner);bottomChrome.setOrientation(1);bottomChrome.setPadding(dp(4),dp(4),dp(4),dp(4));bottomChrome.setBackground(ThemeUi.surface(owner,PANEL,12,0));
        LinearLayout tools=new LinearLayout(owner);String[] labels={"分享","DNG","暗房","排版导出","删除"};int[] colors={CREAM,CREAM,AMBER,CREAM,tone(AppTheme.DANGER)};Runnable[] calls={()->actions.share(currentId()),()->actions.raw(currentId()),()->actions.edit(currentId()),()->actions.print(currentId()),()->actions.delete(currentId())};for(int i=0;i<5;i++){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Math.max(target,dp(40*font+16)),1);if(i<4)lp.rightMargin=dp(4);TextView item=button(labels[i],colors[i],calls[i]);item.setTextSize(12);if(i==1){rawButton=item;item.setVisibility(View.GONE);}tools.addView(item,lp);}bottomChrome.addView(tools);root.addView(bottomChrome,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        browseHint=new HintBubble(owner,"轻点隐藏工具 · 双击或双指缩放\n左右切换 · 下滑返回 · 上滑删除");FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);hp.setMargins(dp(16),0,dp(16),0);root.addView(browseHint,hp);
        setContentView(root);window.setLayout(-1,-1);
    }
    private void chrome(boolean visible){chromeVisible=visible;for(View layer:new View[]{topChrome,bottomChrome}){layer.animate().cancel();enableTree(layer,visible);layer.setImportantForAccessibility(visible?View.IMPORTANT_FOR_ACCESSIBILITY_AUTO:View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);if(visible){if(layer.getVisibility()!=View.VISIBLE)layer.setAlpha(0);layer.setVisibility(View.VISIBLE);}layer.animate().alpha(visible?1:0).setDuration(160).withEndAction(()->{if(!chromeVisible)layer.setVisibility(View.INVISIBLE);}).start();}if(browseHint!=null)browseHint.dismissHint();}
    private void enableTree(View v,boolean enabled){v.setEnabled(enabled);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)enableTree(g.getChildAt(i),enabled);}}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus&&browseHint!=null)browseHint.once("hint_detail_230");}
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN&&browseHint!=null)browseHint.dismissHint();return super.dispatchTouchEvent(event);}
    @Override protected void onStart(){super.onStart();if(!started){started=true;showCurrent();}}
    private void navigate(int direction){if(pager.move(direction))showCurrent();else Toast.makeText(owner,direction>0?"已是最后一张底片":"已是第一张底片",Toast.LENGTH_SHORT).show();}
    private void showCurrent(){String id=currentId();title.setText("底片 · "+id.substring(0,8));position.setText(pager.position()+" / "+pager.count());position.setContentDescription("第 "+pager.position()+" 张，共 "+pager.count()+" 张底片");meta.setText("读取底片信息…");photo.loading();photo.previous=pager.canMove(-1);photo.next=pager.canMove(1);actions.load(this,id,pager.revision());}
    void metadata(String id,long token,String description,boolean raw){if(matches(id,token)){details=description;meta.setText("拍摄参数 ⓘ · "+description);rawButton.setVisibility(raw?View.VISIBLE:View.GONE);}}
    void image(String id,long token,Bitmap value){if(matches(id,token)&&photo!=null)photo.image(value);else value.recycle();}
    void failed(String id,long token){if(matches(id,token)&&photo!=null)photo.error();}
    @Override public void dismiss(){pager.close();super.dismiss();if(photo!=null)photo.image(null);}
}
