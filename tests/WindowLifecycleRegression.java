import javax.tools.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Executes the production ThemeUi against a strict PhoneWindow lifecycle fixture.
 * Fixtures live only in build/, and no Android substitutes enter the APK.
 */
public final class WindowLifecycleRegression {
    static void write(Path dir,List<String> files,String name,String source)throws IOException{Path file=dir.resolve(name);Files.createDirectories(file.getParent());Files.write(file,source.getBytes(StandardCharsets.UTF_8));files.add(file.toString());}
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args[0]),dir=root.resolve("build/window-lifecycle-tests"),classes=dir.resolve("classes");Files.createDirectories(classes);List<String> files=new ArrayList<>();
        write(dir,files,"android/R.java","package android; public final class R {public static final class style {public static final int Theme_Material_Light_NoActionBar=1,Theme_Material_NoActionBar=2,Theme_Material_Light_Dialog=3,Theme_Material_Dialog=4,Theme_Material_Light_Dialog_Alert=5,Theme_Material_Dialog_Alert=6;}}");
        write(dir,files,"android/util/DisplayMetrics.java","package android.util; public class DisplayMetrics {public float density=1;}");
        write(dir,files,"android/content/res/Resources.java","package android.content.res; public class Resources {public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}");
        write(dir,files,"android/content/Context.java","package android.content; public class Context {public android.content.res.Resources getResources(){return new android.content.res.Resources();}}");
        write(dir,files,"android/content/res/ColorStateList.java","package android.content.res; public class ColorStateList {private int c;public static ColorStateList valueOf(int c){ColorStateList s=new ColorStateList();s.c=c;return s;}public int getDefaultColor(){return c;}}");
        write(dir,files,"android/graphics/drawable/Drawable.java","package android.graphics.drawable; public class Drawable {}");
        write(dir,files,"android/graphics/drawable/GradientDrawable.java","package android.graphics.drawable; public class GradientDrawable extends Drawable {public void setColor(int c){}public void setCornerRadius(float r){}public void setStroke(int w,int c){}}");
        write(dir,files,"android/graphics/drawable/ColorDrawable.java","package android.graphics.drawable; public class ColorDrawable extends Drawable {private int c;public int getColor(){return c;}public void setColor(int n){c=n;}}");
        write(dir,files,"android/view/WindowInsetsController.java","package android.view; public class WindowInsetsController {public static final int APPEARANCE_LIGHT_STATUS_BARS=8,APPEARANCE_LIGHT_NAVIGATION_BARS=16;public int appearance=64,calls;public void setSystemBarsAppearance(int value,int mask){appearance=(appearance&~mask)|(value&mask);calls++;}}");
        write(dir,files,"android/view/View.java","package android.view; public class View {public boolean attached;public final WindowInsetsController controller=new WindowInsetsController();public WindowInsetsController getWindowInsetsController(){return attached?controller:null;}public android.graphics.drawable.Drawable getBackground(){return null;}public void invalidate(){}}");
        write(dir,files,"android/view/ViewGroup.java","package android.view; public class ViewGroup extends View {public int getChildCount(){return 0;}public View getChildAt(int i){return null;}}");
        write(dir,files,"android/view/Window.java","package android.view; public class Window {public View decor;public int status,navigation,unsafeReads;public View getDecorView(){if(decor==null)decor=new View();return decor;}public WindowInsetsController getInsetsController(){unsafeReads++;return decor.getWindowInsetsController();}public void setStatusBarColor(int c){status=c;}public void setNavigationBarColor(int c){navigation=c;}}");
        write(dir,files,"android/view/ContextThemeWrapper.java","package android.view; public class ContextThemeWrapper extends android.content.Context {public ContextThemeWrapper(android.content.Context c,int theme){}}");
        write(dir,files,"android/widget/TextView.java","package android.widget; public class TextView extends android.view.View {public android.content.res.ColorStateList getTextColors(){return android.content.res.ColorStateList.valueOf(0);}public android.content.res.ColorStateList getHintTextColors(){return null;}public void setTextColor(android.content.res.ColorStateList c){}public void setHintTextColor(android.content.res.ColorStateList c){}public void setHighlightColor(int c){}}");
        write(dir,files,"android/widget/SeekBar.java","package android.widget; public class SeekBar extends android.view.View {public void setProgressTintList(android.content.res.ColorStateList c){}public void setProgressBackgroundTintList(android.content.res.ColorStateList c){}public void setThumbTintList(android.content.res.ColorStateList c){}}");
        write(dir,files,"com/silverroom/camera/Controls.java","package com.silverroom.camera; final class Controls {static int INK,AMBER,CREAM;static class Dial extends android.view.View {int accent;}}");
        write(dir,files,"com/silverroom/camera/WindowAppearanceProbe.java",
            "package com.silverroom.camera; import android.view.*;import java.util.*;public final class WindowAppearanceProbe {static List<String> passed=new ArrayList<>();static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);passed.add(label);}public static List<String> run(){"+
            "boolean reproduced=false;try{new Window().getInsetsController();}catch(NullPointerException expected){reproduced=true;}check(reproduced,\"Strict pre-decor PhoneWindow fixture reproduces the legacy null dereference\");"+
            "for(int mode=0;mode<2;mode++){AppTheme.select(mode);Controls.INK=AppTheme.color(AppTheme.BG);Window window=new Window();ThemeUi.bars(window);check(window.decor!=null&&window.unsafeReads==0,\"Cold start mode \"+mode+\" installs decor without the unsafe Window controller accessor\");"+
            "check(window.status==Controls.INK&&window.navigation==Controls.INK&&window.decor.controller.calls==0,\"Unattached mode \"+mode+\" applies colors and safely defers unavailable controls\");"+
            "window.decor.attached=true;ThemeUi.bars(window);int wanted=64|(mode==1?24:0);check(window.decor.controller.appearance==wanted,\"Attached mode \"+mode+\" applies matching system icon contrast without clearing unrelated flags\");"+
            "ThemeUi.bars(window);check(window.decor.controller.appearance==wanted&&window.unsafeReads==0,\"Repeated focus or sheet setup in mode \"+mode+\" is safe and idempotent\");"+
            "AppTheme.select(1-mode);Controls.INK=AppTheme.color(AppTheme.BG);ThemeUi.bars(window);check(window.decor.controller.appearance==(64|(mode==0?24:0))&&window.status==Controls.INK,\"Live theme changes from mode \"+mode+\" update existing bars without rebuilding the window\");}"+
            "ThemeUi.bars(null);check(true,\"A missing optional window does not crash appearance setup\");return passed;}}");
        Path themeSource=args.length>1?Paths.get(args[1]):root.resolve("src/com/silverroom/camera/ThemeUi.java");
        files.add(themeSource.toString());files.add(root.resolve("src/com/silverroom/camera/AppTheme.java").toString());
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();if(compiler==null)throw new IllegalStateException("JDK compiler required");List<String> options=new ArrayList<>(Arrays.asList("--release","8","-encoding","UTF-8","-d",classes.toString()));options.addAll(files);if(compiler.run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Window lifecycle fixture compilation failed");
        try(URLClassLoader loader=new URLClassLoader(new URL[]{classes.toUri().toURL()},null)){
            Object result;try{result=loader.loadClass("com.silverroom.camera.WindowAppearanceProbe").getMethod("run").invoke(null);}catch(java.lang.reflect.InvocationTargetException failure){throw new AssertionError("Production ThemeUi failed the strict window lifecycle",failure.getCause());}
            List<?> passed=(List<?>)result;for(Object label:passed)System.out.println("PASS "+label);System.out.println("ALL "+passed.size()+" PRODUCTION WINDOW LIFECYCLE TESTS PASSED");
        }
    }
}
