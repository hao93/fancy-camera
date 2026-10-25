package com.silverroom.camera;

/** X100 Ultra lens routing. A missing optical base must never become a main-camera crop. */
public final class LensPlan {
    private LensPlan() {}
    public enum State { MISSING, CANDIDATE, VERIFIED, REJECTED }
    public static State state(java.util.List<Route> routes,int base,java.util.Set<String> failed,java.util.Set<String> verified){
        boolean found=false,candidate=false;
        for(Route r:routes){if(r.front||r.base!=base)continue;found=true;if(failed.contains(r.key()))continue;
            if(verified.contains(r.key()))return State.VERIFIED;candidate=true;
        }
        return candidate?State.CANDIDATE:found?State.REJECTED:State.MISSING;
    }
    /** Bounded metadata queries; ID numbers never determine a lens's optical role. */
    public static java.util.List<String> extraCandidates(java.util.Set<String> known){
        java.util.List<String> ids=new java.util.ArrayList<>();
        for(int i=0;i<32;i++){String id=Integer.toString(i);if(!known.contains(id))ids.add(id);}
        return ids;
    }
    public static final class Route {
        public final String owner,physical;public final int base;public final float equivalent;public final boolean front,logicalDefault,logicalZoom;
        public Route(String o,String p,int b,float e,boolean f,boolean logical){this(o,p,b,e,f,logical,false);}
        public Route(String o,String p,int b,float e,boolean f,boolean logical,boolean zoom){owner=o;physical=p;base=b;equivalent=e;front=f;logicalDefault=logical;logicalZoom=zoom;}
        public String key(){return (logicalZoom?"zoom/":"")+owner+"/"+physical;}
        public String source(){return logicalZoom?"逻辑变焦 ID "+owner:physical!=null?"绑定物理 ID "+physical+" / 逻辑 ID "+owner:logicalDefault?"系统主摄 1× / ID "+owner:"独立相机 ID "+owner;}
        public int priority(){return logicalDefault?2:physical==null?0:1;}
    }
    public static Route select(java.util.List<Route> routes,float requested,boolean front,java.util.Set<String> failed){
        int base=front?0:base(requested);Route best=null;
        for(Route r:routes){if(r.front!=front||r.base!=base||failed.contains(r.key()))continue;
            if(best==null||r.priority()<best.priority()||(r.priority()==best.priority()&&Math.abs(r.equivalent-base)<Math.abs(best.equivalent-base)))best=r;
        }return best;
    }
    public static int base(float focal) {
        if(!Float.isFinite(focal)||focal<14||focal>230)throw new IllegalArgumentException("Unsupported focal length: "+focal);
        return focal<23?14:focal<85?23:85;
    }
    public static float crop(float focal){return focal/base(focal);}
    public static int identify(float equivalent){
        if(!Float.isFinite(equivalent)||equivalent<=0)return 0;
        int best=0;double error=Double.MAX_VALUE;
        for(int nominal:new int[]{14,23,85}){double e=Math.abs(equivalent-nominal)/nominal;if(e<=.25&&e<error){best=nominal;error=e;}}
        return best;
    }
    public static String name(int base){return base==14?"超广角":base==85?"潜望长焦":"主摄";}
    public static int[] cropSize(int width,int height,float factor){
        if(width<=0||height<=0||!Float.isFinite(factor)||factor<1)throw new IllegalArgumentException("Invalid crop");
        return new int[]{Math.max(1,Math.round(width/factor)),Math.max(1,Math.round(height/factor))};
    }
}
