package com.silverroom.camera;

import java.util.*;

/** Bounded shutter reservations. Accepted photos survive UI cancellation until their worker finishes. */
final class CaptureQueue {
    static final int LIMIT=4;
    static final class Ticket {
        final long token;final FrameFormat.Snapshot frame;private final Film.Settings film;private boolean accepted;
        Ticket(long id,Film.Settings settings,FrameFormat.Snapshot format){token=id;film=settings.copy();frame=format;}
        Film.Settings settings(){return film.copy();}
    }
    private final Map<Long,Ticket> pending=new HashMap<>();
    synchronized Ticket reserve(long token,Film.Settings film,FrameFormat.Snapshot frame){
        if(pending.size()>=LIMIT||pending.containsKey(token))return null;
        Ticket ticket=new Ticket(token,film,frame);pending.put(token,ticket);return ticket;
    }
    synchronized Ticket accept(long token){Ticket t=pending.get(token);if(t==null||t.accepted)return null;t.accepted=true;return t;}
    synchronized void cancel(long token){Ticket t=pending.get(token);if(t!=null&&!t.accepted)pending.remove(token);}
    synchronized void finish(long token){Ticket t=pending.get(token);if(t!=null&&t.accepted)pending.remove(token);}
    synchronized int count(){return pending.size();}
}
