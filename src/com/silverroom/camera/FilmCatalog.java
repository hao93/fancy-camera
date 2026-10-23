package com.silverroom.camera;

/** Original color recipes inspired by film characteristics, not proprietary LUTs. */
public final class FilmCatalog {
    private FilmCatalog(){}
    // Append only: existing negatives store numeric recipe IDs. Never reorder 0..23.
    public static final String[] NAMES={"原生负片","暖金 400","青橙电影","樱桃 200","银盐黑白","午夜 800","经典铬彩","经典负片","怀旧负片","自然真实","标准正片","鲜艳反转","柔和人像","影院柔彩","漂白电影","细腻黑白","柔和负片","暖调棕片","徕卡自然","徕卡鲜明","徕卡黑白","理光正片","理光负片","理光硬黑",
        "Kodak Gold 200","Kodak Ektar 100","Kodak Kodachrome","CineStill 800T","Ilford HP5","Polaroid 宝丽来",
        "王家卫风格","韦斯·安德森风格","诺兰电影风","港风电影","韩国电影风",
        "莫兰迪色系","德系冷调","北欧极简","暗调 Moody","高调 High Key","低调 Low Key",
        "70 年代美式","80 年代港风","90 年代日系","Y2K 千禧风","CCD 数码复古",
        "直闪 Flash","杂志 Editorial","街头纪实","胶片漏光","移轴微缩"};
    public static final String[] TAGS={"NEUTRAL / 100","GOLD / 400","CINEMA / 500T","CHERRY / 200","MONO / 400","NIGHT / 800","CLASSIC CHROME","CLASSIC Neg.","NOSTALGIC Neg.","REALA ACE","PROVIA","Velvia","ASTIA","ETERNA","BLEACH BYPASS","ACROS","PRO Neg. Std","SEPIA","L / NATURAL","L / VIVID","L / MONO","GR / POSITIVE","GR / NEGATIVE","GR / HARD MONO",
        "KODAK / GOLD 200","KODAK / EKTAR 100","KODAK / KODACHROME","CINESTILL / 800T","ILFORD / HP5 PLUS","POLAROID / INSTANT",
        "WONG KAR WAI","WES ANDERSON","CHRISTOPHER NOLAN","HONG KONG CINEMA","KOREAN CINEMA",
        "MORANDI","GERMAN COOL","NORDIC MINIMAL","MOODY","HIGH KEY","LOW KEY",
        "AMERICAN / 1970s","HONG KONG / 1980s","JAPAN / 1990s","Y2K / 2000s","CCD / DIGITAL",
        "DIRECT FLASH","EDITORIAL","STREET DOCUMENTARY","LIGHT LEAK","TILT SHIFT"};
    public static final String[] DESCRIPTIONS={"保留自然光色","暖色日常 · 日落","冷暖对比 · 街景","轻粉肤色 · 春日","硬朗明暗 · 街拍","冷调夜色 · 霓虹","低饱和 · 纪实街拍","浓郁暗部 · 日系街头","琥珀高光 · 怀旧日常","自然肤色 · 全天候","平衡色彩 · 旅行日常","鲜艳蓝绿 · 晴天风光","柔和反差 · 人像花卉","低反差 · 柔彩电影","低饱和 · 硬调电影","细腻灰阶 · 黑白纪实","柔软层次 · 室内人像","暖棕单色 · 旧时光","徕卡灵感 · 自然暖色与柔亮高光","徕卡灵感 · 浓郁色彩与扎实暗部","徕卡灵感 · 清晰灰阶与深邃黑位","理光灵感 · 浓郁正片与冷绿暗部","理光灵感 · 柔暖负片与青色阴影","理光灵感 · 街头高反差黑白",
        "经典胶片 · 暖金日光与柔和颗粒","经典胶片 · 高饱和风光与细腻质感","经典胶片 · 浓郁红黄与深蓝反转色","经典胶片 · 冷蓝夜色与暖色霓虹","经典胶片 · 中等反差黑白与纪实颗粒","经典胶片 · 褪色柔彩与即时成像感",
        "电影美学 · 绿影红光与浓郁夜色","电影美学 · 奶油粉彩与复古暖色","电影美学 · 冷钢蓝与深邃明暗","电影美学 · 青绿阴影与琥珀灯色","电影美学 · 克制冷影与柔暖肤色",
        "现代调色 · 灰调低饱和与柔雾层次","现代调色 · 冷白蓝灰与利落反差","现代调色 · 明亮留白与清淡色彩","现代调色 · 深绿暗部与情绪低饱和","现代调色 · 明亮柔和与轻盈高光","现代调色 · 压暗层次与集中的亮部",
        "复古年代 · 暖褐颗粒与褪色阳光","复古年代 · 品红霓虹与青绿黑位","复古年代 · 清透青影与柔白日常","复古年代 · 冰蓝粉紫与亮丽数码感","复古年代 · 冷调颗粒与阶梯色彩",
        "特色摄影 · 直闪调色，不自动开启闪光灯","特色摄影 · 克制肤色与杂志明暗","特色摄影 · 低彩街头与纪实反差","特色摄影 · 暖橙边缘漏光叠加","特色摄影 · 中央清晰、上下渐进虚化，适合俯拍"};
    public static final int[] COLORS={0xffafa991,0xffe6af50,0xff79b9b9,0xffcf84a1,0xffbbbbac,0xff6e83b1,0xff92a79a,0xffb88969,0xffd6ae77,0xff88b5ab,0xff96b9b8,0xff6eaa8b,0xffc7afa5,0xffa3afa0,0xff8c9c99,0xffb5b6af,0xffbcaea8,0xffb89667,0xffc4a18a,0xffc37f6b,0xffc3c3bc,0xff91aa79,0xffc7a07b,0xff9ca79e,
        0xffe4bb64,0xffce654f,0xffba773d,0xff6fabb9,0xffb7b8af,0xffc2b6a2,
        0xff858d42,0xffd2ae92,0xff8298aa,0xffb09b59,0xff99b0ac,
        0xffaaa59a,0xff899eaf,0xffc4d1cc,0xff637b70,0xffddd4c5,0xff7d818b,
        0xffc49b60,0xffb48396,0xffa6c1b8,0xffa6a9d0,0xff9daebc,
        0xffe4d2bd,0xffbaa99b,0xff939c90,0xffdb8b4b,0xff99b889};
    public static boolean valid(int id){return id>=0&&id<NAMES.length;}
    public static int safe(int id){return valid(id)?id:1;}
    private static final String[] REEL_NEW={"Gold 200","Ektar 100","Kodachrome","800T","HP5","宝丽来","王家卫","韦斯·安德森","诺兰","港风电影","韩国电影","莫兰迪","德系冷调","北欧极简","Moody","High Key","Low Key","70s 美式","80s 港风","90s 日系","Y2K","CCD","直闪","杂志","街头纪实","胶片漏光","移轴微缩"};
    public static String reelName(int id){id=safe(id);return id<24?NAMES[id]:REEL_NEW[id-24];}
    public static float[] parameters(int stock,float cyan,float magenta,float yellow,float exposure,float grain,float fade){
        // 20: light leak, 21: tilt blend, 22: vignette, 23: quantization steps (0 = off).
        float[] p={1,1,1,0,0,0,1,1,fade,grain,0,0,0,0,0,0,0,1,1,1,0,0,.10f,0};
        switch(safe(stock)){
            case 1:set(p,1.06f,1.01f,.89f,.028f,.014f,.008f,.92f,1.08f);break;
            case 2:set(p,1.04f,.98f,.90f,-.012f,.035f,.065f,.82f,1.14f);break;
            case 3:set(p,1.06f,.94f,.99f,.038f,.018f,.035f,.8f,.98f);break;
            case 4:p[6]=0;p[7]=1.14f;break;
            case 5:set(p,.93f,1.01f,1.06f,.018f,.008f,.038f,.72f,1.17f);break;
            case 6:set(p,1.01f,1.025f,.98f,0,.006f,.012f,.73f,1.13f);split(p,-.018f,.007f,.024f,.025f,.007f,-.015f,.35f);break;
            case 7:set(p,1.04f,1.015f,.94f,.004f,-.012f,-.008f,.93f,1.24f);split(p,-.028f,.016f,.012f,.04f,.012f,-.025f,.42f);break;
            case 8:set(p,1.055f,1.015f,.94f,.014f,.007f,.007f,.86f,1.08f);split(p,-.014f,.012f,.017f,.065f,.027f,-.032f,.65f);break;
            case 9:set(p,1.015f,1.006f,.99f,0,0,.003f,.99f,1.10f);split(p,-.005f,.004f,.007f,.012f,.006f,0,.28f);break;
            case 10:set(p,1.015f,1.01f,1.005f,0,0,0,1.06f,1.06f);p[16]=.24f;break;
            case 11:set(p,1.02f,1.04f,1.045f,-.006f,-.003f,0,1.31f,1.17f);split(p,-.008f,.006f,.015f,.012f,.005f,-.008f,.4f);break;
            case 12:set(p,1.025f,1.005f,.99f,.012f,.012f,.012f,.91f,.96f);split(p,.005f,.003f,.012f,.026f,.006f,.002f,.8f);break;
            case 13:set(p,1.015f,1.015f,.99f,.018f,.02f,.027f,.70f,.91f);split(p,-.012f,.006f,.017f,.023f,.009f,-.012f,1.0f);break;
            case 14:set(p,1.005f,1.025f,1.025f,-.006f,0,.008f,.42f,1.28f);split(p,-.014f,.008f,.014f,.014f,.006f,-.004f,.45f);break;
            case 15:p[6]=0;p[7]=1.19f;p[16]=.55f;break;
            case 16:set(p,1.02f,1,.985f,.016f,.015f,.015f,.86f,.92f);split(p,0,.003f,.008f,.025f,.01f,-.003f,.85f);break;
            case 17:p[6]=0;p[7]=1.07f;p[16]=.55f;p[17]=1.12f;p[18]=.98f;p[19]=.82f;break;
            case 18:set(p,1.025f,1.005f,.985f,.003f,.003f,.005f,.94f,1.09f);split(p,-.009f,.002f,.008f,.025f,.012f,.001f,.72f);break;
            case 19:set(p,1.045f,1.005f,.98f,-.008f,-.01f,-.009f,1.16f,1.23f);split(p,-.018f,.002f,.013f,.024f,.004f,-.008f,.6f);break;
            case 20:p[6]=0;p[7]=1.27f;p[3]=p[4]=p[5]=-.008f;p[16]=.82f;break;
            case 21:set(p,1.035f,1.035f,.965f,-.009f,-.004f,-.008f,1.19f,1.20f);split(p,-.026f,.016f,.005f,.043f,.018f,-.017f,.52f);break;
            case 22:set(p,1.035f,1.018f,.97f,.022f,.016f,.011f,.83f,1.04f);split(p,-.022f,.014f,.02f,.048f,.019f,-.019f,.88f);break;
            case 23:p[6]=0;p[7]=1.48f;p[3]=p[4]=p[5]=-.024f;p[16]=.34f;break;
            case 24:set(p,1.055f,1.018f,.93f,.018f,.009f,.002f,1.04f,1.07f);split(p,-.012f,.008f,.018f,.045f,.018f,-.018f,.50f);texture(p,.03f,1.05f);break;
            case 25:set(p,1.028f,1.012f,1.018f,-.008f,-.007f,-.004f,1.30f,1.20f);split(p,-.014f,.004f,.020f,.021f,.007f,-.009f,.32f);texture(p,0,.35f);break;
            case 26:set(p,1.065f,1.015f,.95f,-.014f,-.009f,-.014f,1.20f,1.28f);split(p,-.026f,-.005f,.018f,.048f,.022f,-.014f,.46f);texture(p,.01f,.65f);break;
            case 27:set(p,.94f,1.015f,1.075f,.005f,.004f,.016f,.90f,1.16f);split(p,-.040f,.012f,.046f,.075f,.012f,-.033f,.72f);texture(p,.03f,1.50f);break;
            case 28:p[6]=0;p[7]=1.12f;p[16]=.65f;texture(p,.035f,1.45f);break;
            case 29:set(p,1.03f,1.018f,.94f,.040f,.036f,.024f,.68f,.91f);split(p,-.012f,.021f,.014f,.041f,.023f,-.008f,1.05f);texture(p,.22f,.70f);p[22]=.14f;break;
            case 30:set(p,1.04f,1.035f,.93f,-.026f,-.010f,-.030f,1.16f,1.28f);split(p,-.065f,.044f,.004f,.100f,-.016f,-.028f,.62f);texture(p,.04f,1.20f);p[22]=.18f;break;
            case 31:set(p,1.035f,1.013f,.94f,.048f,.032f,.027f,.74f,.92f);split(p,.026f,-.006f,.024f,.060f,.038f,-.008f,1.10f);texture(p,.10f,.45f);p[22]=.035f;break;
            case 32:set(p,.965f,1.005f,1.045f,-.025f,-.012f,-.006f,.72f,1.31f);split(p,-.035f,.002f,.043f,.012f,.009f,.014f,.46f);texture(p,.015f,.55f);p[22]=.16f;break;
            case 33:set(p,1.055f,1.03f,.92f,-.011f,-.002f,-.008f,1.02f,1.19f);split(p,-.038f,.033f,.012f,.067f,.029f,-.030f,.54f);texture(p,.06f,1.12f);break;
            case 34:set(p,1.008f,1.015f,1.005f,.010f,.014f,.016f,.73f,1.06f);split(p,-.026f,.011f,.028f,.035f,.015f,.006f,.95f);texture(p,.045f,.40f);p[22]=.07f;break;
            case 35:set(p,1.025f,1.005f,.975f,.035f,.033f,.037f,.48f,.90f);split(p,.013f,.009f,.021f,.033f,.026f,.018f,1.20f);texture(p,.14f,.30f);p[22]=.025f;break;
            case 36:set(p,.96f,1.008f,1.052f,-.008f,-.003f,.004f,.80f,1.19f);split(p,-.029f,.007f,.030f,-.007f,.004f,.025f,.65f);texture(p,.01f,.20f);p[22]=.065f;break;
            case 37:set(p,1.02f,1.035f,1.035f,.060f,.065f,.069f,.58f,.89f);split(p,-.013f,.011f,.018f,.018f,.021f,.021f,1.25f);texture(p,.055f,.10f);p[22]=0;break;
            case 38:set(p,.93f,.98f,.95f,-.045f,-.026f,-.024f,.57f,1.23f);split(p,-.039f,.022f,.024f,.025f,.012f,-.009f,.80f);texture(p,.05f,.65f);p[22]=.22f;break;
            case 39:set(p,1.105f,1.095f,1.075f,.075f,.072f,.066f,.82f,.84f);split(p,.017f,.018f,.021f,.021f,.021f,.016f,1.55f);texture(p,.035f,.10f);p[22]=0;break;
            case 40:set(p,.85f,.88f,.92f,-.060f,-.052f,-.045f,.74f,1.34f);split(p,-.018f,-.005f,.022f,.035f,.015f,.007f,.70f);texture(p,0,.35f);p[22]=.25f;break;
            case 41:set(p,1.075f,1.015f,.89f,.031f,.020f,.009f,.78f,1.04f);split(p,.016f,.010f,-.008f,.068f,.034f,-.028f,.82f);texture(p,.23f,1.35f);p[22]=.16f;break;
            case 42:set(p,1.035f,.98f,1.018f,-.006f,.006f,.006f,1.10f,1.23f);split(p,-.025f,.036f,.025f,.072f,-.008f,.039f,.62f);texture(p,.08f,1.10f);p[22]=.14f;break;
            case 43:set(p,1.008f,1.03f,1.025f,.035f,.042f,.043f,.75f,.94f);split(p,-.015f,.021f,.020f,.033f,.026f,.010f,1.10f);texture(p,.09f,.60f);p[22]=.045f;break;
            case 44:set(p,1.025f,.98f,1.08f,.025f,.024f,.044f,1.13f,1.14f);split(p,-.029f,.012f,.046f,.047f,.004f,.046f,.45f);texture(p,.025f,.40f);p[22]=.035f;break;
            case 45:set(p,.985f,1.015f,1.045f,.017f,.015f,.027f,.88f,1.19f);split(p,-.018f,.006f,.027f,.027f,.006f,.010f,.25f);texture(p,.04f,1.70f);p[23]=32;break;
            case 46:set(p,1.065f,1.055f,1.04f,.032f,.026f,.019f,.91f,1.25f);split(p,.012f,.009f,.007f,.041f,.034f,.020f,.35f);texture(p,0,.30f);p[22]=.015f;break;
            case 47:set(p,1.025f,1.002f,.98f,-.004f,-.003f,-.001f,.79f,1.17f);split(p,-.012f,.004f,.015f,.036f,.018f,.005f,.95f);texture(p,.02f,.25f);p[22]=.065f;break;
            case 48:set(p,1.015f,1.025f,.99f,-.012f,-.007f,-.008f,.63f,1.25f);split(p,-.016f,.014f,.010f,.020f,.011f,-.009f,.58f);texture(p,.055f,1.10f);p[22]=.13f;break;
            case 49:set(p,1.045f,1.005f,.945f,.014f,.010f,.010f,.91f,1.06f);split(p,-.018f,.010f,.022f,.037f,.015f,-.010f,.70f);texture(p,.13f,.95f);p[20]=.88f;break;
            case 50:set(p,1.028f,1.025f,.985f,0,.008f,0,1.22f,1.19f);split(p,-.012f,.012f,.020f,.029f,.017f,-.011f,.45f);texture(p,.01f,.25f);p[21]=1;p[22]=.12f;break;
        }
        p[9]=FilmGrain.effective(stock,p[9]);
        float ev=(float)Math.pow(2,exposure);for(int i=0;i<3;i++)p[i]*=ev;
        p[3]-=cyan*.13f;p[4]-=magenta*.13f;p[5]-=yellow*.13f;return p;
    }
    private static void set(float[] p,float r,float g,float b,float lr,float lg,float lb,float sat,float contrast){p[0]=r;p[1]=g;p[2]=b;p[3]=lr;p[4]=lg;p[5]=lb;p[6]=sat;p[7]=contrast;}
    private static void split(float[] p,float r,float g,float b,float hr,float hg,float hb,float shoulder){p[10]=r;p[11]=g;p[12]=b;p[13]=hr;p[14]=hg;p[15]=hb;p[16]=shoulder;}
    private static void texture(float[] p,float fade,float grainScale){p[8]=Math.min(.8f,p[8]+fade);p[9]=Math.min(1,p[9]*grainScale);}
    public static float channel(float value,int c,float[] p){
        float v=((value-.5f)*p[7]+.5f)*p[c]+p[c+3];v=v*(1-p[8]*.35f)+p[8]*.16f;
        float k=Math.max(0,Math.min(1,v));v+=p[10+c]*(1-k)*(1-k)+p[13+c]*k*k;
        if(v>.65f)v=.65f+(v-.65f)/(1+p[16]*(v-.65f));return v;
    }
}
