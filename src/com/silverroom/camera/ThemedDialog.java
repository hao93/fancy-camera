package com.silverroom.camera;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

/** All app-owned sheets use the selected appearance, including system bar icon contrast. */
class ThemedDialog extends Dialog {
    ThemedDialog(Context context){super(context,ThemeUi.dialogTheme());}
    ThemedDialog(Context context,int theme){super(context,theme);}
    protected boolean keepPreviewVisible(){return false;}
    private void previewWindow(){if(!keepPreviewVisible())return;Window window=getWindow();window.setDimAmount(0);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND|WindowManager.LayoutParams.FLAG_BLUR_BEHIND);if(Build.VERSION.SDK_INT>=31)window.setBackgroundBlurRadius(0);}
    @Override protected void onStart(){super.onStart();previewWindow();ThemeUi.bars(getWindow());getWindow().getDecorView().post(()->ThemeUi.bars(getWindow()));}
}
