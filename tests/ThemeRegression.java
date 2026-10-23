import com.silverroom.camera.*;
import android.graphics.Bitmap;
import java.util.*;

public final class ThemeRegression {
    static int count;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);count++;System.out.println("PASS "+label);}
    static double linear(int channel){double s=channel/255.;return s<=.04045?s/12.92:Math.pow((s+.055)/1.055,2.4);}
    static double luminance(int color){return .2126*linear((color>>16)&255)+.7152*linear((color>>8)&255)+.0722*linear(color&255);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    static int[] pixels(Bitmap bitmap){int[] p=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(p,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());return p;}
    public static void main(String[] args){
        check(AppTheme.safe(-1)==AppTheme.BLACK&&AppTheme.safe(9)==AppTheme.BLACK,"Missing or corrupt appearance preferences retain the existing black default");
        AppTheme.select(AppTheme.WHITE);check(AppTheme.light()&&AppTheme.current()==AppTheme.WHITE,"The persisted white selection selects the light palette");
        AppTheme.select(AppTheme.BLACK);check(!AppTheme.light(),"Selecting black again fully restores the dark mode");
        for(int theme=0;theme<2;theme++){
            Set<Integer> colors=new HashSet<>();boolean opaque=true,reversible=true;
            for(int role=0;role<=AppTheme.BLUE;role++){int color=AppTheme.color(theme,role);colors.add(color);opaque&=(color>>>24)==255;reversible&=AppTheme.remap(AppTheme.remap(color,theme,1-theme),1-theme,theme)==color;}
            check(colors.size()==AppTheme.BLUE+1&&opaque,"Palette "+theme+" has opaque, unambiguous UI roles for reversible recoloring");
            check(reversible,"Palette "+theme+" can switch repeatedly without losing border, accent or background roles");
            boolean legible=true;for(int bg:new int[]{AppTheme.BG,AppTheme.SURFACE})for(int fg:new int[]{AppTheme.TEXT,AppTheme.SECONDARY,AppTheme.ACCENT,AppTheme.DANGER})legible&=contrast(AppTheme.color(theme,fg),AppTheme.color(theme,bg))>=4.5;
            check(legible,"Palette "+theme+" main text, captions, accent and delete labels reach 4.5:1 contrast");
            check(contrast(AppTheme.color(theme,AppTheme.ACCENT),AppTheme.color(theme,AppTheme.ACTIVE))>=4.5,"Palette "+theme+" selected controls remain readable against their fill");
            check(contrast(AppTheme.color(theme,AppTheme.SHUTTER),AppTheme.color(theme,AppTheme.BG))>=4.5,"Palette "+theme+" shutter remains distinct against the page");
            check(contrast(AppTheme.color(theme,AppTheme.BG),AppTheme.color(theme,AppTheme.ACCENT))>=4.5,"Palette "+theme+" save buttons and selected gallery checks retain contrast");
            check(AppTheme.remap(AppTheme.OVERLAY_TEXT,theme,1-theme)==AppTheme.OVERLAY_TEXT&&AppTheme.remap(AppTheme.OVERLAY_ACCENT,theme,1-theme)==AppTheme.OVERLAY_ACCENT&&AppTheme.remap(AppTheme.OVERLAY_BG,theme,1-theme)==AppTheme.OVERLAY_BG,"Palette "+theme+" keeps photo-overlay labels legible without theme inversion");
        }
        int[] source=new int[48*36];for(int i=0;i<source.length;i++)source[i]=0xff000000|((i*23%256)<<16)|((i*7%256)<<8)|(i*31%256);
        Bitmap input=Bitmap.createBitmap(48,36,Bitmap.Config.ARGB_8888);input.setPixels(source,0,48,0,0,48,36);boolean exports=true,params=true;
        for(int stock=0;stock<FilmCatalog.NAMES.length;stock++){Film.Settings settings=new Film.Settings();settings.stock=stock;AppTheme.select(AppTheme.BLACK);int[] black=pixels(Film.develop(input,settings));float[] before=Film.parameters(settings);AppTheme.select(AppTheme.WHITE);exports&=Arrays.equals(black,pixels(Film.develop(input,settings)));params&=Arrays.equals(before,Film.parameters(settings));}
        check(exports&&params,"All 51 actual film exports and camera recipe parameters remain byte-identical across UI themes");
        check(Arrays.equals(source,pixels(input)),"Theme switching leaves original photo pixels untouched");
        AppTheme.select(AppTheme.BLACK);System.out.println("ALL "+count+" APPEARANCE / CONTRAST / EXPORT TESTS PASSED");
    }
}
