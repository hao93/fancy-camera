package com.silverroom.camera;

/** Display-referred RGB and luma, not a sensor RAW histogram. */
public final class Histogram {
    public static final int BINS=64;
    private Histogram(){}
    public static int[][] atlas(PreviewAtlas atlas,int index){
        if(index<0||index>=atlas.ids.length)throw new IllegalArgumentException("Invalid histogram tile");int[][] bins=new int[4][BINS];int left=index%atlas.columns*atlas.tileWidth,bottom=index/atlas.columns*atlas.tileHeight;
        for(int y=0;y<atlas.tileHeight;y++){int offset=((bottom+y)*atlas.width+left)*4;for(int x=0;x<atlas.tileWidth;x++){int r=atlas.rgba[offset++]&255,g=atlas.rgba[offset++]&255,b=atlas.rgba[offset++]&255;offset++;bins[0][r>>2]++;bins[1][g>>2]++;bins[2][b>>2]++;bins[3][((77*r+150*g+29*b)>>8)>>2]++;}}return bins;
    }
}
