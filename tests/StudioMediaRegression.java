package com.silverroom.camera;
import javax.tools.*;import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.net.*;import java.util.*;import org.json.*;import android.graphics.Bitmap;

/** Byte-preserving production IO and actual print rasterizer, independent of a phone/gallery provider. */
public final class StudioMediaRegression {
 static int checks;static void check(boolean v,String name){if(!v)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static String method(String source,String name){int start=source.indexOf(name),body=source.indexOf('{',start),depth=0;if(start<0)throw new AssertionError(name);for(int i=body;i<source.length();i++){if(source.charAt(i)=='{')depth++;if(source.charAt(i)=='}'&&--depth==0)return source.substring(start,i+1);}throw new AssertionError(name);}
 static final class Source implements PrintRenderer.Source{
  int decoded;public Bitmap image(String id,int edge){decoded++;int w=id.equals("vertical")?80:160,h=id.equals("vertical")?160:80;int[] pixels=new int[w*h];Arrays.fill(pixels,0xff3495a7);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);b.setPixels(pixels,0,w,0,0,w,h);return b;}
  public PrintMetadata metadata(String id){return new PrintMetadata("20261025_150000_000",new JSONObject().put("stock",28).put("lens","85mm").put("capture",new JSONObject().put("make","vivo").put("model","V2366GA").put("focal_mm",22.48).put("iso",400).put("aperture",2.67).put("exposure_ns",8000000)));}
 }
 public static void main(String[] args)throws Exception{
  Path root=Paths.get(args[0]),dir=root.resolve("build/studio-media-tests"),classes=dir.resolve("classes");Files.createDirectories(classes);
  String media=new String(Files.readAllBytes(root.resolve("src/com/silverroom/camera/MediaArchive.java")),StandardCharsets.UTF_8),activity=new String(Files.readAllBytes(root.resolve("src/com/silverroom/camera/MainActivity.java")),StandardCharsets.UTF_8);
  String code="package com.silverroom.camera;import java.io.*;import java.security.*;import java.util.*;import java.nio.file.*;public class ByteProbe{"+method(media,"static void copy(")+method(media,"static String digest(")+method(activity,"static String gpsRational(")+
   "public static void run(String path)throws Exception{byte[] bytes=new byte[131073];new Random(13).nextBytes(bytes);ByteArrayOutputStream output=new ByteArrayOutputStream();copy(new ByteArrayInputStream(bytes),output);if(!Arrays.equals(bytes,output.toByteArray()))throw new AssertionError(\"Source bytes changed\");File source=Paths.get(path,\"source.heic\").toFile();Files.write(source.toPath(),bytes);String first=digest(source);bytes[66000]^=1;File other=Paths.get(path,\"other.heic\").toFile();Files.write(other.toPath(),bytes);if(first.length()!=64||first.equals(digest(other))||!first.equals(digest(source)))throw new AssertionError(\"Digest/source mutation\");boolean failed=false;try{copy(new ByteArrayInputStream(bytes),new OutputStream(){public void write(int n)throws IOException{throw new IOException(\"No space\");}});}catch(IOException e){failed=true;}if(!failed)throw new AssertionError(\"Write failure hidden\");if(!gpsRational(-22.5).equals(\"22/1,30/1,0/1000000\")||!gpsRational(0).equals(\"0/1,0/1,0/1000000\"))throw new AssertionError(\"GPS rational\");}}";
  Path file=dir.resolve("ByteProbe.java");Files.write(file,code.getBytes(StandardCharsets.UTF_8));if(ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","8","-encoding","UTF-8","-d",classes.toString(),file.toString())!=0)throw new AssertionError("Byte fixture compile");
  try(URLClassLoader loader=new URLClassLoader(new URL[]{classes.toUri().toURL()},null)){try{loader.loadClass("com.silverroom.camera.ByteProbe").getMethod("run",String.class).invoke(null,dir.toString());}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError("Byte-preserving production IO",e.getCause());}}
  check(true,"Production import/export copy preserves arbitrary original bytes across multiple buffers, detects modifications and propagates write failures");
  check(true,"Optional native GPS uses valid absolute rational degrees with a separate hemisphere reference");
  Source source=new Source();for(boolean dark:new boolean[]{false,true})for(int style:new int[]{0,1,2})for(String id:new String[]{"vertical","horizontal"}){
   PrintOptions options=new PrintOptions(style,false,true,true,true,dark,true,true,true);Bitmap b=PrintRenderer.render(source,Arrays.asList(id),options,0,1024,()->false);int[] first=new int[1];b.getPixels(first,0,1,0,0,1,1);check(first[0]==(style==0?0xff3495a7:dark?0xff111113:0xfffffaf2),"Production single export uses correct "+(dark?"black":"white")+" border in style "+style+" for "+id);
   check(b.getWidth()>0&&b.getHeight()>0&&Math.max(b.getWidth(),b.getHeight())<=1024,"Print dimensions remain bounded with five caption rows for "+id+" style "+style+" dark "+dark);b.recycle();
  }
  List<String> ids=new ArrayList<>();for(int i=0;i<10;i++)ids.add(i%2==0?"vertical":"horizontal");PrintOptions contact=new PrintOptions(1,true,true,true,true,true,true,true,true);int before=source.decoded;Bitmap page=PrintRenderer.render(source,ids,contact,1,1024,()->false);check(source.decoded==before+1&&page.getWidth()==724&&page.getHeight()==1024,"Ten-photo contact export decodes only the final remaining image on page two");page.recycle();
  boolean cancelled=false;try{PrintRenderer.render(source,ids,contact,0,1024,()->true);}catch(InterruptedIOException e){cancelled=true;}check(cancelled,"Contact cancellation aborts before another source is decoded");
  PrintMetadata supplement=new PrintMetadata("20261025_150000_000",new JSONObject().put("imported",true).put("annotations",new JSONObject().put("date","2020:01:02 03:04:05")));
  check(supplement.date.startsWith("补填")&&supplement.exifDate.isEmpty(),"Supplementary capture date is labeled and never promoted into original EXIF");
  PrintMetadata imported=new PrintMetadata("20261025_150000_000",new JSONObject().put("imported",true).put("source_capture",new JSONObject().put("mm",35).put("focal_mm",23).put("model","Original camera")));
  check(imported.focal.equals("35mm")&&imported.camera.equals("Original camera"),"Import captions use original equivalent focal length and camera model without a phone-model substitution");
  System.out.println("ALL "+checks+" PRODUCTION MEDIA / PRINT CHECKS PASSED");
 }
}
