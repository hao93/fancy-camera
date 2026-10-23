package com.silverroom.camera;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Deletes only a validated negative's three private files; never follows album URIs. */
public final class NegativeStore {
    private NegativeStore(){}
    public static boolean valid(String id){return id!=null&&id.matches("[0-9]{8}_[0-9]{6}_[0-9]{3}");}
    private static String[] names(String id){return new String[]{id+"_original.jpg",id+".jpg",id+".json"};}
    private static File child(File root,String name)throws IOException{File f=new File(root,name);if(Files.isSymbolicLink(f.toPath())||!root.getCanonicalFile().equals(f.getCanonicalFile().getParentFile())||(f.exists()&&!f.isFile()))throw new IOException("Invalid negative component");return f;}
    public static List<String> list(File root){List<String> ids=new ArrayList<>();File[] files=root.listFiles();if(files!=null)for(File f:files){String n=f.getName();if(n.endsWith(".json")){String id=n.substring(0,n.length()-5);if(valid(id)&&f.isFile()&&!Files.isSymbolicLink(f.toPath()))ids.add(id);}}Collections.sort(ids,Collections.reverseOrder());return ids;}
    public static synchronized boolean delete(File folder,String id)throws IOException{
        if(!valid(id))throw new IllegalArgumentException("Invalid negative ID");File root=folder.getCanonicalFile();List<File> sources=new ArrayList<>();for(String n:names(id)){File f=child(root,n);if(f.exists())sources.add(f);}if(sources.isEmpty())return true;
        File stage=new File(root,".delete-"+UUID.randomUUID());if(!stage.mkdir())throw new IOException("Cannot prepare deletion");
        try{
            Files.write(new File(stage,".record").toPath(),id.getBytes(StandardCharsets.UTF_8));
            for(File f:sources)if(!f.renameTo(new File(stage,f.getName())))throw new IOException("Cannot move negative component");
            Files.write(new File(stage,".committed").toPath(),new byte[]{1});
        }catch(IOException failure){try{restore(root,stage,id);}catch(IOException rollback){failure.addSuppressed(rollback);}throw failure;}
        purge(stage,id);return true;
    }
    private static void restore(File root,File stage,String id)throws IOException{
        for(String name:names(id)){File from=child(stage,name),to=child(root,name);if(from.exists()&&(to.exists()||!from.renameTo(to)))throw new IOException("Cannot restore interrupted deletion");}
        new File(stage,".record").delete();stage.delete();
    }
    private static void purge(File stage,String id)throws IOException{
        for(String name:names(id)){File f=child(stage,name);if(f.exists()&&!f.delete())return;}
        new File(stage,".record").delete();new File(stage,".committed").delete();stage.delete();
    }
    /** Finish committed deletes, restore a delete interrupted before commit. */
    public static synchronized void recover(File folder){
        File[] stages=folder.listFiles();if(stages==null)return;for(File stage:stages){if(!stage.getName().matches("\\.delete-[a-f0-9-]{36}")||!stage.isDirectory()||Files.isSymbolicLink(stage.toPath()))continue;
            try{File record=child(stage,".record");if(!record.isFile()||record.length()>32)continue;String id=new String(Files.readAllBytes(record.toPath()),StandardCharsets.UTF_8);if(!valid(id))continue;if(child(stage,".committed").exists())purge(stage,id);else restore(folder,stage,id);}catch(IOException ignored){}
        }
    }
}
