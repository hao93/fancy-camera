import com.silverroom.camera.LensWheelPolicy;
import com.silverroom.camera.ExposureValue;
import com.silverroom.camera.FilmWheelState;
import com.silverroom.camera.FilmCatalog;
import java.util.*;

public final class CompactControlsRegression {
    static int count;
    static void check(boolean v,String label){if(!v)throw new AssertionError(label);count++;System.out.println("PASS "+label);}
    static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
    public static void main(String[] args){
        boolean[] mainOnly={false,true,true,true,false,false,false};
        check(LensWheelPolicy.nearest(0,mainOnly)==1,"Missing ultrawide cannot become the release target");
        check(LensWheelPolicy.nearest(6,mainOnly)==3,"A fling past blocked telephoto settles on an available main focal");
        check(LensWheelPolicy.nearest(2,mainOnly)==2,"A directly tapped available focal keeps its own detent");
        check(LensWheelPolicy.nearest(4,new boolean[]{true,true,true,true,true,true,true})==4,"A genuine available telephoto retains its native detent");
        check(LensWheelPolicy.nearest(3,new boolean[7])==-1,"No available optical path produces no invented fallback");
        check(LensWheelPolicy.nearest(3,new boolean[0])==-1,"An empty availability update cannot invent a lens");
        check(LensWheelPolicy.nearest(5.5f,mainOnly)==3,"Grey telephoto gaps cannot become selected fling endpoints");
        check(LensWheelPolicy.nearest(Float.NaN,mainOnly)==-1,"Invalid lens coordinates cannot choose an arbitrary optical sensor");
        boolean within=true,roundTrip=true,monotonic=true;float previous=-3;
        for(int i=0;i<=40;i++){float value=ExposureValue.value(i);within&=value>=-2&&value<=2;roundTrip&=ExposureValue.progress(value)==i;monotonic&=value>previous;previous=value;}
        check(within,"Every slider tick stays inside the supported UI exposure range");
        check(roundTrip,"All 41 EV ticks round-trip between popup and compact value");
        check(monotonic,"Moving the EV slider right always increases the requested exposure");
        check(near(ExposureValue.value(-100),-2)&&near(ExposureValue.value(100),2),"EV slider values clamp at both limits");
        float value=0;for(int i=0;i<20;i++)value=ExposureValue.clamp(value+.1f);check(near(value,2),"Repeated plus taps reach +2 without floating-point drift");
        for(int i=0;i<40;i++)value=ExposureValue.clamp(value-.1f);check(near(value,-2),"Repeated minus taps reach -2 without floating-point drift");
        check(near(ExposureValue.clamp(100),2)&&near(ExposureValue.clamp(-100),-2),"Out-of-range direct EV values are clamped");
        check(near(ExposureValue.clamp(Float.NaN),0)&&near(ExposureValue.clamp(Float.POSITIVE_INFINITY),0),"Invalid EV input safely resets to zero");
        check(ExposureValue.label(0).equals("+0.0")&&ExposureValue.label(-.4f).equals("-0.4"),"Compact EV values retain a sign and one decimal place");
        List<Integer> all=new ArrayList<>();for(int id=0;id<FilmCatalog.NAMES.length;id++)all.add(id);FilmWheelState wheel=new FilmWheelState();wheel.reset(all,17);
        check(wheel.count()==FilmCatalog.NAMES.length&&wheel.selected()==17,"A recipe outside favorites is centred within the complete film reel");
        Set<Integer> seen=new HashSet<>();for(int i=0;i<FilmCatalog.NAMES.length;i++){wheel.position(i);seen.add(wheel.selected());}
        check(seen.size()==FilmCatalog.NAMES.length&&seen.contains(0)&&seen.contains(FilmCatalog.NAMES.length-1),"Dragging through the reel reaches every film recipe exactly once");
        wheel.reset(all,8);check(wheel.id(0)==0&&wheel.id(17)==17&&wheel.selected()==8,"Library selection centres the chosen recipe without reordering the all-film reel");
        System.out.println("ALL "+count+" COMPACT CONTROL TESTS PASSED");
    }
}
