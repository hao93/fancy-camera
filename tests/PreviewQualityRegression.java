import com.silverroom.camera.*;
import java.util.*;
import java.nio.*;

public final class PreviewQualityRegression {
    static int count;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);count++;System.out.println("PASS "+label);}
    static boolean near(float a,float b){return Math.abs(a-b)<.00002f;}
    public static void main(String[] args){
        int[][] sizes={{320,240},{640,480},{1280,720},{1440,1080},{1920,1080},{2048,1536},{2560,1920},{4000,3000}};
        long[] times=new long[sizes.length];Arrays.fill(times,33333333L);float[] focals={14,23,35,50,85,135,230};
        for(float mm:focals){
            float crop=LensPlan.crop(mm);int index=PreviewQuality.choose(sizes,times,PreviewQuality.budget(mm,0),4096),w=sizes[index][0],h=sizes[index][1];
            int expected=crop==1?5:crop<1.8f?6:7;
            check(index==expected,(int)mm+"mm selects a source size that accounts for focal crop");
            int[] roi=PreviewQuality.region(w,h,crop);float[] m=new float[16];PreviewQuality.transform(m,w,h,roi);
            boolean inside=true,parity=true;for(int iy=0;iy<=8;iy++)for(int ix=0;ix<=8;ix++){
                float u=.5f+(ix/8f-.5f)/crop,v=.5f+(iy/8f-.5f)/crop,x=m[0]*u+m[12],y=m[5]*v+m[13];
                inside&=x>=-.00001f&&x<=1.00001f&&y>=-.00001f&&y<=1.00001f;
                // The shipping YUV shader flips vT.y after the vertex ROI transform.
                parity&=near((roi[0]+x*roi[2])/w,u)&&near((roi[1]+(1-y)*roi[3])/h,1-v);
            }
            check(inside&&parity,(int)mm+"mm ROI and shader compose exactly one focal crop with no lost edges");
            check(((roi[0]|roi[1]|roi[2]|roi[3])&1)==0&&roi[0]>=0&&roi[1]>=0&&roi[0]+roi[2]<=w&&roi[1]+roi[3]<=h,(int)mm+"mm YUV crop remains chroma aligned and within the source");
            int base=LensPlan.base(mm);check((mm<85?base==14||base==23:base==85)&&near(crop,mm/base),(int)mm+"mm retains its original optical base and photo crop ratio");
            boolean focus=true;for(int sensor:new int[]{0,90,180,270})for(int[] viewport:new int[][]{{1440,2200},{1440,1920},{1920,1080}}){float[] p=PreviewFraming.point(.73f,.29f,sensor,w,h,viewport[0],viewport[1],crop);focus&=p[0]>=0&&p[0]<=1&&p[1]>=0&&p[1]<=1;float[] centre=PreviewFraming.point(.5f,.5f,sensor,w,h,viewport[0],viewport[1],crop);focus&=near(centre[0],.5f)&&near(centre[1],.5f);}
            check(focus,(int)mm+"mm focus uses full-source geometry across portrait landscape and rotations");
        }
        int[] tele=sizes[PreviewQuality.choose(sizes,times,PreviewQuality.budget(230,0),4096)];
        check(tele[0]/LensPlan.crop(230)>640/LensPlan.crop(230)*6,"230mm fixture retains over six times the old preview's linear detail");
        int[] area=PreviewQuality.region(4000,3000,LensPlan.crop(230));check((long)area[2]*area[3]<4000L*3000/7,"230mm conversion copies less than one seventh of the uncropped YUV source");
        check(Arrays.equals(PreviewQuality.region(2048,1536,1),new int[]{0,0,2048,1536}),"Native focal previews keep the complete optical frame");
        long[] slow=times.clone();slow[7]=200000000L;check(PreviewQuality.choose(sizes,slow,PreviewQuality.budget(230,0),4096)==6,"A known five-fps full-size mode does not displace a faster preview");
        check(PreviewQuality.choose(sizes,times,PreviewQuality.budget(230,0),2048)==5,"GPU texture limits cap the selected advertised source size");
        check(PreviewQuality.choose(new int[][]{{640,480}},new long[]{0},PreviewQuality.budget(230,0),4096)==0,"A limited camera still uses its sole advertised preview size");
        check(PreviewQuality.choose(new int[][]{{1280,960}},new long[]{100000000},1000,4096)==0,"A camera with only one slow oversized mode falls back without inventing a resolution");
        boolean steps=true;for(float mm:focals){long prior=Long.MAX_VALUE;for(int p=0;p<5;p++){long b=PreviewQuality.budget(mm,p);steps&=b<prior;prior=b;}}check(steps,"Quality retries descend through smaller budgets for all seven focal lengths");
        check(PreviewQuality.count(true)==8&&PreviewQuality.count(false)==5&&PreviewQuality.yuv(4)&&!PreviewQuality.yuv(5),"Physical routes retain low-resolution YUV and PRIVATE recovery paths");
        check(PreviewQuality.tier(23)!=PreviewQuality.tier(35)&&PreviewQuality.tier(35)!=PreviewQuality.tier(50)&&PreviewQuality.tier(85)!=PreviewQuality.tier(135)&&PreviewQuality.tier(135)!=PreviewQuality.tier(230),"Increasing digital crop changes the stream tier instead of reusing a native-sized source");
        byte[] y=new byte[100],u=new byte[30],v=new byte[30];for(int i=0;i<y.length;i++)y[i]=(byte)i;for(int i=0;i<u.length;i++){u[i]=(byte)(100+i);v[i]=(byte)(150+i);}ByteBuffer yy=ByteBuffer.wrap(y),uu=ByteBuffer.wrap(u),vv=ByteBuffer.wrap(v);yy.position(1);uu.position(2);vv.position(2);
        byte[] actual=YuvFrames.nv21(4,4,2,2,yy,10,1,uu,5,1,vv,5,1);byte[] expected=new byte[24];int at=0;for(int row=0;row<4;row++)for(int x=0;x<4;x++)expected[at++]=y[1+(row+2)*10+x+2];for(int row=0;row<2;row++)for(int x=0;x<2;x++){expected[at++]=v[2+(row+1)*5+x+1];expected[at++]=u[2+(row+1)*5+x+1];}
        check(Arrays.equals(actual,expected)&&yy.position()==1&&uu.position()==2&&vv.position()==2,"Bulk luma rows preserve padded source positions and chroma offsets");
        boolean rejected=false;try{PreviewQuality.region(4000,3000,Float.NaN);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Invalid ROI crop cannot allocate or address a camera plane");
        float[] guard=new float[16];int[] asymmetric={2,2,10,8};PreviewQuality.transform(guard,16,14,asymmetric);float topUV=guard[5]*(1-2/14f)+guard[13],bottomUV=guard[5]*(1-10/14f)+guard[13];check(near(1-topUV,0)&&near(1-bottomUV,1),"Asymmetric even-aligned ROI compensates the shader's Y flip without a two-pixel shift");
        System.out.println("ALL "+count+" PREVIEW DETAIL TESTS PASSED");
    }
}
