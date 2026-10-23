package com.silverroom.camera;
import java.util.*;

/** A viewer owns an immutable album order and a revision for asynchronous image loads. */
public final class PhotoPager {
    private final List<String> ids=new ArrayList<>();private int index;private long revision=1;private boolean closed;
    public PhotoPager(Collection<String> source,String selected){for(String id:source)if(NegativeStore.valid(id)&&!ids.contains(id))ids.add(id);index=ids.indexOf(selected);if(index<0)throw new IllegalArgumentException("Selected negative is absent");}
    public synchronized String current(){return ids.get(index);}
    public synchronized int position(){return index+1;}
    public int count(){return ids.size();}
    public synchronized boolean canMove(int delta){return !closed&&(delta==-1||delta==1)&&index+delta>=0&&index+delta<ids.size();}
    public synchronized boolean move(int delta){if(!canMove(delta))return false;index+=delta;revision++;return true;}
    public synchronized long revision(){return revision;}
    public synchronized boolean matches(String id,long token){return !closed&&revision==token&&ids.get(index).equals(id);}
    public synchronized void close(){closed=true;revision++;}
}
