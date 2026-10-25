import com.silverroom.camera.*;
import android.graphics.Bitmap;
import java.util.*;
import java.nio.file.*;
import java.io.File;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Exercises actual preview/export recipes, stable persisted IDs, and expanded atlas limits. */
public final class HasselbladCinemaRegression {
    static int count;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);count++;System.out.println("PASS "+message);}
    static int[] pixels(Bitmap b){int[] out=new int[b.getWidth()*b.getHeight()];b.getPixels(out,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return out;}
    static Film.Settings settings(int id){Film.Settings s=new Film.Settings();s.stock=id;s.grain=0;s.fade=.1f;return s;}
    public static void main(String[] args)throws Exception{
        check(FilmCatalog.NAMES.length==77&&FilmCatalog.TAGS.length==77&&FilmCatalog.DESCRIPTIONS.length==77&&FilmCatalog.COLORS.length==77&&FilmLabelStyle.count()==77,"All 77 color recipes have matching metadata and original wrapper palettes");
        check(FilmGroups.FAVORITES==10&&FilmGroups.HASSELBLAD==11,"New Hasselblad group preserves the saved favorites/category IDs");
        check(FilmGroups.ids(FilmGroups.HASSELBLAD).equals(Arrays.asList(51,52,53,54,55,56)),"Six Hasselblad inspirations belong to their own family");
        check(FilmGroups.ids(FilmGroups.CLASSIC).size()==12&&FilmGroups.ids(FilmGroups.CINEMA).size()==19,"Classic and cinema groups include the old and appended looks");
        Set<Integer> groups=new HashSet<>();for(int id:FilmGroups.order(false))groups.add(id);
        check(groups.size()==11&&groups.contains(11)&&!groups.contains(10),"Empty favorites hides only favorites; Hasselblad remains reachable");
        int[] order=FilmGroups.order(true);check(order[4]==11&&order[order.length-1]==10&&new HashSet<Integer>(){{for(int g:order)add(g);}}.size()==12,"Category reel places Hasselblad next to camera brands and favorites last");
        Set<Integer> tabs=new HashSet<>();for(int i=3;i<FilmGroups.libraryLabels().length;i++)tabs.add(FilmGroups.libraryGroup(i));
        check(tabs.equals(groupsWithoutAll(groups)),"Library tabs cover every real family exactly once");
        check(FilmGroups.choose(11,24,54)==54&&FilmGroups.choose(11,52,54)==52&&FilmGroups.choose(11,24,24)==51,"Hasselblad group restores valid last selections without taking an unrelated recipe");
        check(FilterShelf.matches(51,"hasselblad")&&FilterShelf.matches(56,"哈苏")&&FilterShelf.matches(57,"portra")&&FilterShelf.matches(57,"柯达")&&FilterShelf.matches(61,"富士")&&FilterShelf.matches(64,"blade runner")&&FilterShelf.matches(63,"dune")&&FilterShelf.matches(76,"joker"),"New brands and movie recipes are searchable by Chinese and English names");
        check(!FilterShelf.matches(57,"哈苏")&&!FilterShelf.matches(51,"leica"),"Brand search does not misclassify other groups");
        check(FilterShelf.parse("76,51,6,24",true).equals(Arrays.asList(76,51,6,24))&&FilterShelf.remember(Arrays.asList(6,24),76).equals(Arrays.asList(76,6,24)),"New final recipe IDs persist through favorites and recent history");
        check(FilmLabelStyle.of(57).iso()==400&&FilmLabelStyle.of(58).iso()==800&&FilmLabelStyle.of(60).iso()==200&&FilmLabelStyle.of(62).iso()==100,"Added real-stock profiles expose their actual nominal ISO");
        check(FilmLabelStyle.of(64).iso()==0&&FilmLabelStyle.of(65).iso()==0&&FilmLabelStyle.of(51).iso()==0,"Movie years and camera-brand looks are not interpreted as film ISO");
        check(FilmLabelStyle.of(56).type().equals("MONOCHROME")&&FilmLabelStyle.of(70).type().equals("MONOCHROME")&&FilmLabelStyle.of(62).type().equals("REVERSAL")&&FilmLabelStyle.of(63).type().equals("CINEMA"),"Black-and-white, reversal, and movie wrapper classifications are accurate");
        boolean metadata=true,curves=true,grain=true;Set<String> names=new HashSet<>();
        for(int i=0;i<77;i++){metadata&=names.add(FilmCatalog.NAMES[i])&&!FilmCatalog.DESCRIPTIONS[i].isEmpty()&&FilmCatalog.reelName(i).length()<=12;float[] p=Film.parameters(settings(i));for(float v:p)curves&=Float.isFinite(v);for(int c=0;c<3;c++){float previous=-100;for(int x=0;x<=255;x++){float v=FilmCatalog.channel(x/255f,c,p);curves&=Float.isFinite(v)&&v>=previous;previous=v;}}if(i>=51){Film.Settings defaults=settings(i);defaults.grain=.2f;grain&=Film.parameters(defaults)[9]<=.05f;}}
        check(metadata,"Every recipe has a unique full name, short reel label, and description");check(curves,"Every curve is finite and monotonic for each RGB input channel");check(grain,"All new default recipes remain under a subtle added-grain budget");
        boolean clean=true;for(int i=51;i<=56;i++){Film.Settings s=settings(i);s.grain=.2f;clean&=Film.parameters(s)[9]<=.01f;}
        check(clean,"Hasselblad inspirations keep especially fine texture");
        FilmWheelState wheel=new FilmWheelState();wheel.reset(FilmGroups.ids(0),76);wheel.position(76);check(wheel.count()==77&&wheel.selected()==76,"All-film reel reaches the newly appended final style");
        int[] ids=new int[77];for(int i=0;i<ids.length;i++)ids[i]=i;int[] layout=PreviewAtlas.layout(1024,1024,77,224);int h=layout[1]*((77+layout[2]-1)/layout[2]);PreviewAtlas atlas=new PreviewAtlas(layout[0],layout[1],ids,new byte[layout[0]*layout[2]*h*4],0);
        check(atlas.width<=2048&&atlas.height<=2048&&atlas.tileWidth==224&&atlas.index(76)==76&&atlas.pixels(76).length==224*224,"77-recipe atlas retains full 224px tiles within the existing GPU texture limit");
        int w=96,hp=64;int[] input=new int[w*hp];for(int y=0;y<hp;y++)for(int x=0;x<w;x++){int r=(x*255)/(w-1),g=(y*255)/(hp-1),b=((x+y)*255)/(w+hp-2);input[y*w+x]=0xff000000|r<<16|g<<8|b;}
        Bitmap bitmap=Bitmap.createBitmap(w,hp,Bitmap.Config.ARGB_8888);bitmap.setPixels(input,0,w,0,0,w,hp);Set<Integer> hashes=new HashSet<>();boolean zero=true,mono=true;
        for(int i=51;i<77;i++){Film.Settings s=settings(i);int[] out=pixels(Film.develop(bitmap,s));hashes.add(Arrays.hashCode(out));if(i==56||i==70)for(int v:out)mono&=(v>>16&255)==(v>>8&255)&&(v>>8&255)==(v&255);s.strength=0;zero&=Arrays.equals(input,pixels(Film.develop(bitmap,s)));}
        check(hashes.size()==26,"All 26 additions produce distinct actual exported pixels on a color-and-tone grid");check(zero,"Zero strength returns exact originals for every appended recipe");check(mono,"Hasselblad and Oppenheimer monochrome exports keep neutral RGB channels");check(Arrays.equals(input,pixels(bitmap)),"Developing new looks does not modify the original bitmap");
        File dir=new File(args[0]);dir.mkdirs();BufferedImage scene=ImageIO.read(new File(args[1],"build/style-checks/source.png"));Bitmap demo=Bitmap.createBitmap(scene.getWidth(),scene.getHeight(),Bitmap.Config.ARGB_8888);demo.setPixels(scene.getRGB(0,0,scene.getWidth(),scene.getHeight(),null,0,scene.getWidth()),0,scene.getWidth(),0,0,scene.getWidth(),scene.getHeight());
        BufferedImage sheet=new BufferedImage(1200,7*270,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(0x151518));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());g.setFont(new Font("Microsoft YaHei",0,14));
        for(int i=51;i<77;i++){Bitmap result=Film.develop(demo,settings(i));BufferedImage tile=new BufferedImage(scene.getWidth(),scene.getHeight(),BufferedImage.TYPE_INT_RGB);tile.setRGB(0,0,tile.getWidth(),tile.getHeight(),pixels(result),0,tile.getWidth());int x=(i-51)%4*300,y=(i-51)/4*270;g.drawImage(tile,x+4,y+4,292,219,null);g.setColor(Color.WHITE);g.drawString(FilmCatalog.NAMES[i],x+10,y+243);g.setColor(new Color(0xaaaaaa));g.drawString(FilmGroups.LABELS[FilmGroups.family(i)],x+10,y+263);}g.dispose();ImageIO.write(sheet,"png",new File(dir,"new-looks-contact-sheet.png"));
        System.out.println("ALL "+count+" HASSELBLAD / CINEMA TESTS PASSED");
    }
    static Set<Integer> groupsWithoutAll(Set<Integer> source){Set<Integer> copy=new HashSet<>(source);copy.remove(0);return copy;}
}
