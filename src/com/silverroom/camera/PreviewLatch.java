package com.silverroom.camera;

/** Keep a consumed image long enough to pair and present it before taking another one. */
final class PreviewLatch {
    static final long WAIT_MS=80;
    private long epoch=-1,timestamp,displayed,at;
    synchronized boolean drain(boolean logical,boolean capture,long currentEpoch,long now,boolean proved,boolean rejected){
        if(!logical||capture||epoch!=currentEpoch||timestamp<=0||displayed==timestamp||rejected)return true;
        return !proved&&now-at>=WAIT_MS;
    }
    synchronized void consumed(long currentEpoch,long value,long now){epoch=currentEpoch;timestamp=value;at=now;}
    synchronized void displayed(long currentEpoch,long value){if(epoch==currentEpoch&&timestamp==value)displayed=value;}
    synchronized void clear(){epoch=-1;timestamp=displayed=at=0;}
}
