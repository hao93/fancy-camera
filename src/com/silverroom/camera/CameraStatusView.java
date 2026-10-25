package com.silverroom.camera;
import android.content.Context;
import android.view.*;
import android.widget.*;
import static com.silverroom.camera.Controls.*;

/** Transient non-intercepting chip; only persistent errors expose a clickable inspection action. */
final class CameraStatusView extends LinearLayout {
    final TextView label;private final ProgressBar spinner;private int phase=-1;
    CameraStatusView(Context context,Runnable inspect){super(context);float d=getResources().getDisplayMetrics().density;setGravity(Gravity.CENTER_VERTICAL);setPadding((int)(10*d),0,(int)(10*d),0);setMinimumHeight((int)(48*d));
        setBackground(ThemeUi.surface(context,AppTheme.OVERLAY_BG,12,0));spinner=new ProgressBar(context);spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(AppTheme.OVERLAY_ACCENT));addView(spinner,new LayoutParams((int)(16*d),(int)(16*d)));
        label=new TextView(context);label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);spinner.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);label.setTextSize(UiSpec.CAPTION);label.setTextColor(AppTheme.OVERLAY_TEXT);label.setPadding((int)(6*d),0,0,0);label.setMaxLines(2);addView(label,new LayoutParams(-2,-2));setOnClickListener(v->inspect.run());setVisibility(GONE);}
    private final Runnable show=()->{if(phase==CameraUiState.SWITCHING||phase==CameraUiState.CONNECTING)setVisibility(VISIBLE);};
    void state(CameraUiState value){int previous=phase;phase=value.phase;if(phase==CameraUiState.READY){removeCallbacks(show);if(previous!=phase){setVisibility(GONE);}return;}
        if(phase==CameraUiState.PAUSED){removeCallbacks(show);setVisibility(GONE);return;}boolean error=phase==CameraUiState.ERROR;spinner.setVisibility(error?GONE:VISIBLE);label.setText(value.label());setContentDescription(value.label()+(error?"，"+value.reason:""));setClickable(error);setFocusable(error);setAccessibilityLiveRegion(error?ACCESSIBILITY_LIVE_REGION_POLITE:ACCESSIBILITY_LIVE_REGION_NONE);
        if(error){removeCallbacks(show);setVisibility(VISIBLE);}else if(previous!=phase){removeCallbacks(show);setVisibility(GONE);postDelayed(show,250);}}
    void failure(String message){state(new CameraUiState(CameraUiState.ERROR,23,23,false,message));}
    @Override protected void onDetachedFromWindow(){removeCallbacks(show);super.onDetachedFromWindow();}
}
