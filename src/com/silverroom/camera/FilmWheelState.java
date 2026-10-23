package com.silverroom.camera;

import java.util.*;

/** A stable reel: crossing a detent changes the film without rebuilding its order. */
public final class FilmWheelState {
    private List<Integer> ids=Collections.singletonList(1);
    private float position;
    public void reset(List<Integer> entries,int selected){
        ArrayList<Integer> copy=new ArrayList<>();for(int id:entries)if(FilmCatalog.valid(id)&&!copy.contains(id))copy.add(id);
        if(FilmCatalog.valid(selected)&&!copy.contains(selected))copy.add(selected);
        if(copy.isEmpty())copy.add(1);ids=copy;position=Math.max(0,ids.indexOf(selected));
    }
    public int count(){return ids.size();}
    public int id(int index){return ids.get(Math.max(0,Math.min(ids.size()-1,index)));}
    public float position(){return position;}
    public int index(){return Math.max(0,Math.min(ids.size()-1,Math.round(position)));}
    public int selected(){return id(index());}
    public void position(float next){if(Float.isFinite(next))position=Math.max(0,Math.min(ids.size()-1,next));}
    public int target(float velocity){float v=Float.isFinite(velocity)?Math.max(-12,Math.min(12,velocity)):0;return Math.max(0,Math.min(ids.size()-1,Math.round(position+v*.14f)));}
}
