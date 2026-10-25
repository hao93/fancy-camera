package com.silverroom.camera;

/** Stable optional-tool identities; settings can never be hidden. */
public final class ToolbarConfig {
    // Append only: the five existing preference keys keep their meaning on upgrade.
    public static final String[] IDS={"flash","timer","frame","metering","histogram","exposure","mode","roll"};
    public static final String[] LABELS={"闪光灯","定时拍摄","画幅","测光方式","直方图","EV 曝光","AUTO / PRO","当前胶卷（手机随拍）"};
    public static final int[] ORDER={0,1,2,3,5,6,7,4};
    public static String key(int index){if(index<0||index>=IDS.length)throw new IllegalArgumentException("Unknown toolbar tool");return "toolbar_show_"+IDS[index];}
    public static int visible(boolean[] values){int n=0;for(int i=0;i<IDS.length;i++)if(values==null||i>=values.length||values[i])n++;return n;}
}
