package com.silverroom.camera;

/** Serializes asynchronous camera opens and closes; used only on the camera handler. */
public final class CameraHandoff {
    private boolean opening,wanted;
    private final java.util.Set<Object> closing=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>());
    public void request(){wanted=true;}
    public void cancel(){wanted=false;}
    public boolean begin(){if(!wanted||opening||!closing.isEmpty())return false;wanted=false;opening=true;return true;}
    public void opened(){opening=false;}
    public void retiring(Object device){closing.add(device);}
    public void closed(Object device){closing.remove(device);}
}
