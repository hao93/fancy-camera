import com.silverroom.camera.CameraHandoff;
import com.silverroom.camera.YuvGeometry;
import com.silverroom.camera.CameraShaders;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class PipelineRegression {
    static int count;
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);count++;System.out.println("PASS "+text);}
    static boolean near(float a,float b){return Math.abs(a-b)<.0001;}
    static boolean corner(float[] m,float x,float y,float expectedX,float expectedY){return near(m[0]*x+m[4]*y,expectedX)&&near(m[1]*x+m[5]*y,expectedY);}
    static String medium(String shader){return shader.replace("#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n","precision mediump float;\n");}
    public static void main(String[] args)throws Exception{
        CameraHandoff gate=new CameraHandoff();Object main=new Object(),tele=new Object();
        gate.request();check(gate.begin(),"Initial main-camera open is admitted");
        gate.cancel();gate.request();check(!gate.begin(),"Switch requested during asynchronous open waits for its callback");
        gate.opened();gate.retiring(main);check(!gate.begin(),"Cancelling an in-flight open closes its returned device before the next open");
        gate.closed(new Object());check(!gate.begin(),"A stale close callback cannot release another device's close barrier");
        gate.closed(main);check(gate.begin(),"Only the matching onClosed permits the pending telephoto open");
        gate.opened();gate.retiring(tele);gate.request();check(!gate.begin(),"Broken-pipe retry cannot reopen a device still being closed");
        gate.closed(tele);check(gate.begin(),"Retry becomes admissible after the broken camera's onClosed");
        gate.opened();gate.retiring(tele);gate.request();gate.cancel();gate.closed(tele);check(!gate.begin(),"Pausing while closing cancels the pending reopen");
        gate.request();check(gate.begin(),"Resume can request a fresh open after cancellation");
        float[] m=new float[16];
        YuvGeometry.model(m,90,0,false,640,480,480,640);
        check(corner(m,-1,1,1,1),"Raw rear YUV top-left rotates to upright portrait top-right exactly once");
        check(corner(m,1,1,1,-1),"Raw rear YUV top-right rotates clockwise to portrait bottom-right");
        check(corner(m,-1,-1,-1,1),"Raw rear YUV bottom-left rotates clockwise to portrait top-left");
        YuvGeometry.model(m,0,0,false,640,480,640,480);check(corner(m,-1,1,-1,1),"Zero-degree YUV preserves native orientation");
        YuvGeometry.model(m,180,0,false,640,480,640,480);check(corner(m,-1,1,1,-1),"180-degree raw YUV rotates once");
        YuvGeometry.model(m,270,0,true,640,480,480,640);check(corner(m,-1,1,1,-1),"Front raw YUV applies sensor rotation and a single screen-space mirror");
        YuvGeometry.model(m,90,90,false,640,480,640,480);check(corner(m,-1,1,-1,1),"Sensor and display rotations cancel in landscape");
        check(near(m[10],1)&&near(m[15],1),"YUV model retains valid homogeneous coordinates");
        if(args.length>0){Path dir=Paths.get(args[0]);Files.createDirectories(dir);Files.write(dir.resolve("camera.vert"),CameraShaders.VERTEX.getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("external.frag"),CameraShaders.EXTERNAL.getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("yuv.frag"),CameraShaders.YUV.getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("raw-external.frag"),CameraShaders.RAW_EXTERNAL.getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("raw-yuv.frag"),CameraShaders.RAW_YUV.getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("medium-external.frag"),medium(CameraShaders.EXTERNAL).getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("medium-yuv.frag"),medium(CameraShaders.YUV).getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("medium-raw-external.frag"),medium(CameraShaders.RAW_EXTERNAL).getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("medium-raw-yuv.frag"),medium(CameraShaders.RAW_YUV).getBytes(StandardCharsets.UTF_8));}
        System.out.println("ALL "+count+" HANDOFF / YUV ORIENTATION TESTS PASSED");
    }
}

