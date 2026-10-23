package com.silverroom.camera;

import android.graphics.Bitmap;
import android.os.*;
import android.widget.ImageView;
import java.util.*;
import java.util.concurrent.*;

/** One popup owns one bounded preview session; bitmap processing never runs on the UI thread. */
final class LiveFilmPreviews implements CameraEngine.PreviewSink {
    private final CameraEngine camera;private final Bitmap still;private final Film.Settings base;private final Runnable changed;
    private final PreviewFlow flow=new PreviewFlow();private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final PreviewFidelity fidelity=new PreviewFidelity();private Bitmap stillSource;
    private final Map<Integer,ImageView> targets=new HashMap<>();private final Bitmap[] latest=new Bitmap[FilmCatalog.NAMES.length];private volatile int[] requested=new int[0];private long received;private volatile int failures;private boolean started;private volatile boolean liveEnabled;
    LiveFilmPreviews(CameraEngine camera,Bitmap still,Film.Settings settings,Runnable changed){this.camera=camera;this.still=still;this.base=settings.copy();this.changed=changed;}
    void start(boolean enabled){if(started||flow.closed())return;started=true;if(camera!=null){enable(enabled);return;}flow.enable(true);requestStill();}
    boolean canLive(){return camera!=null;}
    boolean enabled(){return liveEnabled;}
    void enable(boolean on){if(flow.closed()||camera==null)return;liveEnabled=on;flow.enable(on);failures=0;received=0;if(on)camera.preview(this);else camera.removePreview(this);changed.run();}
    void quality(boolean large){fidelity.mode(large);}
    @Override public long acquire(long now){return failures<3&&requested.length>0?flow.acquire(now):0;}
    @Override public int[] styles(){return requested;}
    @Override public int edge(){return fidelity.edge();}
    @Override public Film.Settings settings(){return base;}
    @Override public void frame(long token,PreviewAtlas atlas){
        if(!flow.valid(token)){flow.complete(token);return;}
        try{worker.execute(()->{Bitmap[] bitmaps=new Bitmap[atlas.ids.length];boolean posted=false;
            try{int[] scratch=new int[atlas.tileWidth*atlas.tileHeight];for(int i=0;i<bitmaps.length;i++){if(!flow.valid(token))return;atlas.copyPixels(i,scratch);bitmaps[i]=Bitmap.createBitmap(scratch,atlas.tileWidth,atlas.tileHeight,Bitmap.Config.ARGB_8888);}main.post(()->commit(token,atlas.ids,bitmaps,atlas.captured));posted=true;}
            catch(RuntimeException|OutOfMemoryError error){failed(token);}finally{if(!posted){discard(bitmaps);flow.complete(token);}}
        });}catch(RejectedExecutionException closed){flow.complete(token);}
    }
    private void commit(long token,int[] ids,Bitmap[] batch,long captured){try{if(!flow.valid(token)||(camera!=null&&!PreviewFlow.current(captured,SystemClock.elapsedRealtime()))){discard(batch);if(flow.valid(token)){fidelity.late();changed.run();}return;}for(int i=0;i<ids.length;i++){latest[ids[i]]=batch[i];ImageView view=targets.get(ids[i]);if(view!=null)view.setImageBitmap(batch[i]);}received=captured;failures=0;fidelity.fresh();changed.run();}finally{flow.complete(token);}}
    private void requestStill(){if(camera!=null||still==null||!started||flow.closed()||failures>=3)return;List<Integer> missing=new ArrayList<>();for(int id:requested)if(latest[id]==null)missing.add(id);if(missing.isEmpty())return;long token=flow.acquire(SystemClock.elapsedRealtime());if(token==0)return;int[] ids=new int[missing.size()];for(int i=0;i<ids.length;i++)ids[i]=missing.get(i);try{worker.execute(()->renderStill(token,ids));}catch(RejectedExecutionException ignored){flow.complete(token);}}
    private void renderStill(long token,int[] ids){Bitmap[] batch=new Bitmap[ids.length];boolean posted=false;try{if(!flow.valid(token))return;if(stillSource==null){int[] size=PreviewPixels.size(still.getWidth(),still.getHeight(),PreviewPixels.MAX_EDGE);stillSource=Bitmap.createScaledBitmap(still,size[0],size[1],true);if(stillSource==still)stillSource=still.copy(Bitmap.Config.ARGB_8888,false);}for(int i=0;i<ids.length;i++){if(!flow.valid(token))return;Film.Settings variant=base.copy();variant.stock=ids[i];batch[i]=Film.develop(stillSource,variant);}long now=SystemClock.elapsedRealtime();main.post(()->commit(token,ids,batch,now));posted=true;}catch(RuntimeException|OutOfMemoryError error){failed(token);}finally{if(!posted){discard(batch);flow.complete(token);}}}
    private static void discard(Bitmap[] batch){for(Bitmap b:batch)if(b!=null&&!b.isRecycled())b.recycle();}
    @Override public void failed(long token){if(!flow.valid(token))return;failures=camera==null?3:failures+1;flow.complete(token);main.post(()->{if(!flow.closed())changed.run();});}
    void bind(int id,ImageView image){targets.put(id,image);if(latest[id]!=null)image.setImageBitmap(latest[id]);}
    void visible(){ArrayList<Integer> ids=new ArrayList<>();android.graphics.Rect rect=new android.graphics.Rect();for(Map.Entry<Integer,ImageView> e:targets.entrySet())if(e.getValue().getGlobalVisibleRect(rect)&&rect.width()>0&&rect.height()>0)ids.add(e.getKey());java.util.Collections.sort(ids);int[] list=new int[ids.size()];for(int i=0;i<list.length;i++)list[i]=ids.get(i);requested=list;requestStill();}
    void unbind(){targets.clear();requested=new int[0];}
    String status(){if(camera!=null&&!liveEnabled)return "实时已关闭";if(failures>=3)return "预览不可用";if(received==0)return camera==null&&still==null?"等待照片":"等待画面";if(camera==null)return "底片预览";return SystemClock.elapsedRealtime()-received>800?"正在更新":"实时预览";}
    boolean fresh(){return failures<3&&received>0&&(camera==null||(liveEnabled&&SystemClock.elapsedRealtime()-received<=800));}
    void close(){flow.close();if(camera!=null)camera.removePreview(this);try{worker.execute(()->{if(stillSource!=null){stillSource.recycle();stillSource=null;}});}catch(RejectedExecutionException ignored){}worker.shutdown();targets.clear();Arrays.fill(latest,null);}
}
