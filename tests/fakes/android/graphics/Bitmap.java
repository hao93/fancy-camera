package android.graphics;

/** Test-only in-memory bitmap; never included in the Android src tree. */
public final class Bitmap {
    public enum Config {ARGB_8888}
    private final int width,height;private final int[] pixels;
    private Bitmap(int w,int h){width=w;height=h;pixels=new int[w*h];}
    public static Bitmap createBitmap(int w,int h,Config config){return new Bitmap(w,h);}
    public int getWidth(){return width;}public int getHeight(){return height;}
    public void getPixels(int[] dst,int offset,int stride,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,dst,offset+row*stride,w);}
    public void setPixels(int[] src,int offset,int stride,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(src,offset+row*stride,pixels,(y+row)*width+x,w);}
}
