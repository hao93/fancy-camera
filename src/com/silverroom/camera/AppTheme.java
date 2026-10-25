package com.silverroom.camera;

/** UI-only colors. Camera/film/export code never reads this palette. */
public final class AppTheme {
    public static final String PREF="appearance";
    public static final int BLACK=0,WHITE=1;
    public static final int BG=0,SURFACE=1,TEXT=2,SECONDARY=3,ACCENT=4,TEAL=5,PINK=6,BORDER=7,ACTIVE=8,PRESSED=9,SELECT_BORDER=10,DANGER=11,DANGER_BG=12,IMAGE_BG=13,DIAL_HI=14,DIAL_MID=15,DIAL_LOW=16,DIAL_BORDER=17,DIAL_INNER=18,TICK=19,SHUTTER=20,SHUTTER_PRESSED=21,SHUTTER_BUSY=22,RED=23,GREEN=24,BLUE=25;
    private static final int[][] COLORS={
        {0xff09090b,0xff1c1c20,0xfff4f4f5,0xffa1a1aa,0xffff626a,0xffe7e7eb,0xffff8d99,0xff44444c,0xff302024,0xff35353d,0xffe66770,0xffff8f96,0xff4b242c,0xff08080a,0xff4a4a51,0xff242429,0xff101013,0xff696971,0xff111114,0xff8a8a94,0xfffafafb,0xffd4d4d8,0xff96969f,0xffef9b9f,0xffa8c9b5,0xffadc5ed},
        {0xfff4f4f5,0xffffffff,0xff202024,0xff61616b,0xffb92c3a,0xff44444e,0xffa9324b,0xffcacad0,0xfff9e9ed,0xffe4e4e9,0xffb92d3d,0xffae2435,0xffffe8ec,0xffeaeaee,0xfffafafd,0xffe5e5eb,0xffc4c4cc,0xff8d8d99,0xffd3d3db,0xff73737f,0xff242429,0xff51515a,0xff868692,0xffb72c3a,0xff387a50,0xff386dad}
    };
    private static int mode=BLACK;
    private AppTheme(){}
    public static int safe(int value){return value==WHITE?WHITE:BLACK;}
    public static int current(){return mode;}
    public static boolean light(){return mode==WHITE;}
    public static void select(int value){mode=safe(value);}
    public static int color(int role){return color(mode,role);}
    public static int color(int theme,int role){return COLORS[safe(theme)][role];}
    public static int remap(int color,int from,int to){for(int i=0;i<COLORS[0].length;i++)if(COLORS[safe(from)][i]==color)return COLORS[safe(to)][i];return color;}
    public static final int OVERLAY_TEXT=0xfffaf8f1,OVERLAY_ACCENT=0xffff636b,OVERLAY_BG=0xbb151519;
}
