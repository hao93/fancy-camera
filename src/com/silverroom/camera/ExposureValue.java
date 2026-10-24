package com.silverroom.camera;

/** Display-level formatting only; ExposureScale maps these levels to physical EV. */
public final class ExposureValue {
    private ExposureValue(){}
    public static float clamp(float ev){return Float.isFinite(ev)?Math.round(Math.max(-3,Math.min(3,ev))*10)/10f:0;}
    public static int progress(float ev){return Math.round((clamp(ev)+3)*10);}
    public static float value(int progress){return (Math.max(0,Math.min(60,progress))-30)/10f;}
    public static String label(float ev){return String.format(java.util.Locale.US,"%+.1f",clamp(ev));}
}
