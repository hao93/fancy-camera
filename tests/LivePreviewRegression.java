import com.silverroom.camera.PreviewPixels;
import com.silverroom.camera.PreviewBudget;
import com.silverroom.camera.PreviewChannel;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class LivePreviewRegression {
    static int count;
    static void check(boolean v,String text){if(!v)throw new AssertionError(text);count++;System.out.println("PASS "+text);}
    public static void main(String[] args)throws Exception{
        ByteBuffer rgba=ByteBuffer.wrap(new byte[]{0,0,(byte)255,0,(byte)255,(byte)255,(byte)255,0,(byte)255,0,0,0,0,(byte)255,0,0});
        int[] pixels=PreviewPixels.argb(rgba,2,2);check(Arrays.equals(pixels,new int[]{0xffff0000,0xff00ff00,0xff0000ff,0xffffffff}),"Readback flips GL bottom-up rows and preserves red green blue channels");
        check(rgba.position()==0,"Pixel conversion leaves the producer buffer position unchanged");
        rgba.order(ByteOrder.LITTLE_ENDIAN);int[] little=PreviewPixels.argb(rgba,2,2);rgba.order(ByteOrder.BIG_ENDIAN);check(Arrays.equals(little,PreviewPixels.argb(rgba,2,2)),"RGBA conversion is independent of native integer byte order");
        ByteBuffer padded=ByteBuffer.allocate(24);padded.putInt(0).put(rgba.array()).putInt(0);padded.position(4);padded.limit(20);check(Arrays.equals(PreviewPixels.argb(padded,2,2),pixels),"Readback accepts a bounded buffer with a nonzero position");
        boolean rejected=false;try{PreviewPixels.argb(ByteBuffer.allocate(3),1,1);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Truncated readbacks are rejected instead of displaying corrupt previews");
        rejected=false;try{PreviewPixels.argb(ByteBuffer.allocate(513*513*4),513,513);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Preview conversion cannot allocate full-resolution photo buffers");
        boolean bounded=true,aspect=true;for(int[] source:new int[][]{{576,900},{1440,1080},{4096,3072},{3072,4096},{200,200}}){int[] size=PreviewPixels.size(source[0],source[1]);bounded&=size[0]<=PreviewPixels.DEFAULT_EDGE&&size[1]<=PreviewPixels.DEFAULT_EDGE;aspect&=Math.abs(size[0]/(float)size[1]-source[0]/(float)source[1])<.01f;}
        check(bounded,"Default popup sources are capped at 320 pixels on their long edge");
        check(aspect,"Portrait landscape and square viewfinders retain their preview aspect");
        check(Arrays.equals(PreviewPixels.size(64,32),new int[]{64,32}),"Small imported negatives are not enlarged for preview processing");
        rejected=false;try{PreviewPixels.size(0,300);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"Unlaid-out viewfinders cannot create invalid readback targets");
        PreviewBudget budget=new PreviewBudget();check(budget.acquire(0),"Opening a popup admits the first preview immediately");
        check(!budget.acquire(5000),"Slow CPU or UI work blocks newer frames instead of queueing them");
        budget.complete();check(!budget.acquire(249)&&budget.acquire(250),"Preview admission is limited to four batches per second");
        budget.complete();check(budget.acquire(500),"A finished batch allows the next current frame");
        budget.close();budget.complete();check(!budget.acquire(10000)&&budget.closed(),"Dismissal permanently blocks late work even after an outstanding completion");
        PreviewBudget contested=new PreviewBudget();CountDownLatch start=new CountDownLatch(1);AtomicInteger admitted=new AtomicInteger();List<Thread> threads=new ArrayList<>();
        for(int i=0;i<12;i++){Thread t=new Thread(()->{try{start.await();if(contested.acquire(1000))admitted.incrementAndGet();}catch(InterruptedException error){throw new RuntimeException(error);}});threads.add(t);t.start();}start.countDown();for(Thread t:threads)t.join();check(admitted.get()==1,"Concurrent producers can admit only one preview batch");
        PreviewChannel<Object> channel=new PreviewChannel<>();Object old=new Object(),next=new Object();channel.set(old);channel.set(next);check(!channel.remove(old)&&channel.get()==next,"An old popup cannot detach a newer popup's camera feed");
        check(channel.remove(next)&&channel.get()==null,"Closing the active popup removes its own camera feed");
        check(!channel.remove(old)&&channel.get()==null,"Repeated stale cleanup leaves an idle camera feed idle");
        PreviewBudget reopened=new PreviewBudget();check(reopened.acquire(0),"Reopening uses a fresh budget rather than an old closed session");
        System.out.println("ALL "+count+" LIVE PREVIEW TESTS PASSED");
    }
}

