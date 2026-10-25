package com.silverroom.camera;
import java.util.*;

/** Freezes each optional reference's record ID when a preview readback is submitted. */
public final class MeterReferences {
    private final ArrayDeque<String> waiting=new ArrayDeque<>();
    private final Map<Long,String> submitted=new HashMap<>();
    public synchronized void offer(String id){if(id!=null&&!waiting.contains(id)&&!submitted.containsValue(id))waiting.addLast(id);}
    public synchronized boolean waiting(){return !waiting.isEmpty();}
    public synchronized void begin(long token){String id=waiting.pollFirst();if(id!=null)submitted.put(token,id);}
    public synchronized String complete(long token){return submitted.remove(token);}
    public synchronized boolean cancel(String id){boolean removed=waiting.remove(id);Iterator<String> i=submitted.values().iterator();while(i.hasNext())if(id.equals(i.next())){i.remove();removed=true;}return removed;}
    public synchronized boolean empty(){return waiting.isEmpty()&&submitted.isEmpty();}
    public synchronized void clear(){waiting.clear();submitted.clear();}
}
