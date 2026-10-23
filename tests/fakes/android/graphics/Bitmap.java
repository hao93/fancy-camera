package android.graphics;

/** Test-only in-memory bitmap; never included in the Android src tree. */
public final class Bitmap {
    public enum Config {ARGB_8888}
    private final int width,height;private final int[] pixels;public final java.awt.image.BufferedImage buffer;private boolean recycled;public static int live,maxLive;
    private Bitmap(int w,int h){width=w;height=h;buffer=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_ARGB);pixels=((java.awt.image.DataBufferInt)buffer.getRaster().getDataBuffer()).getData();live++;maxLive=Math.max(live,maxLive);}
    public static Bitmap createBitmap(int w,int h,Config config){return new Bitmap(w,h);}
    public int getWidth(){return width;}public int getHeight(){return height;}
    public void recycle(){if(!recycled){recycled=true;live--;buffer.flush();}}public boolean isRecycled(){return recycled;}
    public void getPixels(int[] dst,int offset,int stride,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,dst,offset+row*stride,w);}
    public void setPixels(int[] src,int offset,int stride,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(src,offset+row*stride,pixels,(y+row)*width+x,w);}
}
