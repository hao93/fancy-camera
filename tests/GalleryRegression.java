import com.silverroom.camera.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class GalleryRegression {
    static int count;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
    static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
    static void write(File folder,String name)throws Exception{Files.write(new File(folder,name).toPath(),name.getBytes(StandardCharsets.UTF_8));}
    static void negative(File folder,String id)throws Exception{write(folder,id+"_original.jpg");write(folder,id+".jpg");write(folder,id+".json");}
    static File stage(File folder,String id,boolean committed)throws Exception{File d=new File(folder,".delete-"+UUID.randomUUID());if(!d.mkdir())throw new IOException();Files.write(new File(d,".record").toPath(),id.getBytes(StandardCharsets.UTF_8));if(committed)write(d,".committed");return d;}
    public static void main(String[] args)throws Exception{
        String a="20261023_170000_001",b="20261023_170001_002",c="20261023_170002_003";
        check(NegativeStore.valid(a)&&!NegativeStore.valid("../"+a)&&!NegativeStore.valid(a+"/x")&&!NegativeStore.valid("20261023"),"Deletion accepts only complete private negative IDs, never paths");
        NegativeSelection selection=new NegativeSelection();selection.items(Arrays.asList(a,b,c,a,"../bad"));selection.toggle(a);check(selection.count()==1&&selection.contains(a),"A single selected negative is counted once");selection.toggle(b);check(selection.snapshot().equals(Arrays.asList(a,b)),"Multiple selections preserve their stable snapshot order");
        List<String> snapshot=selection.snapshot();selection.toggle(c);check(snapshot.size()==2&&selection.count()==3,"A confirmation snapshot cannot be changed by later selection taps");selection.all();check(selection.count()==0&&!selection.every(),"Select-all toggles to no selection when every item was selected");selection.all();check(selection.every()&&selection.count()==3,"Select-all includes all available negatives exactly once");selection.items(Arrays.asList(b,c));check(!selection.contains(a)&&selection.count()==2,"Refreshing after deletion drops unavailable IDs from selection");selection.toggle(a);check(selection.count()==2,"Deleted or unknown IDs cannot be selected again");selection.clear();check(selection.count()==0,"Cancel selection clears the entire pending batch");
        PhotoViewport view=new PhotoViewport();view.size(400,300,400,300);view.scale(2,300,150);check(near(view.zoom,2)&&near(view.x,-100),"Pinch zoom preserves the image location beneath the finger focus");view.scale(100,200,150);check(near(view.zoom,6),"Photo magnification stops at six times fit");view.pan(10000,-10000);check(near(view.x,1000)&&near(view.y,-750),"Pan clamps both axes to the scaled image bounds");view.scale(0.01f,0,0);check(near(view.zoom,1)&&near(view.x,0)&&near(view.y,0),"Zooming out returns to the complete fitted photo");view.scale(Float.NaN,0,0);view.pan(Float.NaN,100);check(near(view.zoom,1)&&near(view.x,0)&&near(view.y,0),"Invalid gesture values cannot corrupt image coordinates");view.size(400,600,400,300);view.scale(2,200,300);view.pan(30,100);check(near(view.x,30)&&near(view.y,0),"A letterboxed photo cannot be dragged into extra empty space on its covered axis");view.reset();check(near(view.zoom,1)&&near(view.x,0),"Opening a new photo resets prior zoom and pan");
        PhotoBrowseGesture swipe=new PhotoBrowseGesture();swipe.begin(100,300,1);check(near(swipe.drag(110,160,8)[1],-140)&&swipe.finish(110,160,100,100,8)==PhotoBrowseGesture.Action.DELETE,"A deliberate single-finger upward swipe at fit scale requests deletion");
        swipe.begin(100,300,1);check(swipe.finish(100,250,100,100,8)==PhotoBrowseGesture.Action.NONE,"A short upward movement cannot request deletion");
        swipe.begin(100,300,1);check(swipe.finish(100,450,100,100,8)==PhotoBrowseGesture.Action.CLOSE,"Downward swipes return to the list instead of deleting");
        swipe.begin(100,300,1);check(swipe.finish(400,160,100,100,8)==PhotoBrowseGesture.Action.PREVIOUS,"A predominantly horizontal swipe browses instead of deleting");
        swipe.begin(100,300,3);check(swipe.finish(100,0,100,100,8)==PhotoBrowseGesture.Action.NONE,"Dragging a magnified photo cannot trigger deletion");
        swipe.begin(100,300,1);swipe.block();check(swipe.finish(100,0,100,100,8)==PhotoBrowseGesture.Action.NONE,"A pinch sequence stays blocked even when it zooms back to fit");
        swipe.begin(100,300,1);check(swipe.finish(100,100,100,100,8)==PhotoBrowseGesture.Action.DELETE&&swipe.finish(100,0,100,100,8)==PhotoBrowseGesture.Action.NONE,"A gesture release can request deletion at most once");
        swipe.begin(100,300,1);swipe.block();check(swipe.finish(100,0,100,100,8)==PhotoBrowseGesture.Action.NONE,"Cancelled gestures never become delete actions");
        Path parent=Paths.get(args[0]);Files.createDirectories(parent);Path temp=Files.createTempDirectory(parent,"negative-store-");File folder=temp.resolve("negatives").toFile();folder.mkdir();File album=temp.resolve("system-album.jpg").toFile();write(temp.toFile(),album.getName());
        try{
            negative(folder,a);negative(folder,b);negative(folder,c);write(folder,"not-a-negative.json");
            check(NegativeStore.list(folder).equals(Arrays.asList(c,b,a)),"Gallery lists valid records newest first and ignores unrelated files");
            check(NegativeStore.delete(folder,b)&&!new File(folder,b+".json").exists()&&!new File(folder,b+".jpg").exists()&&!new File(folder,b+"_original.jpg").exists(),"Single delete removes metadata, developed copy and original together");
            check(album.isFile()&&new File(folder,a+".jpg").isFile()&&new File(folder,c+".jpg").isFile(),"Single delete leaves system-album and unselected negative files untouched");
            check(NegativeStore.delete(folder,b),"Deleting an already absent negative is harmless");
            boolean rejected=false;try{NegativeStore.delete(folder,"../system-album");}catch(IllegalArgumentException e){rejected=true;}check(rejected&&album.exists(),"Path traversal cannot delete a file outside the negative directory");
            File interrupted=stage(folder,a,false);Files.move(new File(folder,a+"_original.jpg").toPath(),new File(interrupted,a+"_original.jpg").toPath());NegativeStore.recover(folder);check(new File(folder,a+"_original.jpg").isFile()&&new File(folder,a+".json").isFile()&&!interrupted.exists(),"Recovery restores a deletion interrupted before its commit marker");
            File committed=stage(folder,c,true);for(String suffix:new String[]{"_original.jpg",".jpg",".json"})Files.move(new File(folder,c+suffix).toPath(),new File(committed,c+suffix).toPath());NegativeStore.recover(folder);check(!committed.exists()&&!new File(folder,c+".json").exists()&&album.exists(),"Recovery finishes only a committed private deletion, preserving the album");
            new File(folder,b+".jpg").mkdir();write(folder,b+"_original.jpg");rejected=false;try{NegativeStore.delete(folder,b);}catch(IOException e){rejected=true;}check(rejected&&new File(folder,b+"_original.jpg").isFile(),"An unexpected component directory is rejected before moving any original");
            File conflict=stage(folder,a,false);write(conflict,a+"_original.jpg");NegativeStore.recover(folder);check(conflict.exists()&&new File(conflict,a+"_original.jpg").isFile()&&new File(folder,a+"_original.jpg").isFile(),"Recovery never overwrites a surviving original when a conflict exists");
            check(NegativeStore.delete(folder,a)&&NegativeStore.list(folder).isEmpty()&&album.isFile(),"Removing the remaining selected negative leaves an empty gallery and preserves album exports");
        }finally{try(java.util.stream.Stream<Path> paths=Files.walk(temp)){Path[] owned=paths.sorted(Comparator.reverseOrder()).toArray(Path[]::new);for(Path p:owned)Files.deleteIfExists(p);}}
        System.out.println("ALL "+count+" GALLERY DELETE / ZOOM TESTS PASSED");
    }
}

