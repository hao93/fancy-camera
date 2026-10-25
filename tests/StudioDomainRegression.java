package com.silverroom.camera;
import java.util.*;import org.json.*;

/** Numeric reference cases and immutable capture/archive boundaries, without Android hardware. */
public final class StudioDomainRegression {
    static int passed;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);passed++;System.out.println("PASS "+name);}
    static boolean near(double a,double b){return Math.abs(a-b)<Math.max(.000001,Math.abs(b)*.00001);}
    static void rejects(Runnable r,String name){boolean rejected=false;try{r.run();}catch(IllegalArgumentException e){rejected=true;}check(rejected,name);}
    public static void main(String[] args)throws Exception{
        check(near(MeterMath.ev100(16,1d/125,100),14.9657842847),"Sunny-16 native exposure computes EV100 near 15");
        check(near(MeterMath.ev100(8,.008,400),10.9657842847),"Native sensor ISO is normalized to ISO100 exactly once");
        double ev=MeterMath.ev100(8,.008,400);
        check(near(MeterMath.seconds(ev,8,400,0,0),.008),"Aperture-priority target reproduces native exposure");
        check(near(MeterMath.aperture(ev,.008,400,0,0),8),"Shutter-priority calculation is reciprocal to aperture priority");
        check(near(MeterMath.seconds(ev,8,MeterMath.ei(400,1),0,0),.004),"One-stop push is represented through EI once");
        check(near(MeterMath.seconds(ev,8,400,1,0),.016),"Positive compensation doubles exposure rather than darkening it");
        check(near(MeterMath.seconds(ev,8,800,1,3),.064),"EI, compensation and ND compose without double counting");
        check(near(MeterMath.reciprocity(10,MeterMath.reciprocityFactor("HP5+")),20.41737945),"Published HP5+ exponent applies after filter compensation");
        check(MeterMath.reciprocity(.5,1.43)==.5&&MeterMath.reciprocityFactor("unknown")==1,"Short exposure and unknown film do not acquire guessed reciprocity corrections");
        check(near(MeterMath.hyperfocal(50,8,.03),10.4666667),"50mm f8 hyperfocal distance uses metres and millimetres correctly");
        double[] depth=MeterMath.depth(50,8,3,.03);check(depth[0]<3&&depth[1]>3&&near(depth[0],2.34037267),"Near/far depth brackets the chosen distance");
        check(Double.isInfinite(MeterMath.depth(50,8,20,.03)[1]),"Far depth reaches infinity beyond hyperfocal distance");
        check(near(MeterMath.flashFraction(32,8,2,100,0),.25),"GN32 at f8 and two metres suggests quarter power");
        check(near(MeterMath.flashFraction(32,8,2,400,2),.25),"Flash ISO gain and two-stop ND cancel exactly once");
        rejects(()->MeterMath.ev100(0,.01,100),"Missing native aperture cannot fabricate a meter reading");
        rejects(()->MeterMath.seconds(Double.NaN,8,100,0,0),"Missing EV does not produce a physical-camera suggestion");
        rejects(()->MeterMath.ei(400,7),"Invalid push/pull is rejected");
        rejects(()->MeterMath.flashFraction(0,8,2,100,0),"Missing GN cannot suggest direct-flash output");
        for(int angle:new int[]{0,90,180,270}){
            int w=angle%180==0?360:700,h=angle%180==0?700:360;
            for(int id=0;id<FrameFormat.LABELS.length;id++){
                int[] fit=FrameFormat.fit(id,w,h,angle);FrameFormat.Snapshot shot=new FrameFormat.Snapshot(id,fit[0],fit[1],angle);
                check(fit[0]<=w&&fit[1]<=h&&Math.abs(fit[0]/(double)fit[1]-shot.outputAspect(angle))<.005,"Frame "+id+" keeps finder/JPEG aspect at orientation "+angle);
            }
        }
        check(FrameFormat.safe(99)==0&&FrameFormat.LABELS.length==9,"New formats append without invalidating the five legacy IDs");
        int[] wb={1,5};float[] f={1.75f};PhotoCapabilities caps=new PhotoCapabilities(true,100,1600,100000,1000000000L,wb,f,"native");wb[0]=99;f[0]=9;
        PhotoControls p=caps.clamp(new PhotoControls(true,6400,9000000000L,5,1.75f));
        check(p.iso==1600&&p.exposureNs==1000000000L&&p.awb==5&&p.aperture==1.75f,"Phone parameters clamp to the selected route's real capabilities");
        check(caps.whites[0]==1&&caps.apertures[0]==1.75f,"Capability arrays are independent of caller mutation");
        check(!PhotoCapabilities.none().clamp(p).manual,"Unsupported route cannot retain a fake manual mode");
        check(p.matches(1600,1000000000L)&&!p.matches(800,1000000000L)&&!p.matches(1600,500000000L),"Manual still validation rejects a different ISO or shutter");
        NativeReading r=new NativeReading(123,1000,9,400,8000000,8,85,85,1,2,0,2,2,"4",true,100);
        check(r.ready(1500,9)&&!r.ready(2001,9)&&!r.ready(1500,10),"Meter accepts fresh validated results from the current native-control epoch only");
        check(near(r.ev100,ev)&&near(r.estimatedFocusMetres(),.5),"Meter uses unfiltered exposure metadata and calibrated distance");
        r=new NativeReading(123,1000,9,400,8000000,8,85,85,1,1,0,2,1,"4",true,100);
        check(!r.ready(1500,9)&&Double.isNaN(r.estimatedFocusMetres()),"Searching AE and approximate distance cannot claim stable/calibrated results");
        Film.Settings film=new Film.Settings();film.stock=28;film.temperature=.4f;CaptureQueue queue=new CaptureQueue();
        CaptureQueue.Ticket ticket=queue.reserve(1,film,new FrameFormat.Snapshot(8,360,700),"roll-a","{\"name\":\"卷 A\"}");film.stock=1;film.temperature=-.8f;
        Film.Settings frozen=ticket.settings();frozen.stock=3;
        check(ticket.rollId.equals("roll-a")&&ticket.settings().stock==28&&ticket.settings().temperature==.4f&&ticket.rollSnapshot.contains("卷 A"),"Shutter freezes roll, film and temperature before background development");
        PrintOptions all=new PrintOptions(1,false,true,true,true,true,true,true,true);
        JSONObject meta=new JSONObject().put("stock",28).put("captured_at",1700000000000L).put("lens","85mm").put("capture",new JSONObject().put("make","vivo").put("model","V2366GA").put("iso",400).put("aperture",1.75).put("focal_mm",22.48).put("exposure_ns",8000000)).put("annotations",new JSONObject().put("camera","Leica").put("iso",100));
        PrintMetadata labels=new PrintMetadata("20261025_150000_000",meta);String[] captions=labels.captions(all);
        check(labels.camera.equals("vivo V2366GA")&&labels.exposure.contains("400")&&!labels.camera.contains("Leica"),"Native phone metadata wins over external-camera annotations in print labels");
        check(captions.length==5&&all.dark,"All five information rows and black borders are independently represented");
        PrintLayout layout=PrintLayout.single(3072,4096,1,captions.length,4096);
        check(layout.textY+4*layout.lineHeight<layout.height&&near(layout.photoWidth/layout.photoHeight,.75),"Five-row caption block fits below a complete portrait photograph");
        check(new PrintMetadata("20261025_150000_000",new JSONObject().put("imported",true)).date.isEmpty(),"Import date is never presented as a missing capture date");
        check(ArchiveSchema.csv("a,\"b\"\n中").equals("\"a,\"\"b\"\"\n中\"")&&ArchiveSchema.csv("=1+1").startsWith("\"'="),"UTF8 CSV quotes commas/newlines and neutralizes spreadsheet formulas");
        check(ArchiveSchema.photoTime("20261023_120000_000",0)<ArchiveSchema.photoTime("20261025_120000_000",0)&&ArchiveSchema.photoTime("invalid",37)==37,"Legacy archive ordering follows stable photo dates rather than migration processing time");
        MeterReferences references=new MeterReferences();references.offer("first");references.begin(1);references.offer("second");references.offer("third");
        check(references.complete(1).equals("first"),"Later meter records cannot steal an already-submitted reference image");references.begin(2);check(references.complete(2).equals("second"),"Queued reference requests preserve record order");references.begin(3);check(references.cancel("third")&&references.complete(3)==null,"Timed-out reference callback cannot attach an expired image");references.offer("fourth");references.begin(4);references.clear();check(references.complete(4)==null&&references.empty(),"Leaving metering invalidates old reference readbacks");
        System.out.println("ALL "+passed+" STUDIO DOMAIN CHECKS PASSED");
    }
}
