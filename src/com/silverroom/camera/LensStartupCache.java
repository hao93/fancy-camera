package com.silverroom.camera;
import java.util.*;
/** Only real-preview successes are persisted; metadata visibility alone never populates this cache. */
final class LensStartupCache {
 interface Store {String get(String key);void put(String key,String value);void remove(String key);void clear();}
 private final Store store;private final String prefix;
 LensStartupCache(Store data,String firmware){store=data;prefix="lens_start_v1/"+firmware+"/";}
 LensPlan.Route route(List<LensPlan.Route> routes,float mm,boolean front,Set<String> failed){String hint=store.get(prefix+"route/"+(front?0:LensPlan.base(mm)));if(hint!=null)for(LensPlan.Route r:routes)if(r.front==front&&r.base==(front?0:LensPlan.base(mm))&&r.key().equals(hint)&&!failed.contains(r.key()))return r;return LensPlan.select(routes,mm,front,failed);}
 int profile(String key,int count){try{int n=Integer.parseInt(store.get(prefix+"profile/"+key));return n>=0&&n<count?n:-1;}catch(RuntimeException e){return -1;}}
 void verified(LensPlan.Route route,String key,int profile){save(prefix+"route/"+route.base,route.key());save(prefix+"profile/"+key,Integer.toString(profile));}
 private void save(String key,String value){if(!value.equals(store.get(key)))store.put(key,value);}
 void clear(){store.clear();}
 void invalidate(LensPlan.Route route,String key){store.remove(prefix+"profile/"+key);if(route.key().equals(store.get(prefix+"route/"+route.base)))store.remove(prefix+"route/"+route.base);}
}
