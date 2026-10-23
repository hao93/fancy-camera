package com.silverroom.camera;
import java.util.*;

public final class NegativeSelection {
    private final LinkedHashSet<String> live=new LinkedHashSet<>(),chosen=new LinkedHashSet<>();
    public void items(Collection<String> ids){live.clear();for(String id:ids)if(NegativeStore.valid(id))live.add(id);chosen.retainAll(live);}
    public void toggle(String id){if(!live.contains(id))return;if(!chosen.remove(id))chosen.add(id);}
    public void all(){if(chosen.size()==live.size())chosen.clear();else{chosen.clear();chosen.addAll(live);}}
    public boolean contains(String id){return chosen.contains(id);}
    public int count(){return chosen.size();}
    public boolean every(){return !live.isEmpty()&&chosen.size()==live.size();}
    public List<String> snapshot(){return new ArrayList<>(chosen);}
    public void clear(){chosen.clear();}
}
