package com.silverroom.camera;

/** Camera-thread state. A request is not reported as locked until AF and AE results agree. */
public final class MeteringLock {
    public static final int IDLE=0,FOCUSING=1,WAIT_AE=2,LOCKED=3,FAILED=-1;
    private int state;private long epoch;
    public int state(){return state;}public long epoch(){return epoch;}
    public boolean active(){return state!=IDLE;}public boolean locked(){return state==LOCKED;}public boolean ae(){return state>=WAIT_AE;}
    public long begin(){epoch++;state=FOCUSING;return epoch;}
    public void clear(){epoch++;state=IDLE;}
    public int result(long token,Integer af,Integer ae,Boolean aeLock){
        if(token!=epoch||!active())return state;
        if(state==LOCKED&&((af!=null&&af!=4)||(ae!=null&&ae!=3)||Boolean.FALSE.equals(aeLock))){clear();return FAILED;}
        if(af!=null&&af==5){clear();return FAILED;}
        if(state==FOCUSING&&af!=null&&af==4&&ae!=null&&(ae==2||ae==4))state=WAIT_AE;
        else if(state==WAIT_AE&&af!=null&&af==4&&ae!=null&&ae==3&&Boolean.TRUE.equals(aeLock))state=LOCKED;
        return state;
    }
    public boolean timeout(long token){if(token!=epoch||!active()||locked())return false;clear();return true;}
}
