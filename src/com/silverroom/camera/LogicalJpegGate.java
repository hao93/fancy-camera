package com.silverroom.camera;

/** A JPEG cannot enter the save pipeline before its own verified result arrives. */
final class LogicalJpegGate {
    private byte[] bytes; private long imageTimestamp,resultTimestamp; private boolean invalid,delivered;
    void image(long timestamp,byte[] data){if(delivered||invalid)return;if(timestamp<=0||bytes!=null){invalid=true;return;}imageTimestamp=timestamp;bytes=data;}
    void result(long timestamp,boolean verified){if(delivered||invalid)return;if(!verified||timestamp<=0||resultTimestamp!=0){invalid=true;return;}resultTimestamp=timestamp;}
    boolean invalid(){return invalid||(imageTimestamp>0&&resultTimestamp>0&&imageTimestamp!=resultTimestamp);}
    byte[] take(){if(invalid()||delivered||bytes==null||imageTimestamp!=resultTimestamp)return null;delivered=true;byte[] ready=bytes;bytes=null;return ready;}
    void cancel(){invalid=true;bytes=null;}
}
