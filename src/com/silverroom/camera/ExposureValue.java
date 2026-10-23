package com.silverroom.camera;

public final class ExposureValue {
    private ExposureValue(){}
    public static float clamp(float ev){return Float.isFinite(ev)?Math.round(Math.max(-2,Math.min(2,ev))*10)/10f:0;}
    public static int progress(float ev){return Math.round((clamp(ev)+2)*10);}
    public static float value(int progress){return (Math.max(0,Math.min(40,progress))-20)/10f;}
    public static String label(float ev){return String.format(java.util.Locale.US,"%+.1f",clamp(ev));}
}
