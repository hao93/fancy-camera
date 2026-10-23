package com.silverroom.camera;

/** One frame in flight. Pause/resume invalidates old results and their completions. */
public final class PreviewFlow {
    private boolean enabled,closed;private long sequence,active,previous=Long.MIN_VALUE;
    public synchronized void enable(boolean on){if(closed)return;enabled=on;active=0;previous=Long.MIN_VALUE;}
    public synchronized long acquire(long now){if(closed||!enabled||active!=0||(previous!=Long.MIN_VALUE&&now-previous<100))return 0;previous=now;return active=++sequence;}
    public synchronized boolean valid(long token){return token!=0&&token==active&&enabled&&!closed;}
    public synchronized void complete(long token){if(token==active)active=0;}
    public synchronized void close(){closed=true;enabled=false;active=0;}
    public synchronized boolean closed(){return closed;}
    public static boolean current(long captured,long now){return now>=captured&&now-captured<=450;}
}
