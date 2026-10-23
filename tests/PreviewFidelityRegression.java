import com.silverroom.camera.*;
import java.nio.*;
import java.util.*;

public final class PreviewFidelityRegression {
    static int count;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
    static void pixel(byte[] bytes,int width,int x,int y,int r,int g,int b){int i=(y*width+x)*4;bytes[i]=(byte)r;bytes[i+1]=(byte)g;bytes[i+2]=(byte)b;bytes[i+3]=(byte)255;}
    public static void main(String[] args){
        PreviewFidelity quality=new PreviewFidelity();check(quality.edge()==320,"Opening the library starts at the sharper overview resolution");
        quality.mode(true);check(quality.edge()==512,"Detailed viewing requests a 512px long edge");
        quality.late();check(quality.edge()==512,"A single delayed frame cannot immediately degrade preview clarity");quality.late();check(quality.edge()==384,"Two consecutive delayed frames lower quality by one bounded step");
        for(int i=0;i<20;i++)quality.late();check(quality.edge()==320,"Detailed-view fallback never falls below the 320px clarity floor");
        for(int i=0;i<12;i++)quality.fresh();check(quality.edge()==352,"Sustained fresh frames recover detail rather than staying permanently blurry");
        for(int i=0;i<120;i++)quality.fresh();check(quality.edge()==512,"Quality recovery stops at the requested detailed resolution");
        quality.mode(false);for(int i=0;i<20;i++)quality.late();check(quality.edge()==192,"Overview fallback remains above the previous 128px default");
        for(int i=0;i<120;i++)quality.fresh();check(quality.edge()==320,"Overview returns to its full resolution when processing catches up");
        quality.mode(true);quality.late();quality.mode(false);quality.late();check(quality.edge()==320,"Changing layout resets old delay history and the resolution target");
        check(Arrays.equals(PreviewPixels.size(1440,1920),new int[]{240,320}),"Default portrait thumbnails carry 320px along the long edge");
        check(Arrays.equals(PreviewPixels.size(1440,1920,512),new int[]{384,512}),"Detailed portrait thumbnails preserve their aspect at 512px");
        check(Arrays.equals(PreviewPixels.size(64,32,512),new int[]{64,32}),"Small imported photos are never upscaled into invented detail");
        int[] overview=PreviewAtlas.layout(1600,1600,24,320);check(Arrays.equals(overview,new int[]{320,320,4}),"Twenty-four sharper overview tiles repack into four GPU columns");
        int[] detail=PreviewAtlas.layout(1600,1600,6,512);check(Arrays.equals(detail,new int[]{512,512,3}),"A normal visible detailed batch retains its full 512px resolution");
        int[] crowded=PreviewAtlas.layout(1600,1600,24,512);check(crowded[0]>=384&&crowded[0]*crowded[2]<=2048&&crowded[1]*((24+crowded[2]-1)/crowded[2])<=2048,"A crowded detailed batch is bounded by the 2048px atlas budget");
        boolean bounded=true;for(int n=1;n<=FilmCatalog.NAMES.length;n++)for(int edge:new int[]{96,192,320,512})for(int[] source:new int[][]{{1440,1920},{1920,1080},{2048,2048},{1,2000},{64,32}}){int[] p=PreviewAtlas.layout(source[0],source[1],n,edge);bounded&=p[0]>0&&p[1]>0&&p[0]<=source[0]&&p[1]<=source[1]&&p[0]*p[2]<=2048&&p[1]*((n+p[2]-1)/p[2])<=2048&&p[2]==PreviewAtlas.columns(p[0],p[1],n);}
        check(bounded,"All recipe counts and source aspects retain a valid bounded encoder/decoder layout");
        int tw=overview[0],th=overview[1],columns=overview[2],width=tw*columns,height=th*((24+columns-1)/columns);int[] ids=new int[24];byte[] bytes=new byte[width*height*4];
        for(int id=0;id<24;id++){ids[id]=id;for(int yy:new int[]{0,th-1})for(int xx:new int[]{0,tw-1})pixel(bytes,width,id%columns*tw+xx,id/columns*th+yy,id+1,yy==0?20:200,xx==0?1:2);}
        PreviewAtlas atlas=new PreviewAtlas(tw,th,ids,bytes,100);int[] last=atlas.pixels(23);check(atlas.columns==4&&last[0]==0xff18c801&&last[last.length-1]==0xff181402,"Repacked high-resolution tiles decode the final photo upright without neighboring-film bleed");
        check(atlas.pixels(3)[0]==0xff04c801&&atlas.pixels(4)[0]==0xff05c801,"Crossing the new four-column row boundary preserves recipe identity");
        int[][] histogram=Histogram.atlas(atlas,23);boolean totals=true;for(int[] channel:histogram){int sum=0;for(int value:channel)sum+=value;totals&=sum==tw*th;}check(totals,"The shared histogram still samples exactly its selected tile after repacking");
        boolean rejected=false;try{new PreviewAtlas(512,512,ids,new byte[1],0);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"An unpackable oversized atlas is rejected before any pixel decoding");
        rejected=false;try{PreviewAtlas.layout(1000,1000,FilmCatalog.NAMES.length+1,320);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Invalid catalog counts cannot grow GPU allocations beyond the recipe list");
        check(PreviewPixels.argb(ByteBuffer.allocate(512*512*4),512,512).length==512*512,"Bounded raw pixel conversion accepts the new detailed-preview limit");
        System.out.println("ALL "+count+" PREVIEW FIDELITY TESTS PASSED");
    }
}
