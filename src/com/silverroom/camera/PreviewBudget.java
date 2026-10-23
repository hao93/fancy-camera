package com.silverroom.camera;

/** At most one raw read / CPU batch / UI commit, with no queued stale frames. */
public final class PreviewBudget {
    private boolean busy,closed;private long previous=Long.MIN_VALUE;
    public synchronized boolean acquire(long now){if(closed||busy||(previous!=Long.MIN_VALUE&&now-previous<250))return false;busy=true;previous=now;return true;}
    public synchronized void complete(){busy=false;}
    public synchronized void close(){closed=true;}
    public synchronized boolean closed(){return closed;}
}
