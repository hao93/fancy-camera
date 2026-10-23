package com.silverroom.camera;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Bounded JSON interchange. Calibration is device-specific and never applied from a recipe. */
final class RecipeStore {
    static final int LIMIT=50,MAX_BYTES=131072;
    static final class Recipe {final String id,name;final Film.Settings settings;Recipe(String i,String n,Film.Settings s){id=i;name=n;settings=s;}}
    private final File file;
    RecipeStore(File folder){file=new File(folder,"recipes.json");}
    List<Recipe> list()throws Exception{if(!file.exists())return new ArrayList<>();if(file.length()>MAX_BYTES)throw new IOException("配方库过大");return parse(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));}
    private List<Recipe> parse(String text)throws Exception{if(text.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IOException("配方文件超过128KB");JSONObject root=new JSONObject(text);if(root.optInt("schema")!=1||!"FancyCamera.recipes".equals(root.optString("type")))throw new IOException("不是 Fancy Camera 配方文件");JSONArray items=root.getJSONArray("recipes");if(items.length()>LIMIT)throw new IOException("配方最多50份");List<Recipe> result=new ArrayList<>();for(int i=0;i<items.length();i++){JSONObject j=items.getJSONObject(i);String name=j.getString("name").trim();if(name.isEmpty()||name.length()>32||name.matches("(?s).*\\p{Cntrl}.*"))throw new IOException("配方名称无效");JSONObject values=j.getJSONObject("settings");if(!FilmCatalog.valid(values.optInt("stock",-1)))throw new IOException("未知基础胶片");String id=j.optString("id");if(!id.matches("[a-f0-9-]{36}"))id=UUID.randomUUID().toString();Film.Settings s=SettingsCodec.read(values);s.red=s.green=s.blue=s.contrast=1;s.calibrationEv=0;s.iso=0;s.grainEdge=4096;result.add(new Recipe(id,name,s));}return result;}
    String encode(List<Recipe> items)throws Exception{JSONObject root=new JSONObject();root.put("schema",1);root.put("type","FancyCamera.recipes");JSONArray all=new JSONArray();for(Recipe r:items){JSONObject item=new JSONObject();item.put("id",r.id);item.put("name",r.name);item.put("settings",SettingsCodec.write(r.settings));all.put(item);}root.put("recipes",all);return root.toString(2);}
    private void save(List<Recipe> items)throws Exception{String text=encode(items);parse(text);File temp=new File(file.getParentFile(),"recipes.tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(text.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);}
    void add(String name,Film.Settings settings)throws Exception{List<Recipe> items=list();if(items.size()>=LIMIT)throw new IOException("已达到50份配方上限");items.add(new Recipe(UUID.randomUUID().toString(),name.trim(),settings.copy()));save(items);}
    void remove(String id)throws Exception{List<Recipe> items=list();items.removeIf(r->r.id.equals(id));save(items);}
    int merge(String text)throws Exception{List<Recipe> incoming=parse(text),items=list();Set<String> ids=new HashSet<>();for(Recipe r:items)ids.add(r.id);int added=0;for(Recipe r:incoming){if(ids.contains(r.id))continue;if(items.size()>=LIMIT)throw new IOException("导入后将超过50份，未修改配方库");items.add(r);ids.add(r.id);added++;}save(items);return added;}
}
