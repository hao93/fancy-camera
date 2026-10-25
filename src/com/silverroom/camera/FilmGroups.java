package com.silverroom.camera;
import java.util.*;

/** Stable recipe families shared by the main reel, library and search. */
public final class FilmGroups {
    private FilmGroups(){}
    public static final int ALL=0,FUJI=1,LEICA=2,RICOH=3,ORIGINAL=4,CLASSIC=5,CINEMA=6,MODERN=7,RETRO=8,SPECIAL=9,FAVORITES=10,HASSELBLAD=11;
    public static final String[] LABELS={"全部","富士","徕卡","理光","原生","经典胶片","电影美学","现代调色","复古年代","特色摄影","收藏","哈苏"};
    public static final String[] SHORT_LABELS={"全部","富士","徕卡","理光","原生","经典","电影","现代","年代","特色","收藏","哈苏"};
    public static int safe(int group){return group>=0&&group<LABELS.length?group:ALL;}
    public static int family(int id){if(!FilmCatalog.valid(id))return ALL;if(id>=51)return id<57?HASSELBLAD:id<63?CLASSIC:CINEMA;return id<6?ORIGINAL:id<18?FUJI:id<21?LEICA:id<24?RICOH:id<30?CLASSIC:id<35?CINEMA:id<41?MODERN:id<46?RETRO:SPECIAL;}
    public static int[] order(boolean favorites){return favorites?new int[]{ALL,FUJI,LEICA,RICOH,HASSELBLAD,ORIGINAL,CLASSIC,CINEMA,MODERN,RETRO,SPECIAL,FAVORITES}:new int[]{ALL,FUJI,LEICA,RICOH,HASSELBLAD,ORIGINAL,CLASSIC,CINEMA,MODERN,RETRO,SPECIAL};}
    private static final int[] LIBRARY={ALL,FAVORITES,ALL,FUJI,LEICA,RICOH,HASSELBLAD,ORIGINAL,CLASSIC,CINEMA,MODERN,RETRO,SPECIAL};
    public static int libraryGroup(int tab){return tab>=0&&tab<LIBRARY.length?LIBRARY[tab]:ALL;}
    public static String[] libraryLabels(){String[] out=new String[LIBRARY.length];out[0]="全部 "+FilmCatalog.NAMES.length;out[1]="收藏";out[2]="最近";for(int i=3;i<out.length;i++){int g=LIBRARY[i];out[i]=LABELS[g]+(g==FUJI||g==LEICA||g==RICOH||g==HASSELBLAD?"风格":g==ORIGINAL?"胶片":"");}return out;}
    public static boolean contains(int group,int id){return FilmCatalog.valid(id)&&(safe(group)==ALL||family(id)==safe(group));}
    public static List<Integer> ids(int group){List<Integer> ids=new ArrayList<>();for(int i=0;i<FilmCatalog.NAMES.length;i++)if(contains(group,i))ids.add(i);return ids;}
    public static List<Integer> ids(int group,List<Integer> pins){return group==FAVORITES?new ArrayList<>(pins):ids(group);}
    public static int choose(int group,int current,int remembered){if(contains(group,current))return current;if(contains(group,remembered))return remembered;List<Integer> list=ids(group);return list.isEmpty()?1:list.get(0);}
    public static int choose(int group,int current,int remembered,List<Integer> pins){if(group!=FAVORITES)return choose(group,current,remembered);if(pins.contains(current))return current;if(pins.contains(remembered))return remembered;return pins.isEmpty()?1:pins.get(0);}
}
