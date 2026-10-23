package com.silverroom.camera;
import android.hardware.camera2.*;
import android.media.Image;
import android.os.Handler;
import java.io.*;
import java.util.concurrent.ExecutorService;

/** One shutter transaction, one matching RAW/result pair. All offers run on the camera handler. */
final class RawCapture {
    interface Complete {void done(byte[] jpeg,File dng,CaptureResult result,String note);}
    private final File cache;private final CameraCharacteristics chars;private final ExecutorService encoder;private final Handler handler;private final Complete callback;private final int rotation;
    private byte[] jpeg;private CaptureResult result;private Image raw;private boolean needRaw,encoding,done,timedOut;private volatile boolean cancelled;private String note="";
    RawCapture(File c,CameraCharacteristics ch,ExecutorService e,Handler h,int r,boolean need,String reason,Complete cb){cache=c;chars=ch;encoder=e;handler=h;rotation=r;needRaw=need;note=need?"":reason;callback=cb;handler.postDelayed(()->{if(!done&&!encoding){if(raw!=null){raw.close();raw=null;}timedOut=true;note=needRaw?"RAW或拍摄元数据未按时返回，仅保留JPEG":"拍摄元数据未返回";needRaw=false;finish(true);}},5000);}
    void jpeg(byte[] value){if(!cancelled&&!done){jpeg=value;finish(false);}}
    void result(CaptureResult value){if(!cancelled&&!done){result=value;finish(false);}}
    void image(Image value){if(cancelled||done||!needRaw||raw!=null){value.close();return;}raw=value;finish(false);}
    void withoutRaw(String reason){if(raw!=null){raw.close();raw=null;}needRaw=false;note=reason;finish(false);}
    private void finish(boolean allowMissing){if(done||cancelled||jpeg==null||(!allowMissing&&!timedOut&&result==null)||(needRaw&&raw==null))return;done=true;encoding=true;final byte[] bytes=jpeg;final Image image=raw;raw=null;final CaptureResult metadata=result;final String prior=note;
        encoder.execute(()->{File output=null;String message=prior;try{if(image!=null){Long timestamp=metadata==null?null:metadata.get(CaptureResult.SENSOR_TIMESTAMP);if(timestamp==null||!RawPolicy.pair(image.getTimestamp(),timestamp))throw new IOException("RAW与实际拍摄结果时间戳不匹配");output=File.createTempFile("fancy-raw-",".dng",cache);try(DngCreator dng=new DngCreator(chars,metadata);FileOutputStream stream=new FileOutputStream(output)){dng.setOrientation(RawPolicy.orientation(rotation));dng.setDescription("Fancy Camera; native sensor RAW; no film or digital crop");dng.writeImage(stream,image);stream.getFD().sync();}}}catch(Exception|OutOfMemoryError e){if(output!=null)output.delete();output=null;message="RAW未保存，仅保留JPEG："+e.getMessage();}finally{if(image!=null)image.close();}File saved=output;String status=message;handler.post(()->{if(cancelled){if(saved!=null)saved.delete();return;}callback.done(bytes,saved,metadata,status);});});
    }
    void cancel(){cancelled=true;if(!encoding&&raw!=null){raw.close();raw=null;}}
}
