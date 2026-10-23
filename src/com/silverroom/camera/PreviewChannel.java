package com.silverroom.camera;

/** Old dialogs may only detach their own camera subscription. */
public final class PreviewChannel<T> {
    private volatile T value;
    public T get(){return value;}
    public synchronized void set(T next){value=next;}
    public synchronized boolean remove(T expected){if(value!=expected)return false;value=null;return true;}
}
