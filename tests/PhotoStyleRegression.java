package com.silverroom.camera;

import org.json.*;
import java.util.*;

/** Saved per-image style provenance; the active camera and roll are not sources. */
public final class PhotoStyleRegression {
    static int count;
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);count++;System.out.println("PASS "+text);}
    public static void main(String[] args)throws Exception{
        JSONObject photo=new JSONObject("{\"stock\":51,\"roll_id\":\"mobile\",\"roll_snapshot\":{\"stock\":24}}");String before=photo.toString();
        check(PhotoStyle.stock(photo)==51&&PhotoStyle.name(photo).equals("哈苏自然"),"Album caption uses this photo's saved recipe rather than the roll's default film");
        check(photo.toString().equals(before),"Reading an album style never modifies saved photo metadata");
        photo.put("roll_id","another");photo.put("roll_snapshot",new JSONObject().put("stock",76));
        check(PhotoStyle.stock(photo)==51,"Moving a photo to a different roll keeps its original style label");
        Set<String> names=new HashSet<>();for(int i=0;i<FilmCatalog.NAMES.length;i++){JSONObject p=new JSONObject().put("stock",i);if(PhotoStyle.stock(p)!=i||!PhotoStyle.name(p).equals(FilmCatalog.NAMES[i]))throw new AssertionError("recipe "+i);names.add(PhotoStyle.name(p));}
        check(names.size()==77,"All 77 saved styles retain their full distinct album names");
        check(PhotoStyle.stock(new JSONObject().put("stock",0).put("imported",true))==0,"An imported original keeps its recorded neutral recipe instead of the active film");
        check(PhotoStyle.stock(null)==-1&&PhotoStyle.stock(new JSONObject())==-1&&PhotoStyle.name(new JSONObject()).equals("风格未记录"),"Photos without saved style metadata show an honest unknown label");
        check(PhotoStyle.stock(new JSONObject().put("stock",-1))==-1&&PhotoStyle.stock(new JSONObject().put("stock",999))==-1,"Invalid or future recipe IDs never masquerade as the default warm-gold film");
        check(PhotoStyle.stock(new JSONObject().put("stock","not-a-film"))==-1&&PhotoStyle.stock(new JSONObject().put("stock",JSONObject.NULL))==-1,"Malformed style metadata safely uses the unknown caption");
        System.out.println("ALL "+count+" PHOTO STYLE PROVENANCE CHECKS PASSED");
    }
}
