package com.silverroom.camera;

import android.app.Dialog;
import android.content.Context;

/** All app-owned sheets use the selected appearance, including system bar icon contrast. */
class ThemedDialog extends Dialog {
    ThemedDialog(Context context){super(context,ThemeUi.dialogTheme());}
    ThemedDialog(Context context,int theme){super(context,theme);}
    @Override protected void onStart(){super.onStart();ThemeUi.bars(getWindow());getWindow().getDecorView().post(()->ThemeUi.bars(getWindow()));}
}
