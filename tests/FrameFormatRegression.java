import com.silverroom.camera.FrameFormat;
import com.silverroom.camera.PreviewFraming;
import com.silverroom.camera.LensPlan;
import java.util.Arrays;

public final class FrameFormatRegression {
    static int count;
    static void check(boolean v,String message){if(!v)throw new AssertionError(message);count++;System.out.println("PASS "+message);}
    static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
    public static void main(String[] args){
        check(FrameFormat.safe(-1)==0&&FrameFormat.safe(90)==0,"Invalid saved format falls back to filled finder");
        check(Arrays.equals(FrameFormat.fit(0,576,560),new int[]{576,560}),"Fill mode preserves the previous full finder without internal bars");
        check(Arrays.equals(FrameFormat.fit(3,576,560),new int[]{560,560}),"Square format has a square viewport");
        check(Arrays.equals(FrameFormat.fit(1,600,800),new int[]{600,800}),"Native portrait frame retains the complete 3:4 viewport");
        for(int id=0;id<5;id++){
            FrameFormat.Snapshot shot=new FrameFormat.Snapshot(id,576,560);float ratio=FrameFormat.aspect(id,576f/560);
            check(near(shot.outputAspect(0),ratio)&&near(shot.outputAspect(180),ratio)&&near(shot.outputAspect(90),1/ratio)&&near(shot.outputAspect(270),1/ratio),"Format "+id+" follows all four shutter orientations");
        }
        int[] nativeCrop=FrameFormat.crop(3072,4096,3f/4);check(Arrays.equals(nativeCrop,new int[]{0,0,3072,4096}),"Native photo has no additional aspect crop");
        int[] square=FrameFormat.crop(3000,4000,1);check(Arrays.equals(square,new int[]{0,500,3000,3000}),"Square output trims equal top and bottom edges");
        check(Arrays.equals(FrameFormat.crop(4000,3000,1),new int[]{500,0,3000,3000}),"Landscape square trims equal left and right edges");
        check(Arrays.equals(FrameFormat.crop(3011,4017,0),new int[]{0,0,3011,4017}),"Legacy and imported photos keep their original dimensions");
        FrameFormat.Snapshot frozen=new FrameFormat.Snapshot(2,576,560);FrameFormat.fit(4,300,700);check(near(frozen.outputAspect(90),1.5f),"An in-flight shot retains its format after other layout calculations");
        boolean contained=true,centred=true,correctRatio=true;
        for(int[] dimensions:new int[][]{{3072,4096},{4096,3072},{640,480},{1001,2003}})for(float ratio:new float[]{.5625f,.6666667f,.75f,1,1.3333333f,1.5f,1.7777778f,576f/560}){
            int[] c=FrameFormat.crop(dimensions[0],dimensions[1],ratio);
            contained&=c[0]>=0&&c[1]>=0&&c[2]<=dimensions[0]&&c[3]<=dimensions[1]&&c[0]+c[2]<=dimensions[0]&&c[1]+c[3]<=dimensions[1];
            centred&=Math.abs(c[0]-(dimensions[0]-c[2])/2f)<=.5f&&Math.abs(c[1]-(dimensions[1]-c[3])/2f)<=.5f;
            correctRatio&=Math.abs(c[2]-c[3]*ratio)<=Math.max(1,ratio);
        }
        check(contained,"All output crops retain real source pixels without upscaling");
        check(centred,"Every ratio crop remains centred to within a pixel");
        check(correctRatio,"Portrait and landscape outputs meet their selected aspect within pixel rounding");
        boolean aligned=true,fits=true;
        for(int[] available:new int[][]{{576,560},{720,1000},{360,240}})for(int id=0;id<5;id++){
            int[] viewport=FrameFormat.fit(id,available[0],available[1]);fits&=viewport[0]<=available[0]&&viewport[1]<=available[1];
            FrameFormat.Snapshot shot=new FrameFormat.Snapshot(id,viewport[0],viewport[1]);int[] crop=FrameFormat.crop(3000,4000,shot.outputAspect(0));
            for(float x:new float[]{0,.25f,.5f,.75f,1})for(float y:new float[]{0,.25f,.5f,.75f,1}){
                float[] sensor=PreviewFraming.point(x,y,90,4000,3000,viewport[0],viewport[1],1);
                float savedX=(sensor[0]*3000-crop[0])/crop[2],savedY=(sensor[1]*4000-crop[1])/crop[3];
                aligned&=Math.abs(savedX-x)<.004f&&Math.abs(savedY-y)<.004f;
            }
        }
        check(fits,"All selected viewports fit inside the camera area");
        check(aligned,"Finder edges and touch coordinates align with final photo crops across layouts");
        int[] tele=LensPlan.cropSize(3000,4000,LensPlan.crop(135));int[] wide=FrameFormat.crop(tele[0],tele[1],9f/16);
        check(wide[2]<=tele[0]&&wide[3]<=tele[1],"Aspect selection composes after the genuine telephoto focal crop");
        boolean rejected=false;try{new FrameFormat.Snapshot(0,0,500);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"An unlaid-out finder cannot freeze an invalid shot ratio");
        rejected=false;try{FrameFormat.crop(100,100,Float.NaN);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Nonfinite stored crop ratios are rejected");
        System.out.println("ALL "+count+" FRAME FORMAT TESTS PASSED");
    }
}
