import com.silverroom.camera.*;
import java.util.*;

public final class PhotoBrowseRegression {
    static int count;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
    static PhotoBrowseGesture.Action swipe(float x,float y,float zoom){PhotoBrowseGesture gesture=new PhotoBrowseGesture();gesture.begin(200,300,zoom);return gesture.finish(x,y,80,100,8);}
    public static void main(String[] args){
        check(swipe(50,305,1)==PhotoBrowseGesture.Action.NEXT,"Left swipe selects the next photo in album order");
        check(swipe(350,295,1)==PhotoBrowseGesture.Action.PREVIOUS,"Right swipe selects the preceding photo");
        check(swipe(205,440,1)==PhotoBrowseGesture.Action.CLOSE,"Down swipe exits the detail viewer without deleting");
        check(swipe(195,160,1)==PhotoBrowseGesture.Action.DELETE,"Up swipe preserves the existing delete-confirmation action");
        check(swipe(180,290,1)==PhotoBrowseGesture.Action.NONE,"Small finger motion is not treated as navigation");
        check(swipe(50,150,1)==PhotoBrowseGesture.Action.NONE,"Ambiguous diagonal gestures cannot dismiss or delete a photo");
        check(swipe(50,300,3)==PhotoBrowseGesture.Action.NONE&&swipe(200,500,3)==PhotoBrowseGesture.Action.NONE,"Magnified dragging cannot navigate or close the viewer");
        PhotoBrowseGesture gesture=new PhotoBrowseGesture();gesture.begin(200,300,1);gesture.block();check(gesture.finish(50,300,80,100,8)==PhotoBrowseGesture.Action.NONE,"A multi-touch sequence remains excluded from single-finger paging");
        gesture.begin(200,300,1);gesture.drag(180,299,8);check(gesture.finish(50,150,80,100,8)==PhotoBrowseGesture.Action.NONE,"Turning a horizontal drag diagonal cannot become an upward delete");
        gesture.begin(200,300,1);gesture.drag(201,280,8);check(gesture.finish(50,150,80,100,8)==PhotoBrowseGesture.Action.NONE,"A vertical drag turning diagonal springs back instead of firing another action");
        gesture.begin(200,300,1);gesture.drag(180,300,8);check(gesture.finish(50,290,80,100,8)==PhotoBrowseGesture.Action.NEXT,"Minor vertical jitter does not interrupt horizontal browsing");
        check(gesture.finish(50,290,80,100,8)==PhotoBrowseGesture.Action.NONE,"A released gesture cannot issue a duplicate page request");
        check(swipe(200,500,Float.NaN)==PhotoBrowseGesture.Action.NONE,"Invalid zoom state cannot accidentally close the viewer");
        String a="20261023_170000_001",b="20261023_170001_002",c="20261023_170002_003";List<String> source=new ArrayList<>(Arrays.asList(c,b,a));PhotoPager pager=new PhotoPager(source,b);
        check(pager.current().equals(b)&&pager.position()==2&&pager.count()==3,"Opening an arbitrary photo centres the correct album position");
        source.clear();check(pager.count()==3,"An open viewer owns a stable album snapshot");long old=pager.revision();
        check(pager.move(1)&&pager.current().equals(a)&&pager.position()==3,"Next follows the same newest-to-oldest order as the list");
        check(!pager.matches(b,old),"A previous photo's late decode cannot replace the current page");long end=pager.revision();
        check(!pager.move(1)&&pager.revision()==end&&pager.current().equals(a),"Last-photo boundary does not wrap or invalidate the displayed image");
        check(pager.move(-1)&&pager.current().equals(b)&&!pager.matches(b,old),"Returning to the same photo does not revive an earlier decode token");
        check(pager.matches(b,pager.revision()),"Only the current photo and current request revision may update the viewer");
        check(pager.move(-1)&&!pager.canMove(-1)&&pager.canMove(1),"First-photo boundary correctly disables backward movement");
        check(!pager.move(20)&&pager.current().equals(c),"Invalid navigation deltas cannot skip arbitrary pages");
        PhotoPager single=new PhotoPager(Arrays.asList(a,a,"../bad"),a);check(single.count()==1&&!single.move(-1)&&!single.move(1),"A one-photo album supports boundaries without duplicates or invalid IDs");
        long token=pager.revision();pager.close();check(!pager.matches(c,token)&&!pager.move(1),"Dismissing the detail viewer invalidates all pending image callbacks");
        boolean rejected=false;try{new PhotoPager(Arrays.asList(a,b),c);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"A removed initial photo cannot create an inconsistent detail cursor");
        System.out.println("ALL "+count+" PHOTO BROWSING TESTS PASSED");
    }
}
