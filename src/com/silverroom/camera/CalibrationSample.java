package com.silverroom.camera;
import android.os.*;

/** One clean center-frame sample; no private image or network storage. */
final class CalibrationSample implements CameraEngine.PreviewSink {
    interface Done{void ready(float[] rgb,String error);}private final CameraEngine camera;private final Done done;private final Handler main=new Handler(Looper.getMainLooper());private boolean requested,finished;
    CalibrationSample(CameraEngine c,Done d){camera=c;done=d;camera.preview(this);main.postDelayed(()->finish(null,"未收到稳定取景，请稍后重试"),4000);}
    public synchronized long acquire(long now){if(requested||finished)return 0;requested=true;return 1;}public int[] styles(){return new int[]{0};}public int edge(){return 96;}public Film.Settings settings(){Film.Settings s=new Film.Settings();s.stock=0;s.strength=0;return s;}
    public void frame(long token,PreviewAtlas batch){try{int[] p=batch.pixels(0);float r=0,g=0,b=0,lo=1,hi=0;int count=0;for(int y=batch.tileHeight*2/5;y<batch.tileHeight*3/5;y++)for(int x=batch.tileWidth*2/5;x<batch.tileWidth*3/5;x++){int v=p[y*batch.tileWidth+x];float red=((v>>16)&255)/255f,green=((v>>8)&255)/255f,blue=(v&255)/255f;r+=red;g+=green;b+=blue;float l=(red+green+blue)/3;lo=Math.min(lo,l);hi=Math.max(hi,l);count++;}if(count==0||hi-lo>.18f)throw new IllegalArgumentException("中央区域不够均匀，请对准灰卡");finish(ImageQuality.grey(r/count,g/count,b/count),null);}catch(Exception e){finish(null,e.getMessage());}}
    public void failed(long token){finish(null,"灰卡取景读取失败");}
    synchronized void finish(float[] gains,String error){if(finished)return;finished=true;camera.removePreview(this);main.post(()->done.ready(gains,error));}
    void cancel(){finish(null,"已取消灰卡采样");}
}
