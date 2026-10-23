import com.silverroom.camera.*;
import java.util.*;

public final class FastPreviewRegression {
    static int count;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
    static byte[] frame(int tw,int th,int count){int columns=Math.min(3,count),width=tw*columns,height=th*((count+columns-1)/columns);return new byte[width*height*4];}
    static void pixel(byte[] bytes,int width,int x,int y,int rgb){int i=(y*width+x)*4;bytes[i]=(byte)(rgb>>16);bytes[i+1]=(byte)(rgb>>8);bytes[i+2]=(byte)rgb;bytes[i+3]=(byte)255;}
    public static void main(String[] args){
        PreviewFlow flow=new PreviewFlow();check(flow.acquire(0)==0,"A disabled preview cannot schedule GPU work");flow.enable(true);long first=flow.acquire(0);check(first>0&&flow.valid(first),"Enabling admits a fresh frame immediately");
        check(flow.acquire(1000)==0,"One pending UI commit prevents a queue of camera frames");flow.complete(first);check(flow.acquire(99)==0,"GPU preview admission respects the 100ms rate limit");long second=flow.acquire(100);check(second>first,"Completed GPU previews can admit ten fresh frames per second");
        flow.enable(false);check(!flow.valid(second)&&flow.acquire(10000)==0,"Turning the switch off invalidates in-flight images and stops new work");flow.enable(true);long resumed=flow.acquire(101);check(resumed>second&&flow.valid(resumed),"Turning the switch back on starts a new immediate generation");
        flow.complete(second);check(flow.valid(resumed)&&flow.acquire(2000)==0,"An old completion cannot unlock or replace the newly resumed frame");flow.complete(resumed);check(flow.acquire(201)>resumed,"A valid completion releases exactly its own GPU frame");flow.close();flow.enable(true);check(flow.acquire(9000)==0,"Closing the dialog permanently stops its preview generation");
        check(PreviewFlow.current(1000,1450)&&!PreviewFlow.current(1000,1451),"Stale results beyond 450ms are rejected instead of displayed late");check(!PreviewFlow.current(1000,900),"An invalid backwards timestamp cannot be presented as a fresh frame");
        check(Arrays.equals(PreviewAtlas.styles(new int[]{6,6,7,-1,999},1),new int[]{6,7,1}),"Visible recipe plans are valid unique and include the histogram recipe once");check(Arrays.equals(PreviewAtlas.styles(new int[]{1,4},1),new int[]{1,4}),"Sharing a visible recipe does not duplicate histogram rendering");
        int[] ids=new int[FilmCatalog.NAMES.length];for(int i=0;i<ids.length;i++)ids[i]=i;check(PreviewAtlas.styles(ids,4).length==ids.length,"The full film overview fits in one GPU atlas without truncating new recipes");
        byte[] bytes=frame(2,2,6);int width=6;for(int i=0;i<6;i++)for(int y=0;y<2;y++)for(int x=0;x<2;x++)pixel(bytes,width,(i%3)*2+x,(i/3)*2+y,((i+1)<<16)|((y==0?20:220)<<8)|(x+1));
        PreviewAtlas atlas=new PreviewAtlas(2,2,new int[]{0,1,2,3,4,5},bytes,1234);check(atlas.width==6&&atlas.height==4&&atlas.columns==3,"GPU tiles preserve their row stride and packed atlas dimensions");
        int[] top=atlas.pixels(0);check(top[0]==0xff01dc01&&top[2]==0xff011401,"Each tile independently flips GL rows into upright Android pixels");check(atlas.pixels(3)[0]==0xff04dc01&&atlas.pixels(5)[1]==0xff06dc02,"Second-row and right-edge tiles have no neighboring recipe bleed");
        int[] scratch=new int[4];atlas.copyPixels(1,scratch);int[] saved=scratch.clone();atlas.copyPixels(2,scratch);check(saved[0]==0xff02dc01&&scratch[0]==0xff03dc01,"One decode scratch array can be reused safely across GPU thumbnails");
        byte[] rgba=frame(2,2,1);pixel(rgba,2,0,0,0x0000ff);pixel(rgba,2,1,0,0xffffff);pixel(rgba,2,0,1,0xff0000);pixel(rgba,2,1,1,0x00ff00);PreviewAtlas colors=new PreviewAtlas(2,2,new int[]{6},rgba,0);int[][] histogram=Histogram.atlas(colors,0);
        boolean totals=true;for(int[] channel:histogram){int sum=0;for(int n:channel)sum+=n;totals&=sum==4;}check(totals,"RGB and luma histograms count every sampled pixel exactly once");check(histogram[0][0]==2&&histogram[0][63]==2&&histogram[1][0]==2&&histogram[2][63]==2,"RGB histogram axes retain black and white endpoint bins");check(histogram[3][19]==1&&histogram[3][37]==1&&histogram[3][7]==1&&histogram[3][63]==1,"Luma histogram follows weighted display brightness");
        int[][] tileHistogram=Histogram.atlas(atlas,4);check(tileHistogram[0][1]==4&&tileHistogram[2][0]==4,"A shared histogram reads only its chosen tile rather than the complete atlas");
        boolean rejected=false;try{new PreviewAtlas(2,2,new int[]{0},new byte[3],0);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Truncated atlas readbacks are rejected before image decoding");
        rejected=false;try{atlas.pixels(6);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"An unavailable tile index cannot read adjacent GPU memory");
        rejected=false;try{atlas.copyPixels(0,new int[1]);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"An undersized decode buffer cannot corrupt the following tile");
        int[] thumb=PreviewPixels.size(1440,1920,128);check(Arrays.equals(thumb,new int[]{96,128}),"An explicitly requested low-cost thumbnail preserves its 128px long edge");check(Arrays.equals(PreviewPixels.size(1440,1920,96),new int[]{72,96}),"The standalone histogram uses a cheap 96px sample");
        byte[] partial=frame(1,1,4);for(int i=0;i<4;i++)pixel(partial,3,i%3,i/3,(i+1)*0x10101);PreviewAtlas tail=new PreviewAtlas(1,1,new int[]{1,3,5,7},partial,0);check(tail.pixels(3)[0]==0xff040404&&tail.index(7)==3,"A partial final atlas row maps its remaining recipe correctly");
        System.out.println("ALL "+count+" FAST PREVIEW / HISTOGRAM TESTS PASSED");
    }
}

