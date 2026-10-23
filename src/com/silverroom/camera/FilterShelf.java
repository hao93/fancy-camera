package com.silverroom.camera;
import java.util.*;

/** Persistent shortcut/recent IDs remain independent from catalog order. */
public final class FilterShelf {
    private FilterShelf(){}
    private static final int[] DEFAULTS={6,7,1,4};
    public static List<Integer> parse(String csv,boolean defaults){
        List<Integer> out=new ArrayList<>();if(csv!=null)for(String s:csv.split(","))try{int id=Integer.parseInt(s.trim());if(FilmCatalog.valid(id)&&!out.contains(id))out.add(id);}catch(NumberFormatException ignored){}
        if(defaults&&csv==null)for(int id:DEFAULTS)out.add(id);
        int cap=defaults?FilmCatalog.NAMES.length:6;return new ArrayList<>(out.subList(0,Math.min(cap,out.size())));
    }
    public static String encode(List<Integer> ids){StringBuilder out=new StringBuilder();for(int id:ids){if(out.length()>0)out.append(',');out.append(id);}return out.toString();}
    public static List<Integer> visible(List<Integer> pins,int selected){
        List<Integer> out=new ArrayList<>();for(int id:pins)if(FilmCatalog.valid(id)&&!out.contains(id)&&out.size()<4)out.add(id);
        if(FilmCatalog.valid(selected)&&!out.contains(selected)){if(out.size()==4)out.remove(3);out.add(selected);}
        for(int id:DEFAULTS)if(out.size()<4&&!out.contains(id))out.add(id);return out;
    }
    public static List<Integer> remember(List<Integer> prior,int selected){List<Integer> out=new ArrayList<>();if(FilmCatalog.valid(selected))out.add(selected);for(int id:prior)if(FilmCatalog.valid(id)&&!out.contains(id)&&out.size()<6)out.add(id);return out;}
    public static List<Integer> toggle(List<Integer> prior,int id){
        List<Integer> out=new ArrayList<>(prior);if(!FilmCatalog.valid(id))return out;
        if(out.contains(id))out.remove(Integer.valueOf(id));else out.add(id);return out;
    }
    public static List<Integer> move(List<Integer> prior,int from,int to){List<Integer> out=new ArrayList<>(prior);if(from<0||from>=out.size()||to<0||to>=out.size())return out;Integer id=out.remove(from);out.add(to,id);return out;}
    public static boolean matches(int id,String query){if(!FilmCatalog.valid(id))return false;String aliases=id==6?"cc classicchrome":id==7?"cn classicnegative":id==8?"nn nostalgicnegative":id==9?"ra realaace":id==14?"eb bleach":id==15?"ac acros":id==16?"pn portrait":id>=24&&id<=26?"柯达":id==27?"钨丝灯":id==28?"伊尔福":id==29?"拍立得":id==30?"wkw":id==31?"韦斯 安德森":id==32?"nolan":id==33?"港风 香港":id==34?"韩系":id==35?"灰调":id==37?"北欧":id==41?"70年代":id==42?"80年代 港风 香港":id==43?"90年代 日系":id==44?"千禧":id==45?"数码相机":id==49?"漏光":id==50?"微缩 模型":"";int family=FilmGroups.family(id);aliases+=family==FilmGroups.LEICA?" leica 徕卡":family==FilmGroups.RICOH?" ricoh gr 理光":family==FilmGroups.FUJI?" fujifilm fuji 富士":"";String hay=FilmCatalog.NAMES[id]+" "+FilmCatalog.TAGS[id]+" "+FilmCatalog.DESCRIPTIONS[id]+" "+FilmGroups.LABELS[family]+" "+aliases;return hay.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));}
}

