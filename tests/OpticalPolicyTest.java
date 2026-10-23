import com.silverroom.camera.LensPlan;
import com.silverroom.camera.LensPlan.Route;
import com.silverroom.camera.YuvFrames;
import java.nio.ByteBuffer;
import java.util.*;

public final class OpticalPolicyTest {
    static int passed;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);passed++;System.out.println("PASS "+message);}
    static Route choose(List<Route> routes,int mm,Set<String> failed){return LensPlan.select(routes,mm,false,failed);}
    static byte[] convert(int w,int h,ByteBuffer y,int ys,int yp,ByteBuffer u,int us,int up,ByteBuffer v,int vs,int vp){return YuvFrames.nv21(w,h,0,0,y,ys,yp,u,us,up,v,vs,vp);}
    public static void main(String[] args){
        Route wide=new Route("0","wide",14,14,false,false),main=new Route("0","main",23,23,false,false),tele=new Route("0","tele",85,85,false,false);
        List<Route> all=Arrays.asList(main,wide,tele);Set<String> none=Collections.emptySet();
        for(int mm:new int[]{14,23,35,50,85,135,230}){Route r=choose(all,mm,none);check(r!=null&&r.base==LensPlan.base(mm),mm+"mm selects its required optical sensor");}
        for(int mm:new int[]{85,135,230})check(choose(Arrays.asList(main,wide),mm,none)==null,mm+"mm cannot be replaced by main-camera digital zoom");
        check(choose(Arrays.asList(main,tele),14,none)==null,"Missing ultrawide is unavailable, not a zoomed main camera");
        Route directTele=new Route("4",null,85,84,false,false);check(choose(Arrays.asList(tele,directTele,main),135,none)==directTele,"Standalone tele ID preferred over logical binding");
        Set<String> rejected=new HashSet<>(Arrays.asList(directTele.key()));check(choose(Arrays.asList(tele,directTele,main),135,rejected)==tele,"Rejected tele route retries another tele route");
        rejected.add(tele.key());check(choose(Arrays.asList(tele,directTele,main),135,rejected)==null,"All tele routes rejected disables tele group");
        Route defaultMain=new Route("0",null,23,23,false,true);check(choose(Arrays.asList(defaultMain,main),23,none)==main,"Explicit physical main preferred to system default");
        Route front=new Route("1",null,0,25,true,false);check(LensPlan.select(Arrays.asList(main,front),23,true,none)==front,"Front selection never picks a rear sensor");
        check(choose(Arrays.asList(front),23,none)==null,"Rear selection never picks front camera");
        for(int mm:new int[]{14,23,85})check(LensPlan.crop(mm)==1,"Optical anchor "+mm+"mm applies zero digital crop");
        check(Math.abs(LensPlan.crop(35)-35f/23)<.00001,"35mm crops only main base");
        check(Math.abs(LensPlan.crop(230)-230f/85)<.00001,"230mm crops only tele base");
        check(Arrays.equals(LensPlan.cropSize(4000,3000,1),new int[]{4000,3000}),"Optical anchors retain the full pixel rectangle");
        check(Arrays.equals(LensPlan.cropSize(4000,3000,2),new int[]{2000,1500}),"Digital extension keeps real pixels without upsampling");
        check(LensPlan.identify(14)==14&&LensPlan.identify(23)==23&&LensPlan.identify(85)==85,"Metadata identifies X100 Ultra native lenses");
        check(LensPlan.identify(0)==0&&LensPlan.identify(Float.NaN)==0&&LensPlan.identify(36)==0,"Missing or unrelated metadata is not invented as a native lens");
        byte[] y={1,2,3,4,5,6,7,8},u={21,22},v={31,32},expected={1,2,3,4,5,6,7,8,31,21,32,22};
        check(Arrays.equals(convert(4,2,ByteBuffer.wrap(y),4,1,ByteBuffer.wrap(u),2,1,ByteBuffer.wrap(v),2,1),expected),"Physical YUV planar conversion preserves luma and chroma");
        byte[] paddedY={1,2,3,4,99,99,5,6,7,8,99,99},vu={31,21,32,22};ByteBuffer vb=ByteBuffer.wrap(vu),ub=ByteBuffer.wrap(vu);ub.position(1);
        check(Arrays.equals(convert(4,2,ByteBuffer.wrap(paddedY),6,1,ub,4,2,vb,4,2),expected),"Physical YUV handles row padding, pixel stride 2 and shared chroma buffers");
        byte[] offsetY={99,99,99,1,2,3,4,5,6,7,8};ByteBuffer ob=ByteBuffer.wrap(offsetY);ob.position(3);
        check(Arrays.equals(convert(4,2,ob,4,1,ByteBuffer.wrap(u),2,1,ByteBuffer.wrap(v),2,1),expected),"Physical YUV respects ByteBuffer positions");
        byte[] largeY=new byte[60],largeU=new byte[18],largeV=new byte[18];for(int row=0;row<6;row++)for(int col=0;col<8;col++)largeY[row*10+col]=(byte)(row*8+col);
        for(int row=0;row<3;row++)for(int col=0;col<4;col++){largeU[row*6+col]=(byte)(100+row*4+col);largeV[row*6+col]=(byte)(150+row*4+col);}
        byte[] roi=YuvFrames.nv21(4,2,2,2,ByteBuffer.wrap(largeY),10,1,ByteBuffer.wrap(largeU),6,1,ByteBuffer.wrap(largeV),6,1);
        check(Arrays.equals(roi,new byte[]{18,19,20,21,26,27,28,29,(byte)155,105,(byte)156,106}),"Physical YUV honors image crop origin and plane padding");
        boolean invalid=false;try{convert(3,2,ByteBuffer.wrap(y),4,1,ByteBuffer.wrap(u),2,1,ByteBuffer.wrap(v),2,1);}catch(IllegalArgumentException e){invalid=true;}check(invalid,"Malformed YUV frame rejected instead of producing corrupt photos");
        // Regression fixture: the user's Android 16 report exposes ID 0 / ID 1,
        // both non-logical, and no physical children. This is NOT a session failure.
        Route reportedMain=new Route("0",null,23,23,false,false),reportedFront=new Route("1",null,0,24,true,false);
        List<Route> reported=Arrays.asList(reportedMain,reportedFront);
        check(LensPlan.state(reported,14,none,none)==LensPlan.State.MISSING,"User Android 16 inventory: ultrawide was not returned, not rejected during output setup");
        check(LensPlan.state(reported,85,none,none)==LensPlan.State.MISSING,"User Android 16 inventory: telephoto was not returned, not rejected during output setup");
        check(LensPlan.state(reported,23,none,none)==LensPlan.State.CANDIDATE,"Returned main-camera metadata still requires preview validation");
        check(LensPlan.state(reported,23,none,Collections.singleton(reportedMain.key()))==LensPlan.State.VERIFIED,"Main camera marked verified only after a preview result");
        check(LensPlan.state(all,85,Collections.singleton(tele.key()),none)==LensPlan.State.REJECTED,"Returned tele metadata with failed output gets a different state from missing tele metadata");
        check(LensPlan.state(Arrays.asList(tele,directTele),85,Collections.singleton(tele.key()),none)==LensPlan.State.CANDIDATE,"One rejected tele route does not hide another untested tele route");
        check(LensPlan.state(all,85,Collections.singleton(tele.key()),Collections.singleton(tele.key()))==LensPlan.State.REJECTED,"A previously verified route that later fails is no longer reported ready");
        List<String> extra=LensPlan.extraCandidates(new HashSet<>(Arrays.asList("0","1")));
        check(extra.size()==30&&!extra.contains("0")&&!extra.contains("1")&&extra.contains("2")&&extra.contains("3"),"Limited public inventory still permits bounded metadata-only auxiliary queries");
        List<String> withPhysical=LensPlan.extraCandidates(new HashSet<>(Arrays.asList("0","1","2")));
        check(!withPhysical.contains("2")&&withPhysical.size()==29,"Known physical IDs are not re-probed as extra standalone candidates");
        check(new HashSet<>(extra).size()==extra.size()&&extra.stream().allMatch(s->Integer.parseInt(s)>=0&&Integer.parseInt(s)<32),"Extra queries are unique and bounded to 32 numeric IDs");
        check(choose(Arrays.asList(new Route("2",null,23,23,false,false)),85,none)==null,"A readable ID 2 with main-camera optics is never assumed to be telephoto");
        System.out.println("ALL "+passed+" OPTICAL ROUTING / IMAGE TESTS PASSED");
    }
}
