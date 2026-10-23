import com.silverroom.camera.FilmWheelState;
import com.silverroom.camera.FilterShelf;
import java.util.*;

public final class FilmWheelRegression {
    static int count;
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);count++;System.out.println("PASS "+text);}
    static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
    public static void main(String[] args){
        FilmWheelState wheel=new FilmWheelState();List<Integer> pins=new ArrayList<>(Arrays.asList(6,7,1,4));wheel.reset(pins,7);
        check(wheel.selected()==7&&near(wheel.position(),1),"An existing film is centred when the reel opens");
        pins.clear();check(wheel.count()==4&&wheel.id(0)==6,"An active reel owns a stable snapshot of its film order");
        wheel.position(1.49f);check(wheel.selected()==7,"Dragging inside a detent keeps its selected film");
        wheel.position(1.51f);check(wheel.selected()==1,"Crossing the centre threshold selects the next film before settling");
        wheel.position(1.49f);check(wheel.selected()==7,"Reversing across a detent restores the previous film immediately");
        wheel.position(-100);check(wheel.selected()==6&&near(wheel.position(),0),"Dragging beyond the first film clamps at the first detent");
        wheel.position(100);check(wheel.selected()==4&&near(wheel.position(),3),"Dragging beyond the last film clamps at the final detent");
        wheel.position(wheel.position()-.3f);check(near(wheel.position(),2.7f),"Reversing at the edge responds without accumulated overscroll");
        wheel.position(1.2f);check(wheel.target(0)==1,"Slow release snaps to the nearest detent");
        check(wheel.target(8)==2&&wheel.target(-8)==0,"Fling direction advances or reverses through the film order");
        check(wheel.target(Float.MAX_VALUE)==3&&wheel.target(-Float.MAX_VALUE)==0,"Extreme fling speeds stay inside available films");
        check(wheel.target(Float.NaN)==1,"An invalid fling velocity safely falls back to nearest snapping");
        wheel.position(Float.NaN);check(near(wheel.position(),1.2f),"An invalid movement cannot corrupt the reel position");
        wheel.position(1.7f);int before=wheel.selected();wheel.position(wheel.index());check(wheel.selected()==before&&near(wheel.position(),2),"Interrupting a fling for capture keeps the currently visible film");
        boolean stable=true;for(int index=0;index<=3;index++){wheel.position(index);stable&=wheel.id(0)==6&&wheel.id(1)==7&&wheel.id(2)==1&&wheel.id(3)==4;}check(stable,"Live selections do not rebuild or reorder the scrolling reel");
        wheel.reset(Arrays.asList(6,6,-1,999,7),7);check(wheel.count()==2&&wheel.selected()==7,"Duplicate and obsolete saved IDs cannot create invalid detents");
        wheel.reset(Collections.emptyList(),-1);check(wheel.count()==1&&wheel.selected()==1&&wheel.target(12)==0,"An empty reel has one usable fallback and remains scroll-safe");
        List<Integer> common=Arrays.asList(6,7,1,4);wheel.reset(FilterShelf.visible(common,8),8);check(wheel.count()==4&&wheel.selected()==8&&common.equals(Arrays.asList(6,7,1,4)),"Selecting a hidden recipe centres it without changing saved favorites");
        wheel.reset(FilterShelf.visible(common,6),6);check(wheel.count()==4&&wheel.id(3)==4,"Returning to a saved favorite restores the regular shortcut reel");
        wheel.reset(Arrays.asList(0,1,2,3,4,5,6,7,8,9),4);boolean snapped=true;float start=wheel.position();int target=wheel.target(8);for(int i=1;i<=20;i++)wheel.position(start+(target-start)*i/20f);snapped=near(wheel.position(),target)&&wheel.selected()==target;check(snapped,"Animated settling terminates on the same film as the selected detent");
        System.out.println("ALL "+count+" FILM WHEEL TESTS PASSED");
    }
}
