package com.silverroom.camera;

import java.net.URI;
import java.nio.file.Files;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;

/** Strict, Android-independent release/download rules. No credentials are embedded. */
public final class UpdatePolicy {
    private UpdatePolicy(){}
    public static final String REPO="hao93/fancy-camera",PACKAGE="com.silverroom.camera";
    public static final String CERT="12f980371125eb5f2274ddf14fc421cba077255116401a399862aa7588e4b8bb";
    public static final long MAX_APK=200L*1024*1024;
    public static String endpoint(){return "https://api.github.com/repos/"+REPO+"/releases/latest";}
    public static boolean version(String name){return name!=null&&name.length()<=48&&name.matches("[0-9]{1,8}\\.[0-9]{1,8}\\.[0-9]{1,8}");}
    public static String apkName(String version){if(!version(version))throw new IllegalArgumentException("Invalid version");return "FancyCamera-"+version+".apk";}
    public static boolean digest(String value){return value!=null&&value.matches("[a-fA-F0-9]{64}");}
    public static boolean newer(long current,long candidate,boolean draft,boolean prerelease,int minSdk,int sdk){return current>0&&candidate>current&&!draft&&!prerelease&&minSdk>=30&&minSdk<=sdk;}
    public static boolean https(String url){try{URI u=new URI(url);return "https".equals(u.getScheme())&&u.getRawUserInfo()==null&&u.getFragment()==null&&(u.getPort()==-1||u.getPort()==443)&&u.getHost()!=null;}catch(Exception e){return false;}}
    public static boolean assetUrl(String url,String tag,String name){try{if(!https(url)||!tag.matches("v[0-9.]+")||!name.matches("[A-Za-z0-9._-]+"))return false;URI u=new URI(url);return "github.com".equalsIgnoreCase(u.getHost())&&u.getRawPath().equals("/"+REPO+"/releases/download/"+tag+"/"+name)&&u.getRawQuery()==null;}catch(Exception e){return false;}}
    public static boolean redirect(String url){if(!https(url))return false;String host=URI.create(url).getHost().toLowerCase(Locale.ROOT);return host.equals("github.com")||host.equals("release-assets.githubusercontent.com")||host.equals("objects.githubusercontent.com")||host.equals("github-releases.githubusercontent.com");}
    public static boolean archive(String packageName,long code,String name,String certificate,long expectedCode,String expectedName){return PACKAGE.equals(packageName)&&code==expectedCode&&version(name)&&name.equals(expectedName)&&CERT.equalsIgnoreCase(certificate);}
    public static String sha256(File file)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}return hex(digest.digest());}
    public static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    public static File pending(File cache)throws IOException{File dir=new File(cache,"updates");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create update cache");File file=new File(dir,"pending-update.apk");if(Files.isSymbolicLink(dir.toPath())||Files.isSymbolicLink(file.toPath())||!file.getCanonicalFile().getParentFile().equals(dir.getCanonicalFile()))throw new IOException("Invalid update cache");return file;}
}
