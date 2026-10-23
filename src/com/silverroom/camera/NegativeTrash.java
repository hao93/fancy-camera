package com.silverroom.camera;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Seven-day private recycle bin. Album copies and arbitrary filesystem paths are never touched. */
public final class NegativeTrash {
    public static final long RETENTION=7L*24*60*60*1000;
    private NegativeTrash(){}
    private static File safe(File root,String name,boolean directory)throws IOException{File f=new File(root,name);if(Files.isSymbolicLink(f.toPath())||!f.getCanonicalFile().getParentFile().equals(root.getCanonicalFile())||(f.exists()&&(directory?!f.isDirectory():!f.isFile())))throw new IOException("Invalid recycle-bin path");return f;}
    private static File bin(File root)throws IOException{File f=safe(root,".trash",true);if(!f.exists()&&!f.mkdir())throw new IOException("Cannot create recycle bin");return f;}
    private static File entry(File root,String id)throws IOException{if(!NegativeStore.valid(id))throw new IOException("Invalid negative ID");return safe(bin(root),id,true);}
    public static File image(File root,String id)throws IOException{return safe(entry(root,id),id+".jpg",false);}
    public static long deletedAt(File root,String id)throws IOException{File f=safe(entry(root,id),".when",false);if(f.length()>32)throw new IOException("Invalid deletion record");try{return Long.parseLong(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));}catch(NumberFormatException e){throw new IOException(e);}}
    public static List<String> list(File root){List<String> ids=new ArrayList<>();try{File[] entries=bin(root).listFiles();if(entries!=null)for(File f:entries)if(NegativeStore.valid(f.getName())&&f.isDirectory()&&!Files.isSymbolicLink(f.toPath())&&safe(f,".ready",false).exists())ids.add(f.getName());}catch(IOException ignored){}Collections.sort(ids,Collections.reverseOrder());return ids;}
    public static synchronized boolean trash(File root,String id,long now)throws IOException{
        File target=entry(root,id);if(target.exists())throw new IOException("回收站中存在同名底片");List<File> sources=new ArrayList<>();for(String name:NegativeStore.names(id)){File file=safe(root,name,false);if(file.exists())sources.add(file);}if(sources.isEmpty())return true;
        File stage=safe(bin(root),".moving-"+id+"-"+UUID.randomUUID(),true);if(!stage.mkdir())throw new IOException("Cannot prepare recycle operation");
        try{Files.write(new File(stage,".id").toPath(),id.getBytes(StandardCharsets.UTF_8));Files.write(new File(stage,".when").toPath(),Long.toString(now).getBytes(StandardCharsets.UTF_8));for(File f:sources)if(!f.renameTo(safe(stage,f.getName(),false)))throw new IOException("Cannot move negative to recycle bin");Files.write(new File(stage,".ready").toPath(),new byte[]{1});if(!stage.renameTo(target))throw new IOException("Cannot commit recycle operation");return true;}
        catch(IOException e){if(!new File(stage,".ready").exists())rollback(root,stage,id);throw e;}
    }
    private static void rollback(File root,File from,String id)throws IOException{for(String name:NegativeStore.names(id)){File f=safe(from,name,false),to=safe(root,name,false);if(f.exists()&&(to.exists()||!f.renameTo(to)))throw new IOException("Cannot roll back recycle operation");}cleanMarkers(from);}
    private static void cleanMarkers(File dir)throws IOException{for(String name:new String[]{".id",".when",".ready",".restoring"}){File f=safe(dir,name,false);if(f.exists()&&!f.delete())throw new IOException("Cannot remove recycle marker");}dir.delete();}
    public static synchronized boolean restore(File root,String id)throws IOException{File dir=entry(root,id);if(!safe(dir,".ready",false).isFile())return false;for(String name:NegativeStore.names(id))if(safe(root,name,false).exists())throw new IOException("已存在同名底片，未覆盖");Files.write(new File(dir,".restoring").toPath(),new byte[]{1});finishRestore(root,dir,id);return true;}
    private static void finishRestore(File root,File dir,String id)throws IOException{for(String name:NegativeStore.names(id)){File from=safe(dir,name,false),to=safe(root,name,false);if(from.exists()&&(to.exists()||!from.renameTo(to)))throw new IOException("Cannot complete restore");}cleanMarkers(dir);}
    public static synchronized void erase(File root,String id)throws IOException{File dir=entry(root,id);if(!dir.exists())return;for(String name:NegativeStore.names(id)){File f=safe(dir,name,false);if(f.exists()&&!f.delete())throw new IOException("Cannot erase recycled component");}cleanMarkers(dir);}
    public static synchronized void recover(File root,long now){try{File[] all=bin(root).listFiles();if(all==null)return;for(File dir:all){if(!dir.isDirectory()||Files.isSymbolicLink(dir.toPath()))continue;try{if(dir.getName().startsWith(".moving-")){File idFile=safe(dir,".id",false);if(!idFile.isFile()||idFile.length()>32)continue;String id=new String(Files.readAllBytes(idFile.toPath()),StandardCharsets.UTF_8);if(!NegativeStore.valid(id))continue;if(safe(dir,".ready",false).exists()){File to=entry(root,id);if(!to.exists())dir.renameTo(to);}else rollback(root,dir,id);}else if(NegativeStore.valid(dir.getName())){String id=dir.getName();if(safe(dir,".restoring",false).exists())finishRestore(root,dir,id);else if(safe(dir,".ready",false).exists()&&now-deletedAt(root,id)>=RETENTION)erase(root,id);}}catch(IOException ignored){}}}catch(IOException ignored){}}
}
