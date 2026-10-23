import com.silverroom.camera.*;
import java.util.*;
import java.nio.file.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public final class PhotoFeaturesRegression {
    static int count;
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);count++;System.out.println("PASS "+text);}
    static float shaderChannel(float value,int ch,float[] p){
        float c=(value-.5f)*p[7]+.5f;c=c*p[ch]+p[ch+3];c=c*(1-p[8]*.35f)+p[8]*.16f;
        float k=Math.max(0,Math.min(1,c));c+=p[ch+10]*(1-k)*(1-k)+p[ch+13]*k*k;
        if(c>=.65f){float hi=Math.max(c-.65f,0);c=.65f+hi/(1+p[16]*hi);}return c;
    }
    static int clip(float v){return Math.max(0,Math.min(255,Math.round(v*255)));}
    public static void main(String[] args)throws Exception{
        int[] rear={90,180,270,0},front={270,180,90,0};
        for(int i=0;i<4;i++){
            check(new CaptureOrientation.Snapshot(90,i*90,false).rotation==rear[i],"Rear sensor 90 / phone "+i*90+" uses correct capture rotation");
            check(new CaptureOrientation.Snapshot(270,i*90,true).rotation==front[i],"Front sensor 270 / phone "+i*90+" reverses phone angle correctly");
        }
        check(CaptureOrientation.update(44,0)==0&&CaptureOrientation.update(54,0)==0,"Near-diagonal hand movement does not chatter between portrait and landscape");
        check(CaptureOrientation.update(56,0)==90&&CaptureOrientation.update(34,90)==0,"Crossing hysteresis threshold switches orientation in either direction");
        check(CaptureOrientation.update(359,0)==0&&CaptureOrientation.update(1,0)==0,"Zero-degree wraparound preserves upright pose");
        check(CaptureOrientation.update(-1,270)==270,"Phone held flat keeps the last valid orientation");
        CaptureOrientation.Snapshot shutter=new CaptureOrientation.Snapshot(90,270,false);int later=CaptureOrientation.update(0,270);
        check(later==0&&shutter.device==270&&shutter.rotation==0,"Moving the phone after shutter does not change that photo's orientation snapshot");
        check(shutter.comment().contains("device_degrees=270")&&shutter.comment().contains("pixels_upright=true"),"Saved metadata describes the captured pose and normalized pixels");
        check(Arrays.equals(Arrays.copyOf(FilmCatalog.NAMES,6),new String[]{"原生负片","暖金 400","青橙电影","樱桃 200","银盐黑白","午夜 800"}),"Legacy negative recipe IDs retain their original names and order");
        boolean finite=true,parity=true;java.util.Set<String> looks=new java.util.HashSet<>();
        for(int id=0;id<FilmCatalog.NAMES.length;id++){
            float[] p=FilmCatalog.parameters(id,.1f,-.2f,.15f,.3f,0,.1f);looks.add(Arrays.toString(p));
            for(int c=0;c<3;c++)for(float x:new float[]{-.1f,0,.01f,.25f,.5f,.65f,.8f,1,1.2f}){
                float y=FilmCatalog.channel(x,c,p);finite&=Float.isFinite(y);parity&=Math.abs(y-shaderChannel(x,c,p))<.00001;
            }
        }
        check(finite,"Every recipe produces finite channel values across shadows and highlights");
        check(parity,"Export tone curves match the actual GPU shader's scalar operations");
        check(looks.size()==FilmCatalog.NAMES.length,"Every recipe has distinct color/tone parameters");
        check(FilmCatalog.NAMES.length==FilmCatalog.TAGS.length&&FilmCatalog.NAMES.length==FilmCatalog.DESCRIPTIONS.length&&FilmCatalog.NAMES.length==FilmCatalog.COLORS.length,"Picker labels, scenes and swatches cover every recipe");
        java.util.List<Integer> pins=FilterShelf.parse("6,7,1,4",true);
        java.util.List<Integer> quick=FilterShelf.visible(pins,13);
        check(quick.size()==4&&quick.contains(13)&&pins.contains(4),"Choosing a hidden recipe exposes it immediately without deleting saved favorites");
        check(FilterShelf.visible(pins,7).equals(pins),"Selecting a favorite restores the saved four shortcuts");
        java.util.List<Integer> recent=FilterShelf.remember(Arrays.asList(6,7,6,1,2,3,4),7);
        check(recent.get(0)==7&&new java.util.HashSet<>(recent).size()==recent.size()&&recent.size()<=6,"Recent menu is newest-first, unique and bounded");
        java.util.List<Integer> changed=FilterShelf.toggle(pins,13);
        check(changed.size()==5&&changed.get(4)==13&&FilterShelf.parse(FilterShelf.encode(changed),true).equals(changed),"Pinning appends without evicting saved favorites and survives preferences round-trip");
        check(FilterShelf.parse("99,-1,no,6,6,7",false).equals(Arrays.asList(6,7)),"Old or corrupt menu preferences cannot index invalid recipes");
        check(FilterShelf.matches(6," CC ")&&FilterShelf.matches(7,"cn")&&FilterShelf.matches(12,"人像")&&FilterShelf.matches(13,"ETERNA"),"Common abbreviations, Chinese scenes and English names are searchable");
        if(args.length>0){
            BufferedImage sheet=new BufferedImage(720,FilmCatalog.NAMES.length*52,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setFont(new Font("SansSerif",Font.PLAIN,12));
            int[][] colors={{206,157,133},{236,198,178},{166,105,81},{52,112,68},{27,85,155},{191,56,49},{221,190,65},{0,0,0},{255,255,255}};
            for(int id=0;id<FilmCatalog.NAMES.length;id++){
                int top=id*52;g.setColor(new Color(21,26,23));g.fillRect(0,top,720,52);g.setColor(new Color(232,225,205));g.drawString(id+"  "+FilmCatalog.TAGS[id],8,top+28);float[] p=FilmCatalog.parameters(id,0,0,0,0,0,0);
                for(int x=210;x<720;x++)for(int y=top+4;y<top+48;y++){
                    float r,gr,b;if(y<top+24){r=gr=b=(x-210)/509f;}else{int[] rgb=colors[Math.min(colors.length-1,(x-210)*colors.length/510)];r=rgb[0]/255f;gr=rgb[1]/255f;b=rgb[2]/255f;}
                    r=FilmCatalog.channel(r,0,p);gr=FilmCatalog.channel(gr,1,p);b=FilmCatalog.channel(b,2,p);float l=.299f*r+.587f*gr+.114f*b;
                    sheet.setRGB(x,y,(clip((l+(r-l)*p[6])*p[17])<<16)|(clip((l+(gr-l)*p[6])*p[18])<<8)|clip((l+(b-l)*p[6])*p[19]));
                }
            }g.dispose();Path output=Paths.get(args[0]);Files.createDirectories(output.getParent());ImageIO.write(sheet,"png",output.toFile());
        }
        System.out.println("ALL "+count+" CAPTURE ORIENTATION / FILM FEATURE TESTS PASSED");
    }
}
