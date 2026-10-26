package com.silverroom.camera;

import org.json.JSONObject;

/** A photo's saved recipe, independent of the active film or its current roll. */
final class PhotoStyle {
    private PhotoStyle(){}
    static int stock(JSONObject photo){int id=photo==null?-1:photo.optInt("stock",-1);return FilmCatalog.valid(id)?id:-1;}
    static String name(JSONObject photo){int id=stock(photo);return id<0?"风格未记录":FilmCatalog.NAMES[id];}
}
