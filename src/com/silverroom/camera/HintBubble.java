package com.silverroom.camera;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

/** Once-per-upgrade, non-modal hints; dismissing never consumes the user's operation. */
final class HintBubble extends TextView {
    private final Runnable hide=this::dismissHint;
    HintBubble(Context c,String message){super(c);float d=getResources().getDisplayMetrics().density;setText(message);setTextSize(12);setTextColor(AppTheme.OVERLAY_TEXT);setPadding((int)(12*d),(int)(8*d),(int)(12*d),(int)(8*d));setGravity(android.view.Gravity.CENTER);setBackground(ThemeUi.surface(c,AppTheme.OVERLAY_BG,12,0));setClickable(false);setFocusable(false);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);setVisibility(GONE);}
    void once(String key){if(!hasWindowFocus())return;android.content.SharedPreferences p=getContext().getSharedPreferences("silverroom",0);if(p.getBoolean(key,false))return;p.edit().putBoolean(key,true).apply();setVisibility(VISIBLE);postDelayed(hide,5000);}
    void dismissHint(){removeCallbacks(hide);setVisibility(GONE);}
    @Override protected void onDetachedFromWindow(){dismissHint();super.onDetachedFromWindow();}
}
