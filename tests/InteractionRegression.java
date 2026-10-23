import com.silverroom.camera.RotaryGesture;
import com.silverroom.camera.PreviewFraming;
import com.silverroom.camera.YuvGeometry;

public final class InteractionRegression {
    static int count;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);count++;System.out.println("PASS "+message);}
    static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
    static float x(float degrees){return 100*(float)Math.cos(Math.toRadians(degrees));}
    static float y(float degrees){return 100*(float)Math.sin(Math.toRadians(degrees));}
    public static void main(String[] args){
        RotaryGesture dial=new RotaryGesture(-2,2,.1f);
        dial.begin(0,-100,100,0);check(near(dial.move(100,0),1.3f),"Clockwise quarter turn increases EV in 0.1 steps");
        check(near(dial.move(0,-100),0),"Retracing the arc returns to the starting exposure");
        dial.begin(x(179),y(179),100,0);check(near(dial.move(x(-179),y(-179)),0),"Crossing the angle seam by two degrees does not jump EV");
        check(near(dial.move(x(-161),y(-161)),.3f),"Clockwise seam crossing continues smoothly");
        dial.begin(x(-179),y(-179),100,0);check(near(dial.move(x(161),y(161)),-.3f),"Counterclockwise seam crossing decreases smoothly");
        dial.begin(0,-100,100,1.9f);check(near(dial.move(100,0),2),"Upper exposure limit clamps");
        check(near(dial.move(x(-10),y(-10)),1.9f),"Turning back from a limit responds immediately");
        dial.begin(0,-100,100,-1.9f);check(near(dial.move(-100,0),-2),"Lower exposure limit clamps");
        dial.begin(0,0,100,.7f);check(near(dial.move(1,1),.7f),"Centre touches do not alter EV");
        check(near(dial.move(100,0),.7f),"Leaving the centre establishes an anchor without a jump");
        check(near(dial.move(0,100),2),"A circular drag works after leaving the centre");
        dial.begin(100,0,100,.4f);dial.move(0,0);check(near(dial.move(-100,0),.4f),"Crossing the centre does not cause a half-turn jump");
        dial.begin(0,-100,100,0);for(int i=1;i<=10;i++)dial.move(x(-90+i),y(-90+i));check(near(dial.move(x(-80),y(-80)),.1f),"Small movement events accumulate below a detent");
        dial.begin(-100,0,100,-.6f);check(near(dial.move(-100,0),-.6f),"Beginning a new gesture preserves the current EV");
        RotaryGesture color=new RotaryGesture(-100,100,1);color.begin(100,0,100,0);check(near(color.move(0,100),67),"Darkroom dials share the rotary interaction with their own range");
        float[] p=PreviewFraming.point(.2f,.8f,90,640,480,480,640,1);check(near(p[0],.2f)&&near(p[1],.8f),"Matching portrait aspect preserves focus coordinates");
        p=PreviewFraming.point(.5f,.5f,90,640,480,600,500,1);check(near(p[0],.5f)&&near(p[1],.5f),"Filled preview keeps the focus centre stationary");
        p=PreviewFraming.point(0,0,90,640,480,640,480,1);check(near(p[0],0)&&near(p[1],.21875f),"A wide viewport maps its top edge into the visible sensor crop");
        p=PreviewFraming.point(0,0,90,640,480,320,640,1);check(near(p[0],1f/6)&&near(p[1],0),"A tall viewport maps its left edge into the visible sensor crop");
        p=PreviewFraming.point(0,0,90,640,480,640,480,2);check(near(p[0],.25f)&&near(p[1],.359375f),"Digital focal crop composes with viewport fill for touch focus");
        // Invert the shipping YUV geometry and reconstruct view points across aspect ratios.
        boolean roundTrip=true,covered=true;
        for(int[] viewport:new int[][]{{640,480},{320,900},{480,640},{720,720}}){
            float[] m=new float[16];YuvGeometry.model(m,90,0,false,640,480,viewport[0],viewport[1]);
            for(float vx:new float[]{0,.2f,.5f,.8f,1})for(float vy:new float[]{0,.3f,.5f,.7f,1}){
                p=PreviewFraming.point(vx,vy,90,640,480,viewport[0],viewport[1],1);
                float sensorX=2*p[1]-1,sensorY=2*p[0]-1;
                roundTrip&=near(m[0]*sensorX+m[4]*sensorY,2*vx-1)&&near(m[1]*sensorX+m[5]*sensorY,1-2*vy);
                covered&=p[0]>=0&&p[0]<=1&&p[1]>=0&&p[1]<=1;
            }
        }
        check(roundTrip,"Touch mapping inverts the shipping renderer across viewport shapes");
        check(covered,"Every viewport point lies inside the image with no empty sidebars");
        System.out.println("ALL "+count+" ROTARY / PREVIEW FRAMING TESTS PASSED");
    }
}
