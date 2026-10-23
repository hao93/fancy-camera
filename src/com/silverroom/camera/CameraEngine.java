package com.silverroom.camera;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.hardware.camera2.params.*;
import android.media.*;
import android.opengl.*;
import android.os.*;
import android.util.*;
import android.view.*;
import java.nio.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import com.silverroom.camera.LensPlan.Route;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class CameraEngine implements GLSurfaceView.Renderer {
    public interface PreviewSink {long acquire(long now);int[] styles();int edge();Film.Settings settings();void frame(long token,PreviewAtlas batch);void failed(long token);}
    public interface HistogramSink {void histogram(int[][] bins);}
    private final PreviewChannel<PreviewSink> previewChannel=new PreviewChannel<>();private PreviewReadback popupReadback;private GrainTexture grainTexture;
    private final PreviewChannel<HistogramSink> histogramChannel=new PreviewChannel<>();private PreviewReadback histogramReadback;private long lastHistogram;
    public void preview(PreviewSink sink){previewChannel.set(sink);view.requestRender();}
    public void removePreview(PreviewSink sink){if(previewChannel.remove(sink))view.queueEvent(()->{if(previewChannel.get()==null&&popupReadback!=null)popupReadback.releaseTarget();});}
    public void histogram(HistogramSink sink){histogramChannel.set(sink);view.requestRender();}
    public void removeHistogram(HistogramSink sink){if(histogramChannel.remove(sink))view.queueEvent(()->{if(histogramChannel.get()==null&&histogramReadback!=null)histogramReadback.releaseTarget();});}
    public interface Listener {void status(String text);void picture(Shot shot);void error(String message);void lensesChanged();void metering(int state,String reason);}
    public static final class Shot {
        public final byte[] jpeg;public final int clockwise;public final float crop;public final String lens;public final CaptureOrientation.Snapshot orientation;public final File dng;public final CaptureInfo info;public final String rawNote;
        Shot(byte[] bytes,int rotation,float factor,String source,CaptureOrientation.Snapshot pose,File raw,CaptureInfo actual,String note){jpeg=bytes;clockwise=rotation;crop=factor;lens=source;orientation=pose;dng=raw;info=actual;rawNote=note;}
    }
    private static final class ShotSpec {
        final float crop;final int rotation;final String description;final CaptureOrientation.Snapshot orientation;
        ShotSpec(float c,CaptureOrientation.Snapshot pose,String d){crop=c;rotation=pose.rotation;orientation=pose;description=d;}
    }
    final Activity activity;final Listener listener;final CameraManager manager;
    private final OrientationEventListener orientationListener;private volatile int deviceOrientation;
    final HandlerThread thread=new HandlerThread("OpticalCamera");final Handler handler;
    final ExecutorService encoder=Executors.newSingleThreadExecutor();
    private final ExecutorService previewWorker=Executors.newSingleThreadExecutor();private final java.util.concurrent.atomic.AtomicBoolean previewBusy=new java.util.concurrent.atomic.AtomicBoolean();
    public final GLSurfaceView view;private SurfaceTexture texture;private Surface surface;
    private CameraDevice device;private CameraCaptureSession session;private CaptureRequest.Builder request;private ImageReader reader,previewReader,rawReader;private RawCapture rawCapture;private final Set<String> rawRejected=new HashSet<>();private volatile String rawState="等待镜头就绪";private volatile int lastIso,previewMode;private volatile Size photoSize=new Size(4096,3072);public volatile boolean rawWanted;
    private final CameraHandoff handoff=new CameraHandoff();private final Map<String,Integer> profiles=new HashMap<>();
    private int profileIndex;private volatile boolean yuvPreview;private volatile YuvFrame yuvFrame;
    private static final class YuvFrame {final byte[] data;final int width,height,sourceWidth,sourceHeight;final float focal;final int[] roi;YuvFrame(byte[] d,int w,int h,int[] r,float mm){data=d;sourceWidth=w;sourceHeight=h;roi=r;width=r[2];height=r[3];focal=mm;}}
    private CameraCharacteristics chars,ownerChars;private Route route;
    private final ArrayList<Route> routes=new ArrayList<>();private final Set<String> failed=new HashSet<>(),verified=new HashSet<>();
    private volatile Set<Integer> available=Collections.emptySet(),detected=Collections.emptySet();private volatile boolean front,scanned,previewReady;
    private volatile float requestedMm=23,previewCrop=1;private boolean active,splitPreview,stillOnly,captureInFlight,previewResultSeen;
    private final MeteringLock meter=new MeteringLock();private long focusEpoch;private float lockX=.5f,lockY=.5f;private Float lockDistance;private Long lockExposure,lockDuration;private Integer lockIso;
    private int generation,sessionGeneration,flashMode;private float ev;private Runnable pendingStill;private ShotSpec pendingShot;
    private volatile boolean frame;private volatile int sensorOrientation=90;private volatile Size previewSize=new Size(1440,1080);
    private volatile int width=1,height=1;private int texId,program,yuvProgram;private final int[] yuvTextures=new int[2];private ByteBuffer yuvUpload;private final float[] st=new float[16],model=new float[16];
    private volatile int maxTextureEdge=2048;private YuvFrame uploadedFrame;
    private final FloatBuffer vertices=ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    public volatile Film.Settings film=new Film.Settings();public volatile String report="尚未检测镜头";
    private final StringBuilder events=new StringBuilder();private String enumeration="";private long lastReport;
    public CameraEngine(Activity a,Listener l){
        activity=a;listener=l;manager=(CameraManager)a.getSystemService(Context.CAMERA_SERVICE);thread.start();handler=new Handler(thread.getLooper());
        orientationListener=new OrientationEventListener(a){public void onOrientationChanged(int degrees){deviceOrientation=CaptureOrientation.update(degrees,deviceOrientation);}};
        view=new GLSurfaceView(a);view.setEGLContextClientVersion(2);view.setPreserveEGLContextOnPause(true);view.setRenderer(this);view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        vertices.put(new float[]{-1,-1,0,0,1,-1,1,0,-1,1,0,1,1,1,1,1}).position(0);
    }
    private boolean sameTreatment(Film.Settings a,Film.Settings b){Film.Settings copy=previewSettings(a);copy.stock=b.stock;return Arrays.equals(Film.parameters(copy),Film.parameters(b))&&copy.strength==b.strength&&copy.skin==b.skin&&copy.adaptiveGrain==b.adaptiveGrain;}private Film.Settings previewSettings(Film.Settings settings){Film.Settings copy=settings.copy();copy.iso=lastIso;return copy;}
    public int iso(){return lastIso;}public int plannedEdge(float aspect){Size size=photoSize;return ImageQuality.outputEdge(size.getWidth(),size.getHeight(),LensPlan.crop(requestedMm),aspect);}
    public String rawStatus(){return rawState;}
    public void raw(boolean enabled){handler.post(()->{if(rawWanted==enabled)return;rawWanted=enabled;rawState=enabled?"正在检测当前镜头RAW能力":"RAW已关闭";if(!captureInFlight){close();open();}});}
    public void quality(int mode){handler.post(()->{int next=mode==1?1:0;if(previewMode!=next&&!captureInFlight){previewMode=next;close();open();}});}
    public boolean available(float mm){return !front&&available.contains(LensPlan.base(mm));}
    public String unavailableLabel(float mm){return detected.contains(LensPlan.base(mm))?"不可用":"未检测";}
    public boolean scanned(){return scanned;}public boolean front(){return front;}public float focal(){return requestedMm;}
    public void resume(){active=true;if(orientationListener.canDetectOrientation())orientationListener.enable();view.onResume();handler.post(()->{scan();open();});}
    public void pause(){active=false;orientationListener.disable();handler.post(this::close);view.onPause();}
    public void destroy(){orientationListener.disable();handler.post(()->{close();if(surface!=null)surface.release();if(texture!=null)texture.release();encoder.shutdown();previewWorker.shutdown();});thread.quitSafely();}
    public void permission(){handler.post(()->{scan();open();});}
    public void rescan(){handler.post(()->{close();failed.clear();verified.clear();profiles.clear();rawRejected.clear();events.setLength(0);scan();open();});}
    private boolean logical(CameraCharacteristics c){int[] cap=c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);if(cap!=null)for(int value:cap)if(value==CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)return true;return false;}
    private float equivalent(CameraCharacteristics c){
        float[] focal=c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);SizeF size=c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        return focal==null||focal.length==0||size==null?0:(float)(focal[0]*43.2666/Math.hypot(size.getWidth(),size.getHeight()));
    }
    private void scan(){
        try{
            if(activity.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
            listener.status("正在读取镜头信息…");routes.clear();scanned=false;
            String version=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;
            StringBuilder out=new StringBuilder("Fancy Camera "+version+" / 光学优先\n"+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+"\n应用标识 "+activity.getPackageName()+"\n系统 "+Build.DISPLAY+"\n");
            String[] publicIds=manager.getCameraIdList();Set<String> known=new HashSet<>(Arrays.asList(publicIds));
            out.append("标准相机列表 ").append(Arrays.toString(publicIds)).append('\n');
            for(String owner:publicIds){
                try{inspect(owner,manager.getCameraCharacteristics(owner),out,known,false);}
                catch(Exception e){out.append("ID ").append(owner).append(" 读取失败：").append(e.getClass().getSimpleName()).append(" / ").append(e.getMessage()).append('\n');}
            }
            boolean hasWide=false,hasTele=false;for(Route r:routes){if(!r.front&&r.base==14)hasWide=true;if(!r.front&&r.base==85)hasTele=true;}
            // Some vivo firmware filters cameraIdList while still answering public metadata
            // calls for numeric IDs. Query metadata only; never open arbitrary IDs or assume
            // that ID 2/3 is a particular sensor. This does not grant missing permissions.
            boolean vivo=Build.MANUFACTURER.equalsIgnoreCase("vivo")||Build.BRAND.equalsIgnoreCase("vivo");
            if(vivo&&(!hasWide||!hasTele)){
                out.append("\n扩展检测：尝试读取 0–31 内未列出的 ID 元数据；不自动打开。\n");
                int readable=0,rejected=0;Map<String,Integer> errors=new LinkedHashMap<>();String example=null;
                for(String id:LensPlan.extraCandidates(known)){
                    if(known.contains(id))continue;
                    try{CameraCharacteristics c=manager.getCameraCharacteristics(id);readable++;inspect(id,c,out,known,true);}
                    catch(Exception e){rejected++;String type=e.getClass().getSimpleName();errors.put(type,errors.containsKey(type)?errors.get(type)+1:1);if(example==null)example="ID "+id+"："+type+" / "+e.getMessage();}
                }
                out.append("扩展可读取 ").append(readable).append(" 个；不可读取 ").append(rejected).append(" 个 ").append(errors).append('\n');
                if(example!=null)out.append("异常示例 ").append(example).append('\n');
            }
            routes.sort((a,b)->{int p=Integer.compare(a.priority(),b.priority());return p!=0?p:Float.compare(Math.abs(a.equivalent-a.base),Math.abs(b.equivalent-b.base));});
            enumeration=out.toString();scanned=true;publish();
        }catch(Exception e){listener.error("读取镜头失败："+e.getMessage());}
    }
    private void inspect(String owner,CameraCharacteristics c,StringBuilder out,Set<String> known,boolean extra){
        known.add(owner);Integer facing=c.get(CameraCharacteristics.LENS_FACING);boolean selfie=facing!=null&&facing==CameraCharacteristics.LENS_FACING_FRONT;
        float eq=equivalent(c);boolean multi=logical(c);int base=LensPlan.identify(eq);
        out.append("\nID ").append(owner).append(extra?"（扩展读取）":"").append(selfie?" 前置":facing!=null&&facing==CameraCharacteristics.LENS_FACING_BACK?" 后置":" 其他").append(" · 元数据约 ").append(Math.round(eq)).append("mm · sensor ").append(c.get(CameraCharacteristics.SENSOR_ORIENTATION)).append("°\n");
        out.append("物理焦距 ").append(Arrays.toString(c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS))).append("mm · 传感器 ").append(c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)).append("mm\n");
        out.append("逻辑多摄 ").append(multi).append(" · 子镜头 ").append(c.getPhysicalCameraIds()).append('\n');
        if(facing==null||(facing!=CameraCharacteristics.LENS_FACING_BACK&&!selfie))return;
        if(selfie){if(!extra)routes.add(new Route(owner,null,0,eq,true,false));}
        else if(!multi&&base!=0)routes.add(new Route(owner,null,base,eq,false,false));
        else if(multi&&base==23)routes.add(new Route(owner,null,23,eq,false,true));
        else if(!multi)out.append("未能匹配 14/23/85mm；未添加猜测路由。\n");
        if(!selfie)for(String id:c.getPhysicalCameraIds()){
            known.add(id);
            try{
                CameraCharacteristics physical=manager.getCameraCharacteristics(id);Integer pf=physical.get(CameraCharacteristics.LENS_FACING);
                if(pf!=null&&pf!=CameraCharacteristics.LENS_FACING_BACK)continue;
                float peq=equivalent(physical);int optical=LensPlan.identify(peq);
                out.append("  物理 ID ").append(id).append(" ≈ ").append(Math.round(peq)).append("mm → ").append(optical==0?"未匹配":optical+"mm").append('\n');
                if(optical!=0){
                    routes.add(new Route(owner,id,optical,peq,false,false));
                    boolean directExists=false;for(Route r:routes)if(r.owner.equals(id)&&r.physical==null)directExists=true;
                    if(!directExists&&optical!=23){routes.add(new Route(id,null,optical,peq,false,false));out.append("  同时保留 ID ").append(id).append(" 独立打开候选；元数据可读不等于可直接打开。\n");}
                }
            }catch(Exception e){out.append("  ID ").append(id).append(" 元数据不可读：").append(e.getMessage()).append('\n');}
        }
    }
    private Route select(){return LensPlan.select(routes,requestedMm,front,failed);}
    private void publish(){
        Set<Integer> set=new HashSet<>(),all=new HashSet<>();for(Route r:routes)if(!r.front){all.add(r.base);if(!failed.contains(r.key()))set.add(r.base);}
        detected=Collections.unmodifiableSet(all);available=Collections.unmodifiableSet(set);StringBuilder out=new StringBuilder(enumeration);
        for(int base:new int[]{14,23,85}){LensPlan.State state=LensPlan.state(routes,base,failed,verified);
            String label=state==LensPlan.State.MISSING?"：系统未返回可识别镜头（尚未尝试配置）":state==LensPlan.State.REJECTED?"：发现镜头，但打开/输出失败":state==LensPlan.State.VERIFIED?"：已收到取景结果；拍照需另行验证":"：发现候选镜头，等待实际取景验证";
            out.append('\n').append(base).append("mm ").append(LensPlan.name(base)).append(label);
        }
        out.append("\n\n35/50 固定使用 23mm 主摄；135/230 固定使用 85mm 长焦。基础镜头缺失时禁用该组，不以主摄裁切替代超广角或长焦。\n");
        out.append("镜头通过元数据识别，焦段标签为 X100 Ultra 标称值；是否可用需实际会话配置。\n").append(events);report=out.toString();listener.lensesChanged();
    }
    private void event(String s){if(events.length()>10000)events.delete(0,5000);events.append(s).append('\n');publish();}
    public void focal(float mm){LensPlan.base(mm);handler.post(()->{
        if(front){listener.status("请先切回后置相机");return;}
        if(!available.contains(LensPlan.base(mm))){listener.status((int)mm+"mm 所需的 "+LensPlan.base(mm)+"mm 镜头未开放");return;}
        if(requestedMm!=mm)clearMetering();int oldTier=PreviewQuality.tier(requestedMm);requestedMm=mm;previewCrop=LensPlan.crop(mm);Route next=select();listener.lensesChanged();
        if(next==null)return;
        if(route==null||!next.key().equals(route.key())||oldTier!=PreviewQuality.tier(mm)){close();open();}else update();view.requestRender();
    });}
    public void exposure(float value){handler.post(()->{clearMetering();ev=value;update();});}
    public void flash(int value){handler.post(()->{clearMetering();flashMode=value;update();if(value!=0&&chars!=null&&!Boolean.TRUE.equals(chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)))listener.status("当前镜头没有可用闪光灯");});}
    private void open(){handoff.request();tryOpen();}
    private void tryOpen(){
        if(!active||device!=null||surface==null||activity.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        if(!handoff.begin())return;
        Route next=select();if(next==null){handoff.opened();previewReady=false;view.requestRender();listener.error(front?"没有可用前置相机":"所选光学镜头未开放，不能用主摄裁切替代。请选可用镜头或重新检测。");return;}
        route=next;profileIndex=profiles.containsKey(profileKey())?profiles.get(profileKey()):0;splitPreview=route.physical!=null||(!route.front&&route.base!=23);yuvPreview=splitPreview&&PreviewQuality.yuv(profileIndex);stillOnly=false;previewCrop=front?1:LensPlan.crop(requestedMm);
        try{
            ownerChars=manager.getCameraCharacteristics(route.owner);chars=route.physical==null?ownerChars:manager.getCameraCharacteristics(route.physical);
            Integer orientation=chars.get(CameraCharacteristics.SENSOR_ORIENTATION);sensorOrientation=orientation==null?90:orientation;
            StreamConfigurationMap map=chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);if(map==null)throw new IllegalStateException("镜头没有输出配置");
            StreamConfigurationMap ownerMap=ownerChars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            int previewFormat=yuvPreview?ImageFormat.YUV_420_888:ImageFormat.PRIVATE;
            previewSize=choosePreview(route.physical!=null?shared(map,ownerMap,previewFormat):map.getOutputSizes(previewFormat),map,ownerMap,previewFormat);
            if(yuvPreview){previewReader=ImageReader.newInstance(previewSize.getWidth(),previewSize.getHeight(),ImageFormat.YUV_420_888,3);previewReader.setOnImageAvailableListener(this::previewAvailable,handler);}
            else texture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());
            Size[] jpegSizes=map.getOutputSizes(ImageFormat.JPEG);
            int format=!splitPreview&&jpegSizes!=null&&jpegSizes.length>0?ImageFormat.JPEG:ImageFormat.YUV_420_888;
            Size shot=choose(route.physical==null?map.getOutputSizes(format):shared(map,ownerMap,format),profileIndex==0?13000000:3200000);photoSize=shot;reader=ImageReader.newInstance(shot.getWidth(),shot.getHeight(),format,2);reader.setOnImageAvailableListener(this::imageAvailable,handler);
            prepareRaw(map,ownerMap);
            final int token=++generation;event("准备打开 "+route.source()+"；方案 "+profileIndex+" · "+(yuvPreview?"单路 YUV":"单路 PRIVATE")+"取景 "+previewSize+" · 拍照 "+shot+" / "+format+" · "+rawState);
            listener.status("正在绑定 "+(front?"前置":route.base+"mm "+LensPlan.name(route.base)));
            manager.openCamera(route.owner,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice d){handoff.opened();if(token!=generation||!active){retire(d);return;}device=d;createSession(false);}
                public void onClosed(CameraDevice d){handoff.closed(d);handler.postDelayed(CameraEngine.this::tryOpen,180);}
                public void onDisconnected(CameraDevice d){handoff.opened();retire(d);if(token!=generation)return;close();view.requestRender();listener.error("相机连接已断开，请重新检测镜头");}
                public void onError(CameraDevice d,int code){boolean wasOpen=d==device;handoff.opened();retire(d);if(token!=generation)return;if(code==ERROR_CAMERA_IN_USE||code==ERROR_MAX_CAMERAS_IN_USE){close();view.requestRender();event("相机占用错误 "+code);listener.error("相机正被其他应用占用，请关闭后重新检测");}else if(wasOpen)recover("设备回调错误 code="+code);else failRoute("独立/逻辑相机打开被拒绝 code="+code);}
            },handler);
        }catch(Exception e){handoff.opened();recover("打开/输出准备失败："+e.getClass().getSimpleName()+" / "+e.getMessage());}
    }
    private void retire(CameraDevice d){handoff.retiring(d);d.close();}
    private void prepareRaw(StreamConfigurationMap map,StreamConfigurationMap owner){
        rawState=rawWanted?"当前镜头未开放RAW":"RAW已关闭";if(!rawWanted)return;if(rawRejected.contains(route.key())){rawState="此镜头RAW组合已被系统拒绝，当前仅JPEG";return;}
        try{if(!has(chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW))return;Size[] sizes=route.physical==null?map.getOutputSizes(ImageFormat.RAW_SENSOR):shared(map,owner,ImageFormat.RAW_SENSOR);if(sizes==null||sizes.length==0)return;Size chosen=choose(sizes,50000000);if(!RawPolicy.size(chosen.getWidth(),chosen.getHeight())){rawState="RAW尺寸超过安全预算，当前仅JPEG";return;}rawReader=ImageReader.newInstance(chosen.getWidth(),chosen.getHeight(),ImageFormat.RAW_SENSOR,2);rawReader.setOnImageAvailableListener(ir->{try{Image image=ir.acquireNextImage();if(image!=null){if(ir==rawReader&&rawCapture!=null)rawCapture.image(image);else image.close();}}catch(RuntimeException ignored){}},handler);rawState="RAW候选 "+chosen+"，等待实际拍摄验证";}catch(Exception e){rawState="RAW输出不可配置，当前仅JPEG："+e.getMessage();if(rawReader!=null)rawReader.close();rawReader=null;}
    }
    private Size[] shared(StreamConfigurationMap physical,StreamConfigurationMap logical,int format){
        if(logical==null)throw new IllegalArgumentException("逻辑镜头缺少输出配置");
        Size[] a=physical.getOutputSizes(format),b=logical.getOutputSizes(format);List<Size> same=new ArrayList<>();
        if(a!=null&&b!=null)for(Size size:a)if(Arrays.asList(b).contains(size))same.add(size);
        if(same.isEmpty())throw new IllegalArgumentException("逻辑与物理镜头没有共有输出尺寸，format="+format);
        return same.toArray(new Size[0]);
    }
    private Surface previewSurface(){return yuvPreview?previewReader.getSurface():surface;}
    private CaptureRequest.Builder builder(int template)throws CameraAccessException{return route.physical==null?device.createCaptureRequest(template):device.createCaptureRequest(template,Collections.singleton(route.physical));}
    private String profileKey(){return route.key()+"/detail"+PreviewQuality.tier(requestedMm)+"/mode"+previewMode;}
    private Size choosePreview(Size[] sizes,StreamConfigurationMap map,StreamConfigurationMap owner,int format){
        if(sizes==null||sizes.length==0)throw new IllegalArgumentException("镜头未公开预览输出");int[][] values=new int[sizes.length][2];long[] times=new long[sizes.length];
        for(int i=0;i<sizes.length;i++){values[i][0]=sizes[i].getWidth();values[i][1]=sizes[i].getHeight();try{times[i]=map.getOutputMinFrameDuration(format,sizes[i]);if(route.physical!=null)times[i]=Math.max(times[i],owner.getOutputMinFrameDuration(format,sizes[i]));}catch(IllegalArgumentException ignored){}}
        return sizes[PreviewQuality.choose(values,times,ImageQuality.previewBudget(requestedMm,profileIndex,previewMode),Math.min(4096,maxTextureEdge))];
    }
    private void recover(String reason){
        if(rawReader!=null&&(!splitPreview||stillOnly)){rawRejected.add(route.key());rawState="RAW组合被拒绝，已回退JPEG："+reason;event(rawState);rawReader.close();rawReader=null;if(captureInFlight&&rawCapture!=null&&stillOnly){rawCapture.withoutRaw(rawState);createSession(true);}else{boolean interrupted=captureInFlight;close();open();if(interrupted)listener.error("RAW组合失败，已恢复JPEG，请重新拍摄");}return;}
        if(route!=null&&profileIndex+1<PreviewQuality.count(splitPreview)){boolean interrupted=captureInFlight;String key=profileKey();verified.remove(route.key());event(route.source()+" / 方案 "+profileIndex+"："+reason+"；关闭设备后尝试下一方案");profiles.put(key,profileIndex+1);close();view.requestRender();open();if(interrupted)listener.error("拍摄配置失败，正在重建镜头，请就绪后重试");}
        else failRoute(reason);
    }
    private Size choose(Size[] sizes,long budget){
        if(sizes==null||sizes.length==0)throw new IllegalArgumentException("镜头未公开所需输出格式");Size best=null;double score=Double.MAX_VALUE;
        for(Size size:sizes){long pixels=(long)size.getWidth()*size.getHeight();if(pixels>budget)continue;double value=Math.abs(size.getWidth()/(double)size.getHeight()-4d/3)*10+Math.abs(Math.log(budget/(double)pixels))*.1;if(value<score){score=value;best=size;}}
        if(best==null)best=Collections.min(Arrays.asList(sizes),(a,b)->Long.compare((long)a.getWidth()*a.getHeight(),(long)b.getWidth()*b.getHeight()));return best;
    }
    private OutputConfiguration output(Surface target){OutputConfiguration out=new OutputConfiguration(target);if(route.physical!=null)out.setPhysicalCameraId(route.physical);return out;}
    private void createSession(boolean captureOnly){
        if(device==null||reader==null)return;final int token=generation,sessionToken=++sessionGeneration;stillOnly=captureOnly;
        try{
            if(session!=null){session.close();session=null;}request=null;previewReady=false;previewResultSeen=false;yuvFrame=null;
            List<OutputConfiguration> outputs=new ArrayList<>();
            if(!captureOnly)outputs.add(output(previewSurface()));
            if(captureOnly||!splitPreview)outputs.add(output(reader.getSurface()));
            if(rawReader!=null&&(captureOnly||!splitPreview))outputs.add(output(rawReader.getSurface()));
            SessionConfiguration config=new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,outputs,r->handler.post(r),new CameraCaptureSession.StateCallback(){
                public void onConfigured(CameraCaptureSession s){
                    if(token!=generation||sessionToken!=sessionGeneration||device==null||!active){s.close();return;}session=s;
                    try{if(captureOnly)captureRequest();else{request=builder(CameraDevice.TEMPLATE_PREVIEW);request.addTarget(previewSurface());update();handler.postDelayed(()->{if(token==generation&&sessionToken==sessionGeneration&&!previewReady&&!captureInFlight)recover("预览输出超时");},8000);}}
                    catch(Exception e){recover("会话启动失败："+e.getMessage());}
                }
                public void onConfigureFailed(CameraCaptureSession s){s.close();if(token!=generation||sessionToken!=sessionGeneration)return;
                    recover("输出配置被系统拒绝"+(captureOnly?"（拍摄）":"（预览）"));
                }
            });
            try{if(!device.isSessionConfigurationSupported(config)){recover("会话预检不支持："+(captureOnly?"拍照":"取景"));return;}}
            catch(UnsupportedOperationException ignored){event("设备未实现会话预检，继续实际配置");}
            device.createCaptureSession(config);
        }catch(Exception e){
            recover("会话配置异常 "+(captureOnly?"拍照":"取景")+"："+e.getClass().getSimpleName()+" / "+e.getMessage());
        }
    }
    private void failRoute(String reason){
        if(route==null){listener.error(reason);return;}String failedKey=route.key();String description=route.source();failed.add(failedKey);verified.remove(failedKey);close();event(description+"："+reason);view.requestRender();
        if(select()!=null){listener.status("尝试同一光学镜头的另一条公开路径");open();}else listener.error("该光学镜头无法使用："+reason+"；未切换为主摄裁切。");
    }
    private <T> void physical(CaptureRequest.Builder builder,CaptureRequest.Key<T> key,T value){
        if(route.physical==null)return;List<CaptureRequest.Key<?>> keys=ownerChars.getAvailablePhysicalCameraRequestKeys();if(keys!=null&&keys.contains(key))builder.setPhysicalCameraKey(key,value,route.physical);
    }
    private boolean has(int[] values,int value){if(values!=null)for(int item:values)if(item==value)return true;return false;}
    private void controls(CaptureRequest.Builder b){
        b.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        if(has(chars.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES),CaptureRequest.EDGE_MODE_FAST)&&has(ownerChars.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES),CaptureRequest.EDGE_MODE_FAST))setBoth(b,CaptureRequest.EDGE_MODE,CaptureRequest.EDGE_MODE_FAST);
        if(has(chars.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES),CaptureRequest.NOISE_REDUCTION_MODE_FAST)&&has(ownerChars.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES),CaptureRequest.NOISE_REDUCTION_MODE_FAST))setBoth(b,CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_FAST);
        if(has(chars.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES),CaptureRequest.TONEMAP_MODE_FAST)&&has(ownerChars.get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES),CaptureRequest.TONEMAP_MODE_FAST))setBoth(b,CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_FAST);
        int[] modes=chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);int af=has(modes,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)?CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE:has(modes,CaptureRequest.CONTROL_AF_MODE_AUTO)?CaptureRequest.CONTROL_AF_MODE_AUTO:CaptureRequest.CONTROL_AF_MODE_OFF;
        if(meter.active())af=stillOnly&&meter.locked()?CaptureRequest.CONTROL_AF_MODE_OFF:CaptureRequest.CONTROL_AF_MODE_AUTO;b.setTag(Long.valueOf(meter.epoch()));b.set(CaptureRequest.CONTROL_AF_MODE,af);physical(b,CaptureRequest.CONTROL_AF_MODE,af);
        int ae=flashMode==1?CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH:flashMode==2?CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH:CaptureRequest.CONTROL_AE_MODE_ON;
        if(!has(chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),ae))ae=CaptureRequest.CONTROL_AE_MODE_ON;
        if(stillOnly&&meter.locked())ae=CaptureRequest.CONTROL_AE_MODE_OFF;b.set(CaptureRequest.CONTROL_AE_MODE,ae);physical(b,CaptureRequest.CONTROL_AE_MODE,ae);if(Boolean.TRUE.equals(chars.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE))){b.set(CaptureRequest.CONTROL_AE_LOCK,meter.ae()&&ae!=CaptureRequest.CONTROL_AE_MODE_OFF);physical(b,CaptureRequest.CONTROL_AE_LOCK,meter.ae()&&ae!=CaptureRequest.CONTROL_AE_MODE_OFF);}if(meter.active()){Rect region=chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);if(region!=null)xFocus(b,lockX,lockY,region);if(stillOnly&&meter.locked()){setBoth(b,CaptureRequest.LENS_FOCUS_DISTANCE,lockDistance);setBoth(b,CaptureRequest.SENSOR_EXPOSURE_TIME,lockExposure);setBoth(b,CaptureRequest.SENSOR_SENSITIVITY,lockIso);if(lockDuration!=null)setBoth(b,CaptureRequest.SENSOR_FRAME_DURATION,Math.max(lockDuration,lockExposure));}}
        Range<Integer> range=chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);Rational step=chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        if(range!=null&&step!=null&&step.floatValue()>0){int value=range.clamp(Math.round(ev/step.floatValue()));b.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,value);physical(b,CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,value);}
        // Optical selection is performed by the output's camera ID, NEVER by zoomRatio.
        Range<Float> zoom=ownerChars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);if(zoom!=null&&zoom.contains(1f))b.set(CaptureRequest.CONTROL_ZOOM_RATIO,1f);
        if(route.physical==null){Rect full=chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);if(full!=null)b.set(CaptureRequest.SCALER_CROP_REGION,full);}
        int[] stabilization=chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);if(has(stabilization,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF))b.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        if(Build.VERSION.SDK_INT>=31&&has(chars.get(CameraCharacteristics.SCALER_AVAILABLE_ROTATE_AND_CROP_MODES),CaptureRequest.SCALER_ROTATE_AND_CROP_NONE))b.set(CaptureRequest.SCALER_ROTATE_AND_CROP,CaptureRequest.SCALER_ROTATE_AND_CROP_NONE);
    }
    private String description(){return front?"前置 · "+route.source():(int)requestedMm+"mm · "+LensPlan.name(route.base)+(previewCrop==1?"原生 1×":"基础 "+route.base+"mm + "+String.format(Locale.US,"%.2f×",previewCrop)+"裁切")+" · 预览 "+previewSize+(previewSize.getWidth()<=640?" 低清兼容":"")+" · "+route.source()+(yuvPreview?" · YUV 单流":"");}
    private final CameraCaptureSession.CaptureCallback repeating=new CameraCaptureSession.CaptureCallback(){
        @Override public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest req,TotalCaptureResult result){
            if(s!=session||route==null||stillOnly)return;
            if(route.logicalDefault&&!defaultMainVerified(result)){failRoute("系统默认 1× 无法确认主摄原生输出，拒绝作为光学主摄使用");return;}
            previewResultSeen=true;CaptureResult actual=route.physical==null?result:result.getPhysicalCameraResults().get(route.physical);if(actual!=null){Integer iso=actual.get(CaptureResult.SENSOR_SENSITIVITY);lastIso=iso==null?0:iso;}markPreviewReady();lockResult(req,result);
            if(SystemClock.elapsedRealtime()-lastReport>2500){lastReport=SystemClock.elapsedRealtime();String verified="";
                if(route.physical!=null&&result.getPhysicalCameraResults().containsKey(route.physical))verified=" · 物理结果已返回";
                listener.status(description()+verified);
            }
        }
    };
    private void markPreviewReady(){if(!previewReady&&previewResultSeen&&(!yuvPreview||yuvFrame!=null)){previewReady=true;verified.add(route.key());event("取景已验证："+route.source()+" / 方案 "+profileIndex+" / "+(int)requestedMm+"mm / 源 "+previewSize+" / 焦段裁切后约 "+Math.round(previewSize.getWidth()/previewCrop)+"×"+Math.round(previewSize.getHeight()/previewCrop));view.requestRender();}}
    private boolean defaultMainVerified(TotalCaptureResult result){
        Float focal=result.get(CaptureResult.LENS_FOCAL_LENGTH);float[] expected=chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        if(focal==null||expected==null||expected.length==0||Math.abs(focal-expected[0])>expected[0]*.05f)return false;
        Float ratio=result.get(CaptureResult.CONTROL_ZOOM_RATIO);if(ratio!=null&&Math.abs(ratio-1)> .01f)return false;
        Rect actual=result.get(CaptureResult.SCALER_CROP_REGION),full=chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if(actual==null||full==null||actual.width()<full.width()*.99f||actual.height()<full.height()*.99f)return false;
        String physicalId=result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID);
        if(physicalId!=null){try{if(LensPlan.identify(equivalent(manager.getCameraCharacteristics(physicalId)))!=23)return false;}catch(Exception e){return false;}}
        return true;
    }
    private void update(){if(session==null||request==null||stillOnly)return;try{controls(request);lastReport=0;session.setRepeatingRequest(request.build(),repeating,handler);listener.status(description());}catch(Exception e){if(!previewReady)recover("首帧请求失败："+e.getMessage());else listener.error("镜头参数更新失败："+e.getMessage());}}
    public void shoot(){final int orientationAtShutter=deviceOrientation;handler.post(()->{
        if(session==null||device==null||!previewReady||captureInFlight){listener.error("所选光学镜头尚未就绪");return;}
        if(meter.active()&&!meter.locked()){listener.error("正在锁定对焦和曝光，请稍候");return;}if(meter.locked()&&splitPreview&&!canTransferLock()){listener.error("当前镜头分时拍照无法保留锁定，请先解除 AE/AF 锁定");return;}captureInFlight=true;pendingShot=new ShotSpec(previewCrop,new CaptureOrientation.Snapshot(sensorOrientation,orientationAtShutter,front),description());
        final ShotSpec spec=pendingShot;final int shotGeneration=generation;final Route chosen=route;final float shotMm=requestedMm,shotEv=ev;
        rawCapture=new RawCapture(activity.getCacheDir(),chars,encoder,handler,spec.rotation,rawReader!=null,rawWanted?rawState:"",(bytes,dng,actual,note)->{if(shotGeneration!=generation||pendingShot!=spec){if(dng!=null)dng.delete();return;}pendingShot=null;rawCapture=null;captureInFlight=false;CaptureInfo info=captureInfo(actual,shotMm,shotEv,chosen);if(dng!=null)rawState="已实际输出DNG";else if(rawWanted&&!note.isEmpty())rawState=note;listener.picture(new Shot(bytes,spec.rotation,spec.crop,spec.description,spec.orientation,dng,info,note));if(splitPreview&&active){clearMetering();createSession(false);}});
        event("快门姿态：手机 "+pendingShot.orientation.device+"° / 传感器 "+sensorOrientation+"° / 成片像素旋转 "+pendingShot.rotation+"°");
        final ShotSpec expectedShot=pendingShot;final int expectedGeneration=generation;
        handler.postDelayed(()->{if(expectedGeneration==generation&&captureInFlight&&pendingShot==expectedShot){close();view.requestRender();open();listener.error("所选镜头拍摄超时，已重新连接，请重试");}},rawReader!=null?45000:14000);
        if(splitPreview){createSession(true);return;}
        if(flashMode!=0&&Boolean.TRUE.equals(chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))){
            try{final int token=generation;pendingStill=()->{if(token==generation)captureRequest();};request.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);session.capture(request.build(),null,handler);request.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);handler.postDelayed(()->{if(token==generation&&pendingStill!=null){Runnable next=pendingStill;pendingStill=null;next.run();}},1000);return;}
            catch(Exception ignored){pendingStill=null;}
        }captureRequest();
    });}
    private void captureRequest(){
        // Both JPEG and YUV are normalized once in the pixel pipeline; ask the
        // hardware JPEG encoder for its native orientation to avoid double rotation.
        try{CaptureRequest.Builder shot=builder(CameraDevice.TEMPLATE_STILL_CAPTURE);shot.addTarget(reader.getSurface());if(rawReader!=null)shot.addTarget(rawReader.getSurface());controls(shot);if(reader.getImageFormat()==ImageFormat.JPEG){shot.set(CaptureRequest.JPEG_ORIENTATION,0);shot.set(CaptureRequest.JPEG_QUALITY,(byte)97);}
            final int token=generation,sessionToken=sessionGeneration;session.capture(shot.build(),new CameraCaptureSession.CaptureCallback(){
                @Override public void onCaptureCompleted(CameraCaptureSession s,CaptureRequest r,TotalCaptureResult result){if(token!=generation||sessionToken!=sessionGeneration||rawCapture==null)return;CaptureResult actual=route.physical==null?result:result.getPhysicalCameraResults().get(route.physical);if(actual==null&&route.physical.equals(result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)))actual=result;rawCapture.result(actual);}
                @Override public void onCaptureFailed(CameraCaptureSession s,CaptureRequest r,CaptureFailure f){if(token!=generation||sessionToken!=sessionGeneration)return;recover("拍摄请求失败 reason="+f.getReason());}
            },handler);
        }catch(Exception e){recover("拍摄请求异常："+e.getMessage());}
    }
    private void previewAvailable(ImageReader ir){
        if(ir!=previewReader)return;
        try{
            final Image image=ir.acquireLatestImage();if(image==null)return;
            if(!active||!previewBusy.compareAndSet(false,true)){image.close();return;}
            final int token=generation;final float mm=requestedMm;
            try{previewWorker.execute(()->{
                try(Image held=image){
                    Rect crop=held.getCropRect();int w=crop.width()&~1,h=crop.height()&~1;int[] roi=PreviewQuality.region(w,h,LensPlan.crop(mm));Image.Plane[] p=held.getPlanes();
                    byte[] data=YuvFrames.nv21(roi[2],roi[3],(crop.left&~1)+roi[0],(crop.top&~1)+roi[1],p[0].getBuffer(),p[0].getRowStride(),p[0].getPixelStride(),p[1].getBuffer(),p[1].getRowStride(),p[1].getPixelStride(),p[2].getBuffer(),p[2].getRowStride(),p[2].getPixelStride());
                    YuvFrame converted=new YuvFrame(data,w,h,roi,mm);handler.post(()->{if(token!=generation||ir!=previewReader||mm!=requestedMm||!active)return;yuvFrame=converted;markPreviewReady();view.requestRender();});
                }catch(RuntimeException|OutOfMemoryError e){handler.post(()->{if(token==generation&&ir==previewReader&&active)recover("YUV 取景读取失败："+e.getMessage());});}
                finally{previewBusy.set(false);}
            });}catch(RejectedExecutionException e){image.close();previewBusy.set(false);}
        }catch(RuntimeException e){recover("YUV 取景读取失败："+e.getMessage());}
    }
    private void imageAvailable(ImageReader ir){
        if(ir!=reader)return;
        ShotSpec spec=pendingShot;if(spec==null){try(Image discard=ir.acquireNextImage()){}catch(Exception ignored){}return;}
        try(Image image=ir.acquireNextImage()){
            if(image==null)return;final RawCapture bundle=rawCapture;if(bundle==null)return;
            if(image.getFormat()==ImageFormat.JPEG){ByteBuffer buffer=image.getPlanes()[0].getBuffer();byte[] bytes=new byte[buffer.remaining()];buffer.get(bytes);bundle.jpeg(bytes);}
            else{
                Rect crop=image.getCropRect();int w=crop.width()&~1,h=crop.height()&~1;Image.Plane[] p=image.getPlanes();
                byte[] nv21=YuvFrames.nv21(w,h,crop.left&~1,crop.top&~1,p[0].getBuffer(),p[0].getRowStride(),p[0].getPixelStride(),p[1].getBuffer(),p[1].getRowStride(),p[1].getPixelStride(),p[2].getBuffer(),p[2].getRowStride(),p[2].getPixelStride());
                encoder.execute(()->{try{ByteArrayOutputStream out=new ByteArrayOutputStream();if(!new YuvImage(nv21,ImageFormat.NV21,w,h,null).compressToJpeg(new Rect(0,0,w,h),97,out))throw new IOException("YUV 编码失败");handler.post(()->{if(rawCapture==bundle)bundle.jpeg(out.toByteArray());});}catch(Exception e){handler.post(()->abortShot("物理镜头底片编码失败："+e.getMessage()));}});
            }
        }catch(Exception e){abortShot("读取物理镜头照片失败："+e.getMessage());}
    }
    private void abortShot(String error){if(rawCapture!=null)rawCapture.cancel();rawCapture=null;pendingShot=null;captureInFlight=false;if(splitPreview&&active){clearMetering();createSession(false);}listener.error(error);}
    private <T> void setBoth(CaptureRequest.Builder b,CaptureRequest.Key<T> key,T value){if(value!=null){b.set(key,value);physical(b,key,value);}}
    private CaptureInfo captureInfo(CaptureResult result,float mm,float exposure,Route selected){Integer iso=result==null?null:result.get(CaptureResult.SENSOR_SENSITIVITY),awb=result==null?null:result.get(CaptureResult.CONTROL_AWB_MODE);Long ns=result==null?null:result.get(CaptureResult.SENSOR_EXPOSURE_TIME),timestamp=result==null?null:result.get(CaptureResult.SENSOR_TIMESTAMP);Float focal=result==null?null:result.get(CaptureResult.LENS_FOCAL_LENGTH),aperture=result==null?null:result.get(CaptureResult.LENS_APERTURE);return new CaptureInfo(iso==null?0:iso,ns==null?0:ns,timestamp==null?0:timestamp,mm,focal==null?0:focal,aperture==null?0:aperture,result!=null&&result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)!=null&&chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)!=null?result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)*chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).floatValue():exposure,awb==null?-1:awb,selected.base,selected.source());}
    private void clearMetering(){boolean active=meter.active();meter.clear();focusEpoch++;lockDistance=null;lockExposure=null;lockIso=null;lockDuration=null;if(active)listener.metering(MeteringLock.IDLE,"");}
    public void unlockMetering(){handler.post(()->{clearMetering();try{if(request!=null&&session!=null&&!stillOnly){setBoth(request,CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);session.capture(request.build(),null,handler);setBoth(request,CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);}}catch(Exception ignored){}update();});}
    private boolean canTransferLock(){
        if(flashMode!=0||lockDistance==null||lockExposure==null||lockIso==null||!has(chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)||!has(chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),CaptureRequest.CONTROL_AF_MODE_OFF))return false;
        if(route.physical!=null){List<CaptureRequest.Key<?>> keys=ownerChars.getAvailablePhysicalCameraRequestKeys();if(keys==null||!keys.containsAll(Arrays.asList(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.LENS_FOCUS_DISTANCE,CaptureRequest.CONTROL_AE_MODE,CaptureRequest.SENSOR_EXPOSURE_TIME,CaptureRequest.SENSOR_SENSITIVITY))||(lockDuration!=null&&!keys.contains(CaptureRequest.SENSOR_FRAME_DURATION)))return false;}return true;
    }
    public void lockMetering(float x,float y){handler.post(()->{
        if(request==null||session==null||stillOnly||!previewReady||captureInFlight||route==null){listener.metering(MeteringLock.IDLE,"相机尚未就绪");return;}
        clearMetering();if(!Boolean.TRUE.equals(chars.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE))||!has(chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),CaptureRequest.CONTROL_AF_MODE_AUTO)){listener.metering(MeteringLock.IDLE,"当前镜头不支持同时锁定对焦和曝光");return;}
        lockX=Math.max(0,Math.min(1,x));lockY=Math.max(0,Math.min(1,y));long token=meter.begin();event("AE/AF 锁定请求："+route.source()+" / "+(splitPreview?"分时输出":"并发输出"));listener.metering(MeteringLock.FOCUSING,"");
        try{controls(request);setBoth(request,CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);session.capture(request.build(),null,handler);setBoth(request,CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);session.capture(request.build(),repeating,handler);setBoth(request,CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);session.setRepeatingRequest(request.build(),repeating,handler);handler.postDelayed(()->{if(meter.timeout(token)){focusEpoch++;listener.metering(MeteringLock.IDLE,"未能确认 AE/AF 锁定，请重试");update();}},5000);}
        catch(Exception e){clearMetering();update();listener.metering(MeteringLock.IDLE,"锁定请求失败："+e.getMessage());}
    });}
    private void lockResult(CaptureRequest req,TotalCaptureResult result){
        if(!meter.active()||!(req.getTag() instanceof Long)||((Long)req.getTag()).longValue()!=meter.epoch())return;
        CaptureResult actual=result;if(route.physical!=null){actual=result.getPhysicalCameraResults().get(route.physical);if(actual==null){String activeId=result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID);if(!route.physical.equals(activeId))return;actual=result;}}
        int before=meter.state(),next=meter.result(meter.epoch(),actual.get(CaptureResult.CONTROL_AF_STATE),actual.get(CaptureResult.CONTROL_AE_STATE),actual.get(CaptureResult.CONTROL_AE_LOCK));
        if(next==MeteringLock.FAILED){focusEpoch++;listener.metering(MeteringLock.IDLE,"相机未保持 AE/AF 锁定，已解除，请重试");update();return;}
        if(next==MeteringLock.WAIT_AE&&before!=next){listener.metering(next,"");update();}
        if(next==MeteringLock.LOCKED){lockDistance=actual.get(CaptureResult.LENS_FOCUS_DISTANCE);lockExposure=actual.get(CaptureResult.SENSOR_EXPOSURE_TIME);lockIso=actual.get(CaptureResult.SENSOR_SENSITIVITY);lockDuration=actual.get(CaptureResult.SENSOR_FRAME_DURATION);if(before!=next){event("AE/AF 结果已确认："+route.source()+" / 分时拍照传递 "+canTransferLock());listener.metering(next,"");}}
    }
    public void focus(float x,float y){handler.post(()->{
        if(request==null||session==null||stillOnly||route==null||captureInFlight)return;clearMetering();long ticket=++focusEpoch;
        try{controls(request);Rect a=chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);if(a!=null)xFocus(request,x,y,a);if(!has(chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),CaptureRequest.CONTROL_AF_MODE_AUTO)){update();return;}
            setBoth(request,CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_AUTO);setBoth(request,CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);session.capture(request.build(),null,handler);setBoth(request,CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);session.capture(request.build(),null,handler);setBoth(request,CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);session.setRepeatingRequest(request.build(),repeating,handler);handler.postDelayed(()->{if(ticket==focusEpoch&&!meter.active())update();},2500);
        }catch(Exception ignored){}
    });}
    private void xFocus(CaptureRequest.Builder b,float x,float y,Rect a){
        Size buffer=previewSize;YuvFrame pixels=yuvFrame;int bw=yuvPreview&&pixels!=null?pixels.sourceWidth:buffer.getWidth(),bh=yuvPreview&&pixels!=null?pixels.sourceHeight:buffer.getHeight();
        float[] point=PreviewFraming.point(x,y,sensorOrientation,bw,bh,width,height,previewCrop);x=point[0];y=point[1];
        if(front)x=1-x;float sx=x,sy=y;if(sensorOrientation==90){sx=y;sy=1-x;}else if(sensorOrientation==270){sx=1-y;sy=x;}else if(sensorOrientation==180){sx=1-x;sy=1-y;}
        int size=Math.min(a.width(),a.height())/16,cx=a.left+(int)(sx*a.width()),cy=a.top+(int)(sy*a.height());int left=Math.max(a.left,Math.min(a.right-size,cx-size/2)),top=Math.max(a.top,Math.min(a.bottom-size,cy-size/2));
        MeteringRectangle[] area={new MeteringRectangle(left,top,size,size,1000)};
        Integer afCount=chars.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF),aeCount=chars.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        if(afCount!=null&&afCount>0){if(route.physical==null)b.set(CaptureRequest.CONTROL_AF_REGIONS,area);else physical(b,CaptureRequest.CONTROL_AF_REGIONS,area);}
        if(aeCount!=null&&aeCount>0){if(route.physical==null)b.set(CaptureRequest.CONTROL_AE_REGIONS,area);else physical(b,CaptureRequest.CONTROL_AE_REGIONS,area);}
    }
    private void close(){lastIso=0;if(rawCapture!=null)rawCapture.cancel();rawCapture=null;if(rawReader!=null)rawReader.close();rawReader=null;clearMetering();generation++;sessionGeneration++;handoff.cancel();pendingStill=null;pendingShot=null;captureInFlight=false;previewReady=false;previewResultSeen=false;yuvFrame=null;if(session!=null)session.close();session=null;if(device!=null){CameraDevice old=device;device=null;retire(old);}request=null;if(reader!=null)reader.close();reader=null;if(previewReader!=null)previewReader.close();previewReader=null;}
    public void refreshFilm(){view.requestRender();}

    private static int shader(int type,String src){int id=GLES20.glCreateShader(type);GLES20.glShaderSource(id,src);GLES20.glCompileShader(id);int[] ok=new int[1];GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetShaderInfoLog(id));return id;}
    private static int program(String fragment){int p=GLES20.glCreateProgram();GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,CameraShaders.VERTEX));GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,fragment));GLES20.glLinkProgram(p);int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(p));return p;}
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
        grainTexture=new GrainTexture();popupReadback=null;histogramReadback=null;lastHistogram=0;uploadedFrame=null;int[] limit=new int[1];GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE,limit,0);maxTextureEdge=Math.max(2048,limit[0]);
        program=program(CameraShaders.EXTERNAL);yuvProgram=program(CameraShaders.YUV);
        GLES20.glGenTextures(2,yuvTextures,0);for(int id:yuvTextures){GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,id);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);}
        int[] t=new int[1];GLES20.glGenTextures(1,t,0);texId=t[0];GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texId);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        final Surface oldSurface=surface;final SurfaceTexture oldTexture=texture;
        texture=new SurfaceTexture(texId);texture.setOnFrameAvailableListener(s->{frame=true;view.requestRender();});surface=new Surface(texture);handler.post(()->{close();if(oldSurface!=null)oldSurface.release();if(oldTexture!=null)oldTexture.release();open();});
    }
    @Override public void onSurfaceChanged(GL10 gl,int w,int h){width=w;height=h;GLES20.glViewport(0,0,w,h);}
    @Override public void onDrawFrame(GL10 gl){
        GLES20.glClearColor(.04f,.045f,.04f,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);if(texture==null||!previewReady)return;
        boolean raw=yuvPreview;YuvFrame pixels=yuvFrame;if(raw&&(pixels==null||pixels.focal!=requestedMm))return;
        if(!raw&&frame){texture.updateTexImage();texture.getTransformMatrix(st);frame=false;}
        int current=raw?yuvProgram:program;GLES20.glUseProgram(current);grainTexture.bind(current);
        int displayDegrees=activity.getDisplay()==null?0:activity.getDisplay().getRotation()*90;
        if(raw){
            PreviewTransform.yuvModel(model,sensorOrientation,displayDegrees,front,pixels.sourceWidth,pixels.sourceHeight,width,height);PreviewQuality.transform(st,pixels.sourceWidth,pixels.sourceHeight,pixels.roi);
            if(uploadedFrame!=pixels){
            if(yuvUpload==null||yuvUpload.capacity()<pixels.data.length)yuvUpload=ByteBuffer.allocateDirect(pixels.data.length);
            yuvUpload.clear();yuvUpload.put(pixels.data);yuvUpload.flip();GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT,1);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,yuvTextures[0]);GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE,pixels.width,pixels.height,0,GLES20.GL_LUMINANCE,GLES20.GL_UNSIGNED_BYTE,yuvUpload);
            yuvUpload.position(pixels.width*pixels.height);GLES20.glActiveTexture(GLES20.GL_TEXTURE1);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,yuvTextures[1]);GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_LUMINANCE_ALPHA,pixels.width/2,pixels.height/2,0,GLES20.GL_LUMINANCE_ALPHA,GLES20.GL_UNSIGNED_BYTE,yuvUpload);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(current,"uY"),0);GLES20.glUniform1i(GLES20.glGetUniformLocation(current,"uVU"),1);
            uploadedFrame=pixels;}else{GLES20.glUniform1i(GLES20.glGetUniformLocation(current,"uY"),0);GLES20.glUniform1i(GLES20.glGetUniformLocation(current,"uVU"),1);}
        }else{Size buffer=previewSize;PreviewTransform.model(model,sensorOrientation,displayDegrees,buffer.getWidth(),buffer.getHeight(),width,height);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texId);GLES20.glUniform1i(GLES20.glGetUniformLocation(current,"uTex"),0);}
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(current,"uM"),1,false,model,0);GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(current,"uST"),1,false,st,0);
        Film.Settings shown=film.copy();shown.iso=lastIso;GLES20.glUniform1f(GLES20.glGetUniformLocation(current,"uCrop"),previewCrop);float[] p=Film.parameters(shown);GLES20.glUniform3f(GLES20.glGetUniformLocation(current,"uGain"),p[0],p[1],p[2]);GLES20.glUniform3f(GLES20.glGetUniformLocation(current,"uLift"),p[3],p[4],p[5]);GLES20.glUniform4f(GLES20.glGetUniformLocation(current,"uTone"),p[6],p[7],p[8],p[9]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(current,"uShadow"),p[10],p[11],p[12]);GLES20.glUniform3f(GLES20.glGetUniformLocation(current,"uHighlight"),p[13],p[14],p[15]);GLES20.glUniform1f(GLES20.glGetUniformLocation(current,"uShoulder"),p[16]);
        GLES20.glUniform3f(GLES20.glGetUniformLocation(current,"uPost"),p[17],p[18],p[19]);
        GLES20.glUniform4f(GLES20.glGetUniformLocation(current,"uFx"),p[20],p[21],p[22],p[23]);GLES20.glUniform2f(GLES20.glGetUniformLocation(current,"uMix"),FilmStrength.clamp(shown.strength),FilmStrength.exposure(shown.exposure));
        GLES20.glUniform2f(GLES20.glGetUniformLocation(current,"uQuality"),ImageQuality.clamp(shown.skin,0,1),shown.adaptiveGrain?1:0);vertices.position(0);int ap=GLES20.glGetAttribLocation(current,"aP");GLES20.glEnableVertexAttribArray(ap);GLES20.glVertexAttribPointer(ap,2,GLES20.GL_FLOAT,false,16,vertices);vertices.position(2);int at=GLES20.glGetAttribLocation(current,"aT");GLES20.glEnableVertexAttribArray(at);GLES20.glVertexAttribPointer(at,2,GLES20.GL_FLOAT,false,16,vertices);GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        long now=SystemClock.elapsedRealtime();HistogramSink chart=histogramChannel.get();boolean chartDone=false;PreviewSink sink=previewChannel.get();long token=sink==null?0:sink.acquire(now);
        if(token!=0){
            try{if(popupReadback==null)popupReadback=new PreviewReadback(grainTexture);int[] styles=PreviewAtlas.styles(sink.styles(),shown.stock);PreviewAtlas batch=popupReadback.atlas(raw,current,texId,yuvTextures,model,st,previewCrop,width,height,styles,previewSettings(sink.settings()),sink.edge(),now);if(chart!=null&&now-lastHistogram>=125&&sameTreatment(sink.settings(),shown)){chart.histogram(Histogram.atlas(batch,batch.index(shown.stock)));lastHistogram=now;chartDone=true;}sink.frame(token,batch);}
            catch(RuntimeException|OutOfMemoryError failure){sink.failed(token);}
        }
        if(chart!=null&&!chartDone&&now-lastHistogram>=125){
            try{if(histogramReadback==null)histogramReadback=new PreviewReadback(grainTexture);PreviewAtlas sample=histogramReadback.atlas(raw,current,texId,yuvTextures,model,st,previewCrop,width,height,new int[]{shown.stock},shown,96,now);chart.histogram(Histogram.atlas(sample,0));lastHistogram=now;}
            catch(RuntimeException|OutOfMemoryError ignored){lastHistogram=now;}
        }
    }
}








