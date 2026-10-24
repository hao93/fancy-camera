package com.silverroom.camera;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import java.util.*;

/** Shipping latch + ready + render guard, including the producer-stall callback ordering. */
public final class PreviewStartupProbe {
    static List<String> passed=new ArrayList<>();
    static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);passed.add(label);}
    public static List<String> run(){
        ZoomEngine first=new ZoomEngine();first.previewReady=false;first.frame=true;
        first.texture.afterDrain=()->first.handler.post(()->{first.deliverResult();});
        first.renderPrefix();check(first.texture.updates==1&&!first.previewReady,"PRIVATE buffers are consumed before readiness even when the producer cannot deliver its result until drained");
        first.handler.flush();check(first.previewReady&&first.externalFrameReady,"An image arriving before CaptureResult becomes ready without requiring a third frame callback");
        ZoomEngine resultFirst=new ZoomEngine();resultFirst.previewReady=false;resultFirst.previewResultSeen=true;resultFirst.frame=true;resultFirst.renderPrefix();resultFirst.handler.flush();
        check(resultFirst.previewReady&&resultFirst.texture.updates==1,"CaptureResult arriving before the PRIVATE image also completes startup");
        ZoomEngine early=new ZoomEngine();early.previewReady=false;early.frame=true;early.renderPrefix();early.handler.flush();
        check(!early.previewReady&&early.externalFrameReady,"A drained image alone never bypasses actual camera-result verification");early.deliverResult();
        check(early.previewReady,"A later result can use the already-drained image to finish startup");
        ZoomEngine stale=new ZoomEngine();stale.previewReady=false;stale.previewResultSeen=true;stale.frame=true;stale.renderPrefix();stale.sessionGeneration++;stale.handler.flush();
        check(!stale.previewReady&&!stale.externalFrameReady,"A queued latch from a closed session cannot make the next optical session ready");
        stale=new ZoomEngine();stale.previewReady=false;stale.previewResultSeen=true;stale.frame=true;stale.renderPrefix();stale.texture=new SurfaceTexture();stale.handler.flush();
        check(!stale.previewReady,"Replacing the EGL SurfaceTexture invalidates an older queued latch");
        ZoomEngine paused=new ZoomEngine();paused.previewReady=false;paused.frame=true;paused.renderPrefix();paused.active=false;paused.handler.flush();
        check(!paused.externalFrameReady,"Late frame callbacks after pause do not reactivate the preview");
        ZoomEngine yuv=new ZoomEngine();yuv.previewReady=false;yuv.yuvPreview=true;yuv.frame=true;yuv.renderPrefix();
        check(yuv.texture.updates==0,"YUV sessions do not falsely consume or verify the PRIVATE texture");
        ZoomEngine broken=new ZoomEngine();broken.previewReady=false;broken.frame=true;broken.texture.fail=true;broken.renderPrefix();broken.handler.flush();
        check(broken.recovered==1&&!broken.previewReady,"PRIVATE texture-read exceptions enter camera recovery instead of crashing or falsely reporting readiness");
        ZoomEngine retry=new ZoomEngine();retry.previewReady=false;retry.device=null;retry.focal(23);retry.handler.flush();
        check(retry.opened==1&&retry.closed==0,"Retapping the already-selected main focal can restart an absent device instead of being ignored");
        PreviewHold cache=new PreviewHold();GLES20.version=31;cache.save(300,400);GLES20.error=1282;GLES20.version=32;cache.save(300,400);cache.draw(300,400);
        check(GLES20.presented==32,"Unrelated earlier GL errors do not invalidate a successfully copied transition frame");
        GLES20.copyError=1282;GLES20.version=33;cache.save(300,400);cache.draw(300,400);
        check(GLES20.presented==32,"A failed copy preserves the last complete transition frame instead of turning the preview black");
        PreviewHold empty=new PreviewHold();int allocated=GLES20.allocations;empty.save(300,400);check(!empty.draw(300,400),"A failed first copy cannot expose uninitialized transition pixels");
        GLES20.copyError=0;empty.save(300,400);empty.draw(300,400);
        check(GLES20.presented==33&&GLES20.allocations==allocated+2,"The next valid frame retries allocation after an initial GPU cache failure");
        return passed;
    }
}
