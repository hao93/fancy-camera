package com.silverroom.camera;

/** Album geometry; visual pill insets never reduce the full touch target. */
public final class ArchiveLayout {
    private ArchiveLayout(){}
    public static int heading(float font){return Math.max(48,(int)Math.ceil(28*Math.max(1,font)+16));}
    public static int tool(float font){return Math.max(48,(int)Math.ceil(20*Math.max(1,font)+16));}
    public static int columns(int width,float font){return Math.max(1,Math.min(3,width/(int)Math.ceil(104*Math.max(1,font))));}
}
