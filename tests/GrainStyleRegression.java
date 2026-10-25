import com.silverroom.camera.*;
import android.graphics.Bitmap;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class GrainStyleRegression {
    static int count;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
    static boolean near(float a,float b){return Math.abs(a-b)<.00001f;}
    static int[] pixels(Bitmap b){int[] p=new int[b.getWidth()*b.getHeight()];b.getPixels(p,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return p;}
    static Film.Settings setting(int id,float grain){Film.Settings s=new Film.Settings();s.stock=id;s.grain=grain;return s;}
    static float amount(int id){return Film.parameters(setting(id,.2f))[9];}
    static double rms(int[] a,int[] b){double sum=0;for(int i=0;i<a.length;i++){double d=(a[i]&255)-(b[i]&255);sum+=d*d;}return Math.sqrt(sum/a.length);}
    public static void main(String[] args)throws Exception{
        boolean bounded=true;for(int id=0;id<FilmCatalog.NAMES.length;id++)bounded&=FilmGrain.scale(id)>=.08f&&FilmGrain.scale(id)<=.65f;check(bounded,"All catalog looks apply a bounded style-specific grain reduction");
        check(FilmGrain.scale(-1)==FilmGrain.scale(1)&&FilmGrain.scale(999)==FilmGrain.scale(1),"Invalid style IDs use the same fallback as the color catalog");
        check(FilmGrain.effective(1,Float.NaN)==0&&FilmGrain.effective(1,-1)==0&&FilmGrain.effective(1,3)==FilmGrain.scale(1),"Corrupt or out-of-range grain values cannot amplify noise");
        check(near(amount(1),.044f)&&near(amount(24),.063f),"Warm everyday and Gold looks retain only light texture at the existing saved default");
        check(near(amount(25),.0084f)&&near(amount(37),.002f),"Ektar and Nordic looks remain exceptionally clean");
        check(near(amount(28),.174f)&&near(amount(41),.162f)&&near(amount(45),.17f),"HP5, 70s and CCD retain more character without the previous heavy amplitude");
        int[] clean={0,9,10,11,12,16,18,19,25,31,34,35,36,37,39,44,46,47,50};boolean restrained=true;for(int id:clean)restrained&=amount(id)<=.025f;check(restrained,"Clean portraits, modern color and fine-grain looks stay below subtle-noise budgets");
        boolean older=true;for(int id=0;id<FilmCatalog.NAMES.length;id++)older&=FilmGrain.effective(id,.2f)<.2f&&FilmGrain.effective(id,1)<=.65f;check(older,"Existing 20-percent preferences and manually raised grain both receive the new reduction");
        boolean curves=true;for(int id=0;id<FilmCatalog.NAMES.length;id++){float[] off=Film.parameters(setting(id,0)),on=Film.parameters(setting(id,.2f));for(int i=0;i<off.length;i++)if(i!=9)curves&=off[i]==on[i];}check(curves,"Grain restraint leaves every color curve, fade and spatial parameter independent");
        boolean off=true;for(int id=0;id<FilmCatalog.NAMES.length;id++)off&=Film.parameters(setting(id,0))[9]==0;check(off,"Setting grain to zero still disables every style's added noise");
        int w=320,h=240;int[] src=new int[w*h];Arrays.fill(src,0xff909090);Bitmap input=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);input.setPixels(src,0,w,0,0,w,h);
        boolean pixelClean=true;double largestClean=0;for(int id:clean){int[] none=pixels(Film.develop(input,setting(id,0))),light=pixels(Film.develop(input,setting(id,.2f)));largestClean=Math.max(largestClean,rms(none,light));for(int i=0;i<none.length;i++)for(int shift=0;shift<=16;shift+=8)pixelClean&=Math.abs(((none[i]>>shift)&255)-((light[i]>>shift)&255))<=1;}
        check(pixelClean&&largestClean<.5,"Actual clean-look exports add at most one 8-bit level and less than half a level RMS");
        double cleanRms=rms(pixels(Film.develop(input,setting(25,0))),pixels(Film.develop(input,setting(25,.2f)))),hp5=rms(pixels(Film.develop(input,setting(28,0))),pixels(Film.develop(input,setting(28,.2f)))),seventies=rms(pixels(Film.develop(input,setting(41,0))),pixels(Film.develop(input,setting(41,.2f))));
        check(hp5>cleanRms*3&&seventies>cleanRms*3&&hp5<2.2&&seventies<2.2,"Actual textured exports remain distinct yet bounded compared with clean Ektar");
        check(Arrays.equals(pixels(input),src),"Reducing grain never modifies original pixels in place");
        Film.Settings high=setting(28,1),zeroStrength=setting(28,1);zeroStrength.strength=0;check(Arrays.equals(pixels(Film.develop(input,zeroStrength)),src)&&Film.parameters(high)[9]>amount(28),"Manual grain remains adjustable and zero film strength still returns the source");
        if(args.length>0){File dir=new File(args[0]);dir.mkdirs();int[] ids={1,18,25,28,41,45};BufferedImage sheet=new BufferedImage(640,6*276,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setFont(new Font("Microsoft YaHei",Font.PLAIN,14));for(int row=0;row<ids.length;row++){int id=ids[row],top=row*276;for(int column=0;column<2;column++){Bitmap actual=Film.develop(input,setting(id,column==0?0:.2f));BufferedImage tile=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);tile.setRGB(0,0,w,h,pixels(actual),0,w);g.drawImage(tile,column*w,top,null);}g.setColor(new Color(20,26,22));g.fillRect(0,top+h,640,36);g.setColor(new Color(237,226,205));g.drawString(FilmCatalog.NAMES[id]+" · 无颗粒",10,top+h+24);g.drawString("新版默认颗粒",330,top+h+24);}g.dispose();ImageIO.write(sheet,"png",new File(dir,"light-grain-export-check.png"));}
        System.out.println("Default noise RMS: clean="+cleanRms+", HP5="+hp5+", 70s="+seventies+"; max clean="+largestClean);
        System.out.println("ALL "+count+" STYLE GRAIN RESTRAINT TESTS PASSED");
    }
}
