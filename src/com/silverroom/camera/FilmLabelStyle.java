package com.silverroom.camera;

/** UI packaging palettes only. Never read by Film, the shaders, or photo export. */
public final class FilmLabelStyle {
    // Match stable FilmCatalog IDs; do not reorder to group visually similar wrappers.
    private static final int[][] COLORS={
        {0xffeee9db,0xff292b29,0xffa99970},{0xffedbd48,0xff683425,0xfffcdf78},
        {0xff184647,0xffd9793e,0xfff1e0bb},{0xffe6b9be,0xff7d304c,0xfff8e3ce},
        {0xffedece6,0xff1c2421,0xff568462},{0xff222d54,0xffa75470,0xffaebcdf},
        {0xffa3ad9b,0xff4f5547,0xffddd3ae},{0xffbf8c6c,0xff324b3c,0xffe0c2a0},
        {0xffd7b475,0xff653c2b,0xfff1d8a4},{0xffe3e9df,0xff23624c,0xffb6c6a9},
        {0xfff1eee4,0xff213e84,0xffd45344},{0xff592f69,0xff1b4d3f,0xffd7b452},
        {0xffeed7d9,0xff6d455c,0xffc9977f},{0xffa8b7a3,0xff3a504b,0xffe6d6ac},
        {0xffbdc2ba,0xff333d4a,0xffdfd8ca},{0xffeeeeea,0xff19191c,0xff9e6688},
        {0xffbcbeb9,0xff4b515b,0xffe7d1a1},{0xffb99a66,0xff4e3628,0xffe6d2ac},
        {0xffddd9cf,0xff3c3b38,0xffa8303d},{0xffa72b36,0xff222326,0xfff4e8df},
        {0xfff5f3ed,0xff222225,0xff9f303d},{0xff214a59,0xffcaad66,0xffefe6c9},
        {0xffd5c5a6,0xff465c48,0xffa56d4a},{0xff242523,0xffdeddd2,0xffbd7244},
        {0xffffcf20,0xff9d352c,0xffe69a35},{0xff741f27,0xffffcb10,0xffbd3431},
        {0xff7b2c31,0xffe9ba3a,0xfff2d880},{0xff172c49,0xffc34d43,0xffd4e0df},
        {0xfff4f4eb,0xff111a15,0xff078a38},{0xffede6d1,0xff303735,0xffd5784a},
        {0xff203c2e,0xff9b3434,0xffa5a346},{0xffdfb5b9,0xffa8c2b9,0xffeacb76},
        {0xff708398,0xff242e3d,0xffc0c5c6},{0xff416c65,0xffb9813c,0xffead9ae},
        {0xffc3cfcc,0xff35474d,0xffb08f7d},{0xffb4aca9,0xff777b74,0xffd5caba},
        {0xffc5d3de,0xff374a60,0xff91a8b5},{0xffe4e9e3,0xff8daba1,0xffc2c7bf},
        {0xff273c33,0xff9a8054,0xff667d69},{0xfff0e8dd,0xffc5afa0,0xffe3d4be},
        {0xff232526,0xff6b3b40,0xff7d8686},{0xffb9814a,0xff68412d,0xffe4c585},
        {0xff663f65,0xff273747,0xffd0a94f},{0xffe0e7d6,0xff52786d,0xffbdcdbd},
        {0xffb9b1d9,0xff523c88,0xffd987b1},{0xff54646b,0xff80b6bf,0xffbb8dac},
        {0xffedf0ed,0xff2e6396,0xffc4dce4},{0xffd6c4aa,0xff754b48,0xfff0e7d7},
        {0xffc7b56e,0xff343a37,0xffece1bc},{0xffd8753f,0xff933630,0xfff1c778},
        {0xffa8bda5,0xff527783,0xffe7dfc6}
    };
    public final int paper,band,accent,ink,bandInk,pattern,id;
    private FilmLabelStyle(int index){id=index;int[] c=COLORS[index];paper=c[0];band=c[1];accent=c[2];ink=textColor(paper);bandInk=textColor(band);pattern=index%5;}
    public static FilmLabelStyle of(int id){return new FilmLabelStyle(FilmCatalog.safe(id));}
    private static double linear(int v){double x=v/255d;return x<=.04045?x/12.92:Math.pow((x+.055)/1.055,2.4);}
    public static int textColor(int color){double y=.2126*linear(color>>16&255)+.7152*linear(color>>8&255)+.0722*linear(color&255);return (y+.05)/.05>=1.05/(y+.05)?0xff000000:0xffffffff;}
    public static int count(){return COLORS.length;}
    public int iso(){switch(id){case 0:case 25:return 100;case 3:case 24:return 200;case 1:case 4:case 28:return 400;case 2:return 500;case 5:case 27:return 800;default:return 0;}}
    public String mark(){switch(id){case 24:return "GOLD 200";case 25:return "EKTAR 100";case 26:return "KODACHROME";case 27:return "800T";case 28:return "HP5 PLUS";case 29:return "INSTANT";default:return FilmCatalog.reelName(id);}}
    public String type(){return id==4||id==15||id==17||id==20||id==23||id==28?"MONOCHROME":id==10||id==11||id==21||id==26?"REVERSAL":id==2||id==5||id==13||id==14||id==27||id>=30&&id<=34?"CINEMA":"COLOR PROFILE";}
}
