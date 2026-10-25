package com.silverroom.camera;
import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import org.json.*;

/** Executes unchanged RollRepository methods on real host SQLite through a tiny Android adapter. */
public final class StudioArchiveRegression {
 static int passed;static void check(boolean v,String n){if(!v)throw new AssertionError(n);passed++;System.out.println("PASS "+n);}
 static String hash(Path file)throws Exception{return Arrays.toString(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));}
 static void fixture(File root,String id,JSONObject meta)throws Exception{Files.write(new File(root,id+".json").toPath(),meta.toString().getBytes(StandardCharsets.UTF_8));Files.write(new File(root,id+".jpg").toPath(),new byte[]{1,2,3});Files.write(new File(root,id+"_original.jpg").toPath(),new byte[]{0,0,0,24,102,116,121,112,104,101,105,99,45,23});}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory(Paths.get(args[0]),"archive-");File negatives=root.resolve("negatives").toFile();negatives.mkdir();android.content.Context context=new android.content.Context(root.toString());
  String legacy="20261023_120000_000",fresh="20261025_160000_001",derived="20261025_160001_002",imported="20261025_160002_003";
  fixture(negatives,legacy,new JSONObject().put("stock",6));String originalHash=hash(new File(negatives,legacy+"_original.jpg").toPath());
  RollRepository repo=new RollRepository(context,negatives);repo.syncLegacy();repo.syncLegacy();
  check(repo.list("rolls",null).size()==3&&repo.items(RollRepository.HISTORY).size()==1,"Idempotent migration creates three defaults and one association per legacy negative");
  check(repo.count(RollRepository.HISTORY)==1&&hash(new File(negatives,legacy+"_original.jpg").toPath()).equals(originalHash),"Migration preserves original photo bytes and counts one exposure");
  repo.saveRoll(RollRepository.object("id","a","name","秋日,胶卷","kind","phone","capacity",2,"stock",28,"nominal_iso",400,"pinned",1));
  repo.saveRoll(RollRepository.object("id","b","name","实体卷","kind","physical","status",2));
  check(repo.list("rolls",null).get(0).optString("id").equals("a")&&repo.list("rolls","physical").size()==2,"Pinned ordering and physical-roll filtering execute in SQLite");
  JSONObject actual=RollRepository.object("iso",400,"aperture",1.75,"exposure_ns",8000000L,"make","vivo","model","V2366GA");
  JSONObject metadata=RollRepository.object("stock",28,"capture",actual,"archive_roll","a","captured_at",1234567890L);fixture(negatives,fresh,metadata);
  repo.photo(fresh,"a",repo.find("rolls","a").toString(),metadata,false);
  JSONObject derivative=RollRepository.copy(metadata);derivative.put("stock",20).put("parent_id",fresh);fixture(negatives,derived,derivative);repo.photo(derived,"a",repo.find("rolls","a").toString(),derivative,true);
  check(repo.count("a")==1&&repo.items("a").size()==2,"A developed derivative is associated but never counted as a second exposure");
  repo.annotate(fresh,RollRepository.object("notes","备注\n第二行","camera","Leica M","iso",100));
  check(repo.find("items",fresh).getJSONObject("capture").getInt("iso")==400&&repo.find("items",fresh).getJSONObject("annotations").getInt("iso")==100,"Supplementary values do not rewrite actual native EXIF fields");
  repo.photo(fresh,"a","{}",metadata,false);
  check(repo.find("items",fresh).getJSONObject("annotations").optString("notes").contains("第二行"),"Repeated metadata commit preserves existing user annotations");
  repo.move(Arrays.asList(fresh,derived),"b");repo.syncLegacy();
  check(repo.count("a")==0&&repo.count("b")==1&&repo.find("items",fresh).optString("roll_id").equals("b"),"Batch move is persistent and migration does not reassign existing items");
  boolean rejected=false;try{repo.move(Arrays.asList(fresh),"missing");}catch(IllegalArgumentException e){rejected=true;}
  check(rejected&&repo.find("items",fresh).optString("roll_id").equals("b"),"Invalid destination cannot partially alter membership");
  String meter=repo.meter("b",RollRepository.object("ei",800,"seconds",.004,"aperture",8,"ev100",12,"notes","实体相机建议"));
  check(repo.count("b")==2&&repo.find("items",meter).optString("kind").equals("meter"),"Explicit metering record is separate from a phone photograph");
  repo.reference(meter,"reference.jpg");repo.removeMeter(meter);
  check(repo.count("b")==1&&repo.meterTrash().size()==1,"Deleted metering record enters seven-day trash and leaves the active frame count");
  repo.restoreMeter(meter);check(repo.count("b")==2&&repo.find("items",meter).optString("reference").equals("reference.jpg"),"Restoring a metering record retains its reference and parameters");
  NegativeTrash.trash(negatives,fresh,System.currentTimeMillis());check(repo.count("b")==1,"Recycled phone photo leaves active roll count without deleting its association");
  NegativeTrash.restore(negatives,fresh);repo.syncLegacy();check(repo.count("b")==2&&repo.find("items",fresh).has("annotations"),"Restoring a photo recovers its moved roll and annotations");
  JSONObject source=RollRepository.object("imported",true,"stock",0,"source_mime","image/heic","source_name","original.heic","source_capture",actual);fixture(negatives,imported,source);repo.photo(imported,"a","{}",source,false);
  check(repo.find("items",imported).optString("kind").equals("import")&&!repo.find("items",imported).has("captured_at"),"Date-less byte-preserved import remains a distinct source type with no invented capture date");
  repo.saveProfile("equipment",RollRepository.object("id","lens","kind","lens","name","50mm","focal",50));repo.saveProfile("film_profiles",RollRepository.object("id","hp5","name","HP5+","type","黑白","stock",28,"nominal_iso",400));
  check(repo.list("equipment","lens").size()==1&&repo.list("film_profiles",null).get(0).optInt("stock")==28,"Equipment and custom film profiles persist independently of renderer IDs");
  String csv=repo.csv("b");check(csv.startsWith("\ufeff")&&csv.contains("备注\n第二行")&&csv.contains("实体相机建议")&&csv.contains("手机实际参数"),"CSV preserves UTF8, multiline notes and distinct parameter provenance");
  repo.removeRoll("b");check(repo.find("rolls","b")==null&&repo.find("items",fresh).optString("roll_id").equals(RollRepository.HISTORY)&&new File(negatives,fresh+".jpg").isFile(),"Deleting a roll moves associations to history and preserves image files");
  rejected=false;try{repo.removeRoll(RollRepository.MOBILE);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"The default shooting roll cannot be accidentally deleted");
  repo.close();repo=new RollRepository(context,negatives);repo.syncLegacy();
  check(repo.find("items",fresh).getJSONObject("annotations").optString("camera").equals("Leica M")&&repo.find("items",derived).optInt("exposure_count")==0,"Reopening SQLite preserves annotations, associations and derivative counts");
  check(hash(new File(negatives,legacy+"_original.jpg").toPath()).equals(originalHash),"Migration, movement, deletion and reopen never rewrite the original source bytes");repo.close();
  System.out.println("ALL "+passed+" PRODUCTION SQLITE ARCHIVE CHECKS PASSED");
 }
}
