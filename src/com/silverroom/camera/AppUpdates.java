package com.silverroom.camera;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.ProgressBar;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.concurrent.*;

/** Startup/manual only. Checking errors and no-update responses are always silent. */
final class AppUpdates {
    interface Ui {boolean ready();void checking(boolean active);void beforePrompt();}
    private final Activity owner;private final Ui ui;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile boolean closed;private volatile HttpsURLConnection connection;private volatile int downloadEpoch;
    private boolean foreground,checking,downloading,awaitingPermission,permissionLaunched,installReady,installing;private long offered=-1;private Info available;private AlertDialog dialog;private ProgressBar bar;
    private static final class Asset {final String url,name,digest;final long size;Asset(JSONObject j){url=j.optString("browser_download_url");name=j.optString("name");size=j.optLong("size");digest=j.optString("digest");}}
    private static final class Info {final long code,size;final String name,url,sha,notes;Info(long c,String n,String u,long s,String h,String body){code=c;name=n;url=u;size=s;sha=h;notes=body;}}
    AppUpdates(Activity a,Ui listener){owner=a;ui=listener;}
    long current()throws PackageManager.NameNotFoundException{return owner.getPackageManager().getPackageInfo(owner.getPackageName(),0).getLongVersionCode();}
    void foreground(boolean automatic){foreground=true;installing=false;if(permissionLaunched){permissionLaunched=false;if(!owner.getPackageManager().canRequestPackageInstalls())awaitingPermission=false;}if(automatic){offered=-1;check();}flush();}
    void background(){foreground=false;}
    void check(){if(closed||checking||downloading||installing||awaitingPermission||installReady||!UpdatePolicy.PACKAGE.equals(owner.getPackageName()))return;offered=-1;checking=true;ui.checking(true);try{worker.execute(()->{Info found=null;try{found=latest();}catch(Exception ignored){}Info result=found;main.post(()->{if(closed)return;checking=false;ui.checking(false);available=result;flush();});});}catch(RejectedExecutionException ignored){checking=false;ui.checking(false);}}
    private JSONObject json(String url,boolean api)throws Exception{HttpsURLConnection c=open(url,api);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(closed||out.size()+n>1024*1024)throw new IOException("Metadata limit");out.write(buf,0,n);}return new JSONObject(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));}finally{c.disconnect();if(connection==c)connection=null;}}
    private HttpsURLConnection open(String address,boolean api)throws Exception{
        String next=address;for(int redirect=0;redirect<=5;redirect++){
            if(closed||Thread.currentThread().isInterrupted()||(!api&&!UpdatePolicy.redirect(next))||(api&&!UpdatePolicy.endpoint().equals(next)))throw new IOException("Untrusted update address");
            HttpsURLConnection c=(HttpsURLConnection)new URL(next).openConnection();connection=c;c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);c.setRequestProperty("User-Agent","FancyCamera-Android");c.setRequestProperty("Accept",api?"application/vnd.github+json":"application/octet-stream");if(api)c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
            int status;try{status=c.getResponseCode();}catch(Exception e){c.disconnect();throw e;}if(status==200)return c;if(!api&&(status==301||status==302||status==303||status==307||status==308)){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Missing redirect");next=new URL(new URL(next),location).toString();continue;}c.disconnect();throw new IOException("Update request unavailable");
        }throw new IOException("Redirect limit");
    }
    private Info latest()throws Exception{
        JSONObject release=json(UpdatePolicy.endpoint(),true);if(release.optBoolean("draft",true)||release.optBoolean("prerelease",true))return null;String tag=release.optString("tag_name"),name=tag.startsWith("v")?tag.substring(1):"";if(!UpdatePolicy.version(name))return null;
        JSONArray assets=release.optJSONArray("assets");if(assets==null)return null;Asset apk=null,manifest=null;for(int i=0;i<assets.length();i++){Asset a=new Asset(assets.getJSONObject(i));if(a.name.equals(UpdatePolicy.apkName(name)))apk=a;else if(a.name.equals("update.json"))manifest=a;}
        if(apk==null||manifest==null||manifest.size<1||manifest.size>1024*1024||apk.size<1||apk.size>UpdatePolicy.MAX_APK||!UpdatePolicy.assetUrl(apk.url,tag,apk.name)||!UpdatePolicy.assetUrl(manifest.url,tag,manifest.name))return null;
        JSONObject update=json(manifest.url,false);long code=update.optLong("versionCode");String sha=update.optString("sha256");int minSdk=update.optInt("minSdk",0);
        if(update.optInt("schema")!=1||!UpdatePolicy.PACKAGE.equals(update.optString("packageName"))||!name.equals(update.optString("versionName"))||!apk.name.equals(update.optString("apkName"))||apk.size!=update.optLong("size")||!UpdatePolicy.digest(sha)||!UpdatePolicy.newer(current(),code,false,false,minSdk,Build.VERSION.SDK_INT))return null;
        if(!apk.digest.isEmpty()&&!apk.digest.equalsIgnoreCase("sha256:"+sha))return null;
        String body=release.optString("body","");if(body.length()>2400)body=body.substring(0,2400);return new Info(code,name,apk.url,apk.size,sha,body);
    }
    void flush(){
        if(closed||installing||!foreground||!ui.ready())return;
        if(awaitingPermission){if(owner.getPackageManager().canRequestPackageInstalls()){awaitingPermission=false;install();}return;}
        if(installReady){install();return;}
        if(available==null||downloading||offered==available.code||(dialog!=null&&dialog.isShowing()))return;
        try{if(available.code<=current()){available=null;return;}}catch(Exception e){return;}
        Info candidate=available;offered=candidate.code;ui.beforePrompt();dialog=new AlertDialog.Builder(owner,ThemeUi.alertTheme()).setTitle("发现新版本 "+candidate.name).setMessage((candidate.notes.isEmpty()?"更新 Fancy Camera，保留底片和设置。":candidate.notes)+"\n\n下载后由系统确认安装。").setNegativeButton("稍后",null).setPositiveButton("下载并安装",(d,w)->download(candidate)).create();dialog.show();
    }
    private void download(Info info){
        if(closed||downloading)return;available=info;downloading=true;int epoch=++downloadEpoch;bar=new ProgressBar(owner,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setPadding(32,24,32,24);dialog=new AlertDialog.Builder(owner,ThemeUi.alertTheme()).setTitle("下载 "+info.name).setMessage("正在下载并校验更新…").setView(bar).setNegativeButton("取消",(d,w)->cancel()).setOnCancelListener(d->cancel()).create();dialog.show();
        worker.execute(()->{File temp=null;try{
            File target=UpdatePolicy.pending(owner.getCacheDir());if(!verified(target,info)){
                temp=new File(target.getParentFile(),"download-"+epoch+".part");HttpsURLConnection c=open(info.url,false);try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(temp)){byte[] buf=new byte[65536];long total=0;int n,last=-1;while((n=in.read(buf))!=-1){if(closed||epoch!=downloadEpoch)throw new IOException("Cancelled");total+=n;if(total>info.size||total>UpdatePolicy.MAX_APK)throw new IOException("APK size mismatch");out.write(buf,0,n);int percent=(int)(total*100/info.size);if(percent!=last){last=percent;main.post(()->{if(!closed&&epoch==downloadEpoch&&bar!=null)bar.setProgress(percent);});}}out.getFD().sync();if(total!=info.size)throw new IOException("Incomplete APK");}finally{c.disconnect();if(connection==c)connection=null;}
                if(closed||epoch!=downloadEpoch||!verified(temp,info))throw new IOException("APK verification failed");Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);temp=null;
            }
            main.post(()->{if(closed||epoch!=downloadEpoch)return;downloading=false;installReady=true;if(dialog!=null)dialog.dismiss();flush();});
        }catch(Exception e){main.post(()->{if(closed||epoch!=downloadEpoch)return;downloading=false;if(dialog!=null&&dialog.isShowing()){dialog.setMessage("下载未完成，可稍后重试。");dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText("关闭");}});}finally{if(temp!=null)temp.delete();}});
    }
    private void cancel(){downloadEpoch++;downloading=false;HttpsURLConnection c=connection;if(c!=null)c.disconnect();}
    private boolean verified(File file,Info info)throws Exception{
        if(!file.isFile()||Files.isSymbolicLink(file.toPath())||file.length()!=info.size||!UpdatePolicy.sha256(file).equalsIgnoreCase(info.sha))return false;PackageManager pm=owner.getPackageManager();PackageInfo apk=pm.getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES),installed=pm.getPackageInfo(owner.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
        if(apk==null||apk.signingInfo==null||installed.signingInfo==null||apk.signingInfo.getApkContentsSigners().length!=1||installed.signingInfo.getApkContentsSigners().length!=1)return false;
        String cert=UpdatePolicy.hex(MessageDigest.getInstance("SHA-256").digest(apk.signingInfo.getApkContentsSigners()[0].toByteArray())),existing=UpdatePolicy.hex(MessageDigest.getInstance("SHA-256").digest(installed.signingInfo.getApkContentsSigners()[0].toByteArray()));
        return UpdatePolicy.CERT.equalsIgnoreCase(existing)&&UpdatePolicy.archive(apk.packageName,apk.getLongVersionCode(),apk.versionName,cert,info.code,info.name)&&apk.getLongVersionCode()>installed.getLongVersionCode()&&apk.applicationInfo.minSdkVersion<=Build.VERSION.SDK_INT;
    }
    private void install(){
        if(closed||installing||!foreground||available==null)return;Info info=available;
        if(!owner.getPackageManager().canRequestPackageInstalls()){
            installReady=false;awaitingPermission=true;ui.beforePrompt();dialog=new AlertDialog.Builder(owner,ThemeUi.alertTheme()).setTitle("允许安装 Fancy Camera 更新").setMessage("请在系统设置中允许本应用安装更新，返回后继续安装已校验的 APK。").setNegativeButton("稍后",(d,w)->awaitingPermission=false).setPositiveButton("前往设置",(d,w)->{try{permissionLaunched=true;owner.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+owner.getPackageName())));}catch(ActivityNotFoundException e){permissionLaunched=false;awaitingPermission=false;}}).create();dialog.setOnCancelListener(d->awaitingPermission=false);dialog.show();return;
        }
        installReady=false;awaitingPermission=false;installing=true;try{worker.execute(()->{boolean ok=false;try{ok=verified(UpdatePolicy.pending(owner.getCacheDir()),info);}catch(Exception ignored){}boolean valid=ok;main.post(()->{if(closed)return;if(!valid){installing=false;return;}if(!foreground||!ui.ready()){installing=false;installReady=true;return;}try{Intent open=new Intent(Intent.ACTION_VIEW).setDataAndType(UpdateFileProvider.uri(owner),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);owner.startActivity(open);}catch(ActivityNotFoundException|SecurityException e){installing=false;/* No unsolicited popup. */}});});}catch(RejectedExecutionException ignored){}
    }
    void close(){closed=true;downloadEpoch++;HttpsURLConnection c=connection;if(c!=null)c.disconnect();worker.shutdownNow();if(dialog!=null)dialog.dismiss();}
}


