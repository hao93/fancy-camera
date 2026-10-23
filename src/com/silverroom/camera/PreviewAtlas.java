package com.silverroom.camera;

import java.util.*;

/** Immutable packed GPU thumbnails. Tiles are indexed in GL's bottom-up atlas. */
public final class PreviewAtlas {
    public final int tileWidth,tileHeight,width,height,columns;public final int[] ids;public final byte[] rgba;public final long captured;
    public static int[] styles(int[] requested,int histogram){ArrayList<Integer> ids=new ArrayList<>();for(int id:requested)if(FilmCatalog.valid(id)&&!ids.contains(id))ids.add(id);if(FilmCatalog.valid(histogram)&&!ids.contains(histogram))ids.add(histogram);int[] out=new int[ids.size()];for(int i=0;i<out.length;i++)out[i]=ids.get(i);return out;}
    public static int columns(int w,int h,int count){if(w<1||h<1||count<1)return 0;for(int c=Math.min(3,count);c<=count;c++)if((long)w*c<=2048&&(long)h*((count+c-1)/c)<=2048)return c;return 0;}
    public static int[] layout(int w,int h,int count,int edge){if(count<1||count>FilmCatalog.NAMES.length||edge<1||edge>PreviewPixels.MAX_EDGE)throw new IllegalArgumentException("Invalid atlas request");for(int e=edge;e>0;e=Math.max(0,e-32)){int[] tile=PreviewPixels.size(w,h,e);int c=columns(tile[0],tile[1],count);if(c>0)return new int[]{tile[0],tile[1],c};}throw new IllegalArgumentException("Atlas cannot fit");}
    public PreviewAtlas(int tw,int th,int[] styles,byte[] pixels,long time){
        if(tw<=0||th<=0||tw>PreviewPixels.MAX_EDGE||th>PreviewPixels.MAX_EDGE||styles.length==0||styles.length>FilmCatalog.NAMES.length)throw new IllegalArgumentException("Invalid atlas");
        tileWidth=tw;tileHeight=th;ids=styles.clone();columns=columns(tw,th,ids.length);if(columns==0)throw new IllegalArgumentException("Atlas exceeds preview budget");width=tw*columns;height=th*((ids.length+columns-1)/columns);if(pixels.length!=width*height*4)throw new IllegalArgumentException("Truncated atlas");rgba=pixels;captured=time;
    }
    public int index(int id){for(int i=0;i<ids.length;i++)if(ids[i]==id)return i;return -1;}
    public int[] pixels(int index){int[] out=new int[tileWidth*tileHeight];copyPixels(index,out);return out;}
    public void copyPixels(int index,int[] out){if(index<0||index>=ids.length||out.length<tileWidth*tileHeight)throw new IllegalArgumentException("Invalid tile");int left=index%columns*tileWidth,bottom=index/columns*tileHeight;
        for(int y=0;y<tileHeight;y++){int offset=((bottom+tileHeight-1-y)*width+left)*4;for(int x=0;x<tileWidth;x++){int r=rgba[offset++]&255,g=rgba[offset++]&255,b=rgba[offset++]&255;offset++;out[y*tileWidth+x]=0xff000000|(r<<16)|(g<<8)|b;}}
    }
}
