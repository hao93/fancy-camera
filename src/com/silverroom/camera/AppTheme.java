package com.silverroom.camera;

/** UI-only colors. Camera/film/export code never reads this palette. */
public final class AppTheme {
    public static final String PREF="appearance";
    public static final int BLACK=0,WHITE=1;
    public static final int BG=0,SURFACE=1,TEXT=2,SECONDARY=3,ACCENT=4,TEAL=5,PINK=6,BORDER=7,ACTIVE=8,PRESSED=9,SELECT_BORDER=10,DANGER=11,DANGER_BG=12,IMAGE_BG=13,DIAL_HI=14,DIAL_MID=15,DIAL_LOW=16,DIAL_BORDER=17,DIAL_INNER=18,TICK=19,SHUTTER=20,SHUTTER_PRESSED=21,SHUTTER_BUSY=22,RED=23,GREEN=24,BLUE=25;
    private static final int[][] COLORS={
        {0xff121514,0xff1c201e,0xffebe2cb,0xff949b8e,0xffe8ab4f,0xff5fb8ae,0xffd36f97,0xff3c433c,0xff24281f,0xff393e36,0xff86704a,0xffef9a89,0xff522a28,0xff0b0e0c,0xff52564e,0xff222621,0xff10130f,0xff5b6053,0xff0c0e0c,0xff777c6e,0xfff2f0e9,0xffd1d1c8,0xffa5a69d,0xffe28c88,0xff92c4a3,0xff94b5e3},
        {0xfff7f3eb,0xffeee8de,0xff242b27,0xff5c655c,0xff80520a,0xff14756d,0xffa43b67,0xffc9c4b8,0xffefe2c8,0xffdce1d6,0xffb58d44,0xffb13e32,0xfff8ddd7,0xffe9e6e0,0xfffefefb,0xffe0e5da,0xffb9c2b4,0xff9da996,0xffc1cabc,0xff7d8974,0xff28312a,0xff4b574c,0xff7e8c7c,0xffbd4945,0xff388353,0xff3b70b4}
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
    public static final int OVERLAY_TEXT=0xfffaf8f1,OVERLAY_ACCENT=0xffffd47b,OVERLAY_BG=0xbb151b16;
}
