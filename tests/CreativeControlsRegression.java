import com.silverroom.camera.*;
import android.graphics.Bitmap;
import java.util.*;

public final class CreativeControlsRegression {
    static int count;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
    static int[] pixels(Bitmap b){int[] out=new int[b.getWidth()*b.getHeight()];b.getPixels(out,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());return out;}
    static boolean near(float a,float b){return Math.abs(a-b)<.00001f;}
    public static void main(String[] args){
        check(FilmStrength.clamp(-1)==0&&FilmStrength.clamp(2)==1&&FilmStrength.clamp(Float.NaN)==1&&FilmStrength.clamp(Float.POSITIVE_INFINITY)==1,"Invalid persisted strength values recover to a usable range");
        check(FilmStrength.percent(.374f)==37&&FilmStrength.percent(.999f)==100,"Strength percentages use consistent rounding");
        Film.Settings setting=new Film.Settings();check(setting.strength==1,"Old photos and fresh settings default to the complete existing look");setting.strength=.37f;setting.stock=50;setting.exposure=1;Film.Settings copy=setting.copy();setting.strength=.9f;check(copy.strength==.37f&&copy.stock==50&&copy.exposure==1,"A shutter/preview copy freezes strength independently from later changes");
        int w=70,h=50;int[] source=new int[w*h];Random random=new Random(1764);for(int i=0;i<source.length;i++)source[i]=0xff000000|random.nextInt(0x1000000);Bitmap input=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);input.setPixels(source,0,w,0,0,w,h);
        boolean zero=true,half=true,full=true;Set<Integer> fullHashes=new HashSet<>();
        for(int id=0;id<FilmCatalog.NAMES.length;id++){Film.Settings s=new Film.Settings();s.stock=id;s.strength=1;int[] developed=pixels(Film.develop(input,s));fullHashes.add(Arrays.hashCode(developed));s.strength=0;zero&=Arrays.equals(source,pixels(Film.develop(input,s)));s.strength=.5f;int[] mixed=pixels(Film.develop(input,s));for(int i=0;i<mixed.length;i++)for(int shift=0;shift<=16;shift+=8){int expected=Math.round((((source[i]>>shift)&255)+((developed[i]>>shift)&255))/2f);half&=Math.abs(((mixed[i]>>shift)&255)-expected)<=1;}s.strength=1;full&=Arrays.equals(developed,pixels(Film.develop(input,s)));}
        check(zero,"Zero percent returns the original pixels for all catalog looks including grain, leak and tilt");check(half,"Half strength linearly blends actual full-look pixels across every recipe");check(full&&fullHashes.size()==FilmCatalog.NAMES.length,"Full strength retains deterministic distinct existing recipe exports");
        boolean exposure=true;for(float ev:new float[]{-2,-1,1,2}){Film.Settings s=new Film.Settings();s.stock=49;s.strength=0;s.exposure=ev;int[] result=pixels(Film.develop(input,s));for(int i=0;i<result.length;i++)for(int shift=0;shift<=16;shift+=8){int expected=Math.min(255,Math.round(((source[i]>>shift)&255)*(float)Math.pow(2,ev)));exposure&=((result[i]>>shift)&255)==expected;}}check(exposure,"Disabling a look preserves independent positive and negative exposure adjustments");
        check(Arrays.equals(pixels(input),source),"Strength development never mutates an original negative");
        float[] mixValues={0,.13f,.5f,.88f,1};boolean scalar=true;for(float amount:mixValues)for(float gain:new float[]{.25f,1,4}){int original=0xff1987f2,developed=0xffef6413,got=FilmStrength.blend(original,developed,amount,gain);for(int shift=0;shift<=16;shift+=8){float raw=Math.max(0,Math.min(1,((original>>shift)&255)/255f*gain)),effect=((developed>>shift)&255)/255f;int shader=Math.round((raw+(effect-raw)*amount)*255);scalar&=Math.abs(((got>>shift)&255)-shader)<=1;}}check(scalar,"The export strength blend matches the GPU mix and clamp order");
        List<Integer> favorites=FilterShelf.parse(null,true);check(favorites.equals(Arrays.asList(6,7,1,4)),"A first install gets the four familiar defaults");
        check(FilterShelf.parse("",true).isEmpty(),"An intentionally empty favorites shelf stays empty after restart");
        for(int id=0;id<FilmCatalog.NAMES.length;id++)if(!favorites.contains(id))favorites=FilterShelf.toggle(favorites,id);check(favorites.size()==FilmCatalog.NAMES.length&&favorites.subList(0,4).equals(Arrays.asList(6,7,1,4)),"Adding all catalog styles never evicts or reorders existing favorites");
        check(FilterShelf.parse(FilterShelf.encode(favorites),true).equals(favorites),"An expanded favorites shelf persists every ID in its chosen order");
        List<Integer> prior=new ArrayList<>(favorites),moved=FilterShelf.move(favorites,FilmCatalog.NAMES.length-1,0);check(moved.get(0).equals(prior.get(FilmCatalog.NAMES.length-1))&&moved.size()==FilmCatalog.NAMES.length&&new HashSet<>(moved).size()==FilmCatalog.NAMES.length&&favorites.equals(prior),"Moving the last favorite to the front preserves membership and leaves the prior snapshot unchanged");
        moved=FilterShelf.move(moved,0,FilmCatalog.NAMES.length-1);check(moved.equals(prior),"Dragging a favorite back restores its exact prior order");
        check(FilterShelf.move(prior,-1,3).equals(prior)&&FilterShelf.move(prior,1,99).equals(prior),"Invalid drag targets cannot drop or duplicate favorites");
        List<Integer> all=new ArrayList<>(favorites);for(int id:all)favorites=FilterShelf.toggle(favorites,id);check(favorites.isEmpty(),"Removing the final favorite does not add an unrelated default style");
        List<Integer> ordered=Arrays.asList(50,24,6,28);check(FilmGroups.ids(FilmGroups.FAVORITES,ordered).equals(ordered),"The main favorites reel follows the saved drag order");
        List<Integer> snapshot=FilmGroups.ids(FilmGroups.FAVORITES,ordered);snapshot.clear();check(ordered.size()==4,"Reel membership is a separate snapshot from stored favorites");
        check(FilmGroups.choose(FilmGroups.FAVORITES,23,24,ordered)==24&&FilmGroups.choose(FilmGroups.FAVORITES,23,23,ordered)==50&&FilmGroups.choose(FilmGroups.FAVORITES,6,24,ordered)==6,"Favorites category restores a member selection or its first ordered style");
        check(FilmGroups.safe(FilmGroups.FAVORITES)==FilmGroups.FAVORITES&&FilmGroups.libraryLabels().length==13,"Favorites group persists without duplicating the library's category tabs");
        MeteringLock lock=new MeteringLock();check(!lock.active()&&!lock.ae()&&!lock.locked(),"The camera starts with both lock intentions released");long token=lock.begin();check(lock.state()==MeteringLock.FOCUSING&&lock.active()&&!lock.ae(),"A long press starts autofocus before requesting AE lock");
        check(lock.result(token,null,null,null)==MeteringLock.FOCUSING,"Missing camera metadata cannot claim a successful lock");
        check(lock.result(token,2,2,false)==MeteringLock.FOCUSING,"Passive autofocus success alone does not mean focus is locked");
        check(lock.result(token,4,1,false)==MeteringLock.FOCUSING,"Exposure still searching delays the AE lock request");
        check(lock.result(token,4,2,false)==MeteringLock.WAIT_AE&&lock.ae()&&!lock.locked(),"A focused AF lock and converged exposure advance to AE lock confirmation");
        check(lock.result(token,4,3,false)==MeteringLock.WAIT_AE,"An AE state without the applied lock bit cannot display locked");
        check(lock.result(token,4,3,true)==MeteringLock.LOCKED&&lock.locked(),"Only applied AE lock plus locked AF confirms the combined lock");
        check(!lock.timeout(token)&&lock.locked(),"The old acquisition timeout cannot release a confirmed lock");
        lock.clear();check(!lock.active()&&lock.result(token,4,3,true)==MeteringLock.IDLE,"A late camera result cannot relock after an explicit release");
        long second=lock.begin();check(lock.result(token,4,3,true)==MeteringLock.FOCUSING,"A previous focus operation cannot confirm a later long press");
        check(!lock.timeout(token)&&lock.active(),"A stale timeout cannot cancel a new long press");
        check(lock.timeout(second)&&!lock.active(),"A current operation times out honestly instead of showing a permanent pending lock");
        token=lock.begin();check(lock.result(token,5,2,false)==MeteringLock.FAILED&&!lock.active(),"The camera's not-focused-locked result cancels the combined lock");
        token=lock.begin();check(lock.result(token,4,4,false)==MeteringLock.WAIT_AE,"A flash-required but stable AE result can request exposure lock");lock.result(token,4,3,true);check(lock.result(token,4,2,false)==MeteringLock.FAILED&&!lock.active(),"Loss of applied exposure lock removes the locked state");
        token=lock.begin();lock.result(token,4,2,false);lock.result(token,4,3,true);check(lock.result(token,1,3,true)==MeteringLock.FAILED&&!lock.active(),"An AF restart removes the combined lock rather than leaving a stale badge");
        token=lock.begin();lock.clear();long fresh=lock.begin();check(fresh>token&&lock.epoch()==fresh,"Every camera reset advances the operation epoch");
        System.out.println("ALL "+count+" STRENGTH / FAVORITES / METERING LOCK TESTS PASSED");
    }
}
