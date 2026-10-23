import com.silverroom.camera.*;
import android.graphics.Bitmap;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public final class NewStylesRegression {
    static int count;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
    static boolean near(float a,float b){return Math.abs(a-b)<.00001f;}
    static Bitmap image(int w,int h,int[] pixels){Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.setPixels(pixels,0,w,0,0,w,h);return b;}
    static int[] pixels(Bitmap b){int[] p=new int[b.getWidth()*b.getHeight()];b.getPixels(p,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return p;}
    static Film.Settings recipe(int id){Film.Settings s=new Film.Settings();s.stock=id;s.grain=s.fade=0;return s;}
    static float shaderLeak(float x,float y,float strength){float a=Math.max(0,1-x/.42f),b=Math.max(0,1-(1-x)/.20f);return strength*(a*a*a+.30f*b*b*b)*(.8f+.2f*(1-2*Math.abs(y-.5f)));}
    public static void main(String[] args)throws Exception{
        String[] added={"Kodak Gold 200","Kodak Ektar 100","Kodak Kodachrome","CineStill 800T","Ilford HP5","Polaroid 宝丽来","王家卫风格","韦斯·安德森风格","诺兰电影风","港风电影","韩国电影风","莫兰迪色系","德系冷调","北欧极简","暗调 Moody","高调 High Key","低调 Low Key","70 年代美式","80 年代港风","90 年代日系","Y2K 千禧风","CCD 数码复古","直闪 Flash","杂志 Editorial","街头纪实","胶片漏光","移轴微缩"};
        check(Arrays.equals(Arrays.copyOfRange(FilmCatalog.NAMES,24,51),added),"All twenty-seven requested names have stable appended IDs");
        int start=24;int[] lengths={6,5,6,5,5};for(int i=0;i<lengths.length;i++){java.util.List<Integer> ids=FilmGroups.ids(i+5);check(ids.size()==lengths[i]&&ids.get(0)==start&&ids.get(ids.size()-1)==start+lengths[i]-1,"New family "+FilmGroups.LABELS[i+5]+" contains exactly its requested styles");start+=lengths[i];}
        check(FilmGroups.libraryLabels().length==12&&FilmGroups.libraryLabels()[7].equals("经典胶片")&&FilmGroups.libraryLabels()[11].equals("特色摄影"),"Library tabs expose all new categories after the existing tabs");
        boolean tabs=true;for(int g=1;g<FilmGroups.LABELS.length;g++)tabs&=FilmGroups.libraryGroup(g+2)==g&&FilmGroups.SHORT_LABELS[g].length()<=2;check(tabs,"Library category mapping and compact reel labels stay consistent");
        check(FilmGroups.choose(9,24,49)==49&&FilmGroups.choose(5,50,24)==24,"Main category switch restores each family's last style");
        boolean compactNames=true;for(int id=24;id<51;id++)compactNames&=FilmCatalog.reelName(id).length()<=10;check(compactNames&&FilmCatalog.reelName(24).equals("Gold 200"),"Long requested names use readable compact reel titles while the library retains full names");
        check(FilterShelf.matches(24,"柯达")&&FilterShelf.matches(25,"EKTAR")&&FilterShelf.matches(27,"800t")&&FilterShelf.matches(28,"伊尔福")&&FilterShelf.matches(29,"拍立得"),"Classic stocks are searchable by English names and Chinese aliases");
        check(FilterShelf.matches(30,"wong")&&FilterShelf.matches(31,"wes")&&FilterShelf.matches(32,"nolan")&&FilterShelf.matches(38,"moody")&&FilterShelf.matches(50,"tilt"),"Director and modern-effect English aliases are searchable");
        check(FilterShelf.matches(42,"80年代")&&FilterShelf.matches(42,"香港")&&FilterShelf.matches(43,"90年代"),"Era search accepts compact year aliases without requiring spaces");
        check(FilterShelf.matches(50,"特色摄影")&&!FilterShelf.matches(50,"经典胶片"),"Category search matches only its member recipes");
        check(FilterShelf.parse("50,49,28,24",true).equals(Arrays.asList(50,49,28,24))&&FilterShelf.remember(Arrays.asList(23,6),50).get(0)==50,"Favorites and recent history preserve the new maximum style ID");
        FilmWheelState wheel=new FilmWheelState();wheel.reset(FilmGroups.ids(0),50);wheel.position(50);check(wheel.count()==51&&wheel.selected()==50,"The all-film reel reaches every one of the fifty-one recipes");
        float[] hp5=Film.parameters(recipe(28));check(hp5[6]==0&&hp5[7]<Film.parameters(recipe(23))[7],"HP5 uses neutral medium-contrast monochrome rather than hard black");
        check(Film.parameters(recipe(39))[22]==0&&Film.parameters(recipe(40))[22]>.2f,"High-key and low-key use distinct edge shading as well as tone curves");
        check(Film.parameters(recipe(49))[20]>0&&Film.parameters(recipe(50))[21]>0&&Film.parameters(recipe(45))[23]==32,"Leak, tilt and CCD recipes request their actual spatial/digital operations");
        boolean legacy=true;for(int i=0;i<24;i++){float[] p=Film.parameters(recipe(i));legacy&=p[20]==0&&p[21]==0&&p[22]==.10f&&p[23]==0;}check(legacy,"Existing twenty-four recipes retain their spatial processing defaults");
        check(near(FilmEffects.tilt(.5f),0)&&near(FilmEffects.tilt(.38f),0)&&near(FilmEffects.tilt(.08f),1)&&near(FilmEffects.tilt(.2f),FilmEffects.tilt(.8f)),"Tilt keeps a sharp central band and symmetric soft edges");
        boolean parity=true;for(int y=0;y<=10;y++)for(int x=0;x<=10;x++)parity&=near(FilmEffects.leak(x/10f,y/10f,.88f),shaderLeak(x/10f,y/10f,.88f));check(parity,"Export light-leak math matches the GPU expression across the frame");
        check(FilmEffects.leak(0,.5f,1)>FilmEffects.leak(.5f,.5f,1)&&FilmEffects.leak(.5f,.5f,1)==0,"Leak illuminates the frame edges without washing out the center");
        check(FilmEffects.finish(.25f,0,0,.5f,.88f,0)>FilmEffects.finish(.25f,2,0,.5f,.88f,0),"The procedural leak is warm rather than a uniform white overlay");
        int[] solid={0xff5278a3,0xff5278a3,0xff5278a3};check(FilmEffects.blurred(solid,solid,solid,0,100)==solid[0]&&FilmEffects.blurred(solid,solid,solid,2,100)==solid[2],"Blur clamps both borders and preserves a constant image");
        check(FilmEffects.radius(1)==1&&FilmEffects.radius(4096)==49,"Spatial blur uses normalized radii with safe tiny-image behavior");
        boolean geometry=true;for(int sensor:new int[]{0,90,180,270})for(int[] viewport:new int[][]{{1440,1920},{1920,1440},{1000,1000}})for(float crop:new float[]{1,50/23f,230/85f}){float[] m=new float[16];YuvGeometry.model(m,sensor,0,false,4000,3000,viewport[0],viewport[1]);float det=m[0]*m[5]-m[4]*m[1],dx=m[5]*.012f/(det*crop),dy=-m[1]*.012f/(det*crop);geometry&=near((m[0]*dx+m[4]*dy)*crop,.012f)&&near((m[1]*dx+m[5]*dy)*crop,0);dx=-m[4]*.012f/(det*crop);dy=m[0]*.012f/(det*crop);geometry&=near((m[0]*dx+m[4]*dy)*crop,0)&&near((m[1]*dx+m[5]*dy)*crop,.012f);}check(geometry,"GPU blur offsets preserve screen axes across sensor rotations, frame ratios and tele crops");
        check(FilmEffects.blend(0xff123456,0xffabcdef,0)==0xff123456&&FilmEffects.blend(0xff123456,0xffabcdef,1)==0xffabcdef,"Tilt blending preserves both endpoint colors exactly");
        int w=240,h=180;int[] source=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)source[y*w+x]=((x/2+y/2)%2==0)?0xff202020:0xffe0e0e0;
        Bitmap input=image(w,h,source);int[] tilted=pixels(Film.develop(input,recipe(50)));int center=Math.abs((tilted[h/2*w+w/2]&255)-(tilted[h/2*w+w/2+2]&255)),edge=Math.abs((tilted[10*w+w/2]&255)-(tilted[10*w+w/2+2]&255));
        check(center>edge*1.5f,"Actual exported tilt-shift pixels retain center detail and soften edge texture");
        check(Arrays.equals(source,pixels(input)),"Developing all spatial effects never mutates the private original");
        int[] neutral=new int[w*h];Arrays.fill(neutral,0xff666666);int[] leaked=pixels(Film.develop(image(w,h,neutral),recipe(49)));check(((leaked[h/2*w]>>16)&255)>((leaked[h/2*w+w/2]>>16)&255)+40,"Actual exported leak visibly brightens the red edge");
        Set<Integer> hashes=new HashSet<>();boolean bounded=true;for(int id=24;id<51;id++){Bitmap developed=Film.develop(input,recipe(id));int[] p=pixels(developed);hashes.add(Arrays.hashCode(p));bounded&=developed.getWidth()==w&&developed.getHeight()==h;for(int pixel:p)bounded&=(pixel>>>24)==255;}
        check(hashes.size()==27&&bounded,"All twenty-seven exported styles differ while retaining dimensions and opaque pixels");
        boolean gray=true;for(int pixel:pixels(Film.develop(input,recipe(28))))gray&=((pixel>>16)&255)==((pixel>>8)&255)&&((pixel>>8)&255)==(pixel&255);check(gray,"Actual HP5 export stays neutral in every channel");
        boolean tiny=true;for(int id=24;id<51;id++)tiny&=pixels(Film.develop(image(1,1,new int[]{0xff888888}),recipe(id))).length==1;check(tiny,"All new recipes safely develop a one-pixel imported photo");
        if(args.length>0){File dir=new File(args[0]);dir.mkdirs();int sw=320,sh=240;BufferedImage scene=new BufferedImage(sw,sh,BufferedImage.TYPE_INT_RGB);Graphics2D g=scene.createGraphics();g.setPaint(new GradientPaint(0,0,new Color(70,144,202),0,sh,new Color(243,195,135)));g.fillRect(0,0,sw,sh);g.setColor(new Color(246,226,172));g.fillOval(245,18,42,42);g.setColor(new Color(52,88,69));g.fillPolygon(new int[]{0,95,170,260,320,320,0},new int[]{135,60,126,85,132,240,240},7);g.setColor(new Color(191,81,58));g.fillRect(72,133,114,86);g.setColor(new Color(229,197,151));g.fillRect(97,143,30,46);g.setColor(new Color(31,57,66));for(int x=135;x<180;x+=18)g.fillRect(x,145,12,24);g.setColor(new Color(216,184,127));g.fillPolygon(new int[]{140,166,240,75},new int[]{195,195,240,240},4);g.setColor(new Color(228,172,136));g.fillOval(224,129,25,30);g.setColor(new Color(90,98,151));g.fillRect(219,156,36,63);g.setColor(new Color(223,215,185));g.setFont(new Font("SansSerif",Font.BOLD,13));g.drawString("FANCY CAMERA",12,26);g.dispose();int[] scenePixels=scene.getRGB(0,0,sw,sh,null,0,sw);Bitmap demo=image(sw,sh,scenePixels);ImageIO.write(scene,"png",new File(dir,"source.png"));
            BufferedImage sheet=new BufferedImage(sw*3,(sh+56)*9,BufferedImage.TYPE_INT_RGB);g=sheet.createGraphics();g.setFont(new Font("Microsoft YaHei",Font.PLAIN,14));for(int id=24;id<51;id++){int left=(id-24)%3*sw,top=(id-24)/3*(sh+56);Film.Settings setting=recipe(id);setting.grain=.2f;setting.fade=.1f;Bitmap result=Film.develop(demo,setting);BufferedImage tile=new BufferedImage(sw,sh,BufferedImage.TYPE_INT_RGB);tile.setRGB(0,0,sw,sh,pixels(result),0,sw);g.drawImage(tile,left,top,null);g.setColor(new Color(20,26,22));g.fillRect(left,top+sh,sw,56);g.setColor(new Color(238,226,205));g.drawString(FilmCatalog.NAMES[id],left+10,top+sh+23);g.setColor(new Color(180,176,155));g.drawString(FilmGroups.LABELS[FilmGroups.family(id)],left+10,top+sh+44);}g.dispose();ImageIO.write(sheet,"png",new File(dir,"new-styles-contact-sheet.png"));}
        System.out.println("ALL "+count+" NEW STYLE / SPATIAL EXPORT TESTS PASSED");
    }
}
