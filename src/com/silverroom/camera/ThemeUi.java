package com.silverroom.camera;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Recolors existing UI in place: no Activity recreation, camera rebind or photo re-render. */
final class ThemeUi {
    static int activityTheme(){return AppTheme.light()?android.R.style.Theme_Material_Light_NoActionBar:android.R.style.Theme_Material_NoActionBar;}
    static int dialogTheme(){return AppTheme.light()?android.R.style.Theme_Material_Light_Dialog:android.R.style.Theme_Material_Dialog;}
    static int alertTheme(){return AppTheme.light()?android.R.style.Theme_Material_Light_Dialog_Alert:android.R.style.Theme_Material_Dialog_Alert;}
    static Context context(Context owner){return new ContextThemeWrapper(owner,activityTheme());}
    static void bars(Window window){
        if(window==null)return;
        // PhoneWindow.getInsetsController() dereferences mDecor before it is installed.
        // getDecorView() installs it; the View accessor is safe before attachment.
        View decor=window.getDecorView();
        window.setStatusBarColor(Controls.INK);window.setNavigationBarColor(Controls.INK);
        WindowInsetsController controller=decor.getWindowInsetsController();if(controller!=null){int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;controller.setSystemBarsAppearance(AppTheme.light()?mask:0,mask);}
    }
    static GradientDrawable surface(Context context,int fill,float radius,int stroke){return new Surface(fill,radius*context.getResources().getDisplayMetrics().density,stroke,context.getResources().getDisplayMetrics().density);}
    private static final class Surface extends GradientDrawable {
        int fill,stroke;final float width;
        Surface(int f,float radius,int s,float w){fill=f;stroke=s;width=w;setCornerRadius(radius);paint();}
        void paint(){setColor(fill);if(stroke!=0)setStroke(Math.max(1,Math.round(width)),stroke);}
        void remap(int from,int to){fill=AppTheme.remap(fill,from,to);stroke=AppTheme.remap(stroke,from,to);paint();}
    }
    private static ColorStateList tint(ColorStateList value,int from,int to){return value==null?null:ColorStateList.valueOf(AppTheme.remap(value.getDefaultColor(),from,to));}
    static void refresh(View view,int from,int to){
        Drawable background=view.getBackground();if(background instanceof Surface)((Surface)background).remap(from,to);else if(background instanceof ColorDrawable)((ColorDrawable)background).setColor(AppTheme.remap(((ColorDrawable)background).getColor(),from,to));
        if(view instanceof TextView){TextView text=(TextView)view;text.setTextColor(tint(text.getTextColors(),from,to));text.setHintTextColor(tint(text.getHintTextColors(),from,to));text.setHighlightColor(AppTheme.color(AppTheme.ACTIVE));}
        if(view instanceof SeekBar){SeekBar slider=(SeekBar)view;slider.setProgressTintList(ColorStateList.valueOf(Controls.AMBER));slider.setProgressBackgroundTintList(ColorStateList.valueOf(AppTheme.color(AppTheme.BORDER)));slider.setThumbTintList(ColorStateList.valueOf(Controls.CREAM));}
        if(view instanceof Controls.Dial){Controls.Dial dial=(Controls.Dial)view;dial.accent=AppTheme.remap(dial.accent,from,to);}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)refresh(group.getChildAt(i),from,to);}
        view.invalidate();
    }
}
