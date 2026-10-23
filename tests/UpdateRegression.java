import com.silverroom.camera.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class UpdateRegression {
    static int count;
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);count++;System.out.println("PASS "+s);}
    public static void main(String[] args)throws Exception{
        check(UpdatePolicy.endpoint().equals("https://api.github.com/repos/hao93/fancy-camera/releases/latest"),"Update checks use the configured repository's stable latest endpoint");
        check(UpdatePolicy.version("1.19.0")&&UpdatePolicy.version("99.10.100")&&!UpdatePolicy.version("1.19.0-beta")&&!UpdatePolicy.version("../x"),"Stable version names cannot become paths or prerelease strings");
        check(UpdatePolicy.apkName("1.19.0").equals("FancyCamera-1.19.0.apk"),"Release and app agree on the exact standard APK asset name");
        check(UpdatePolicy.newer(23,24,false,false,30,36),"A compatible higher version is offered");
        check(!UpdatePolicy.newer(24,24,false,false,30,36)&&!UpdatePolicy.newer(25,24,false,false,30,36),"The current or an older release stays silent");
        check(!UpdatePolicy.newer(23,24,true,false,30,36)&&!UpdatePolicy.newer(23,24,false,true,30,36),"Draft and prerelease builds are never offered");
        check(!UpdatePolicy.newer(23,24,false,false,37,36)&&!UpdatePolicy.newer(0,24,false,false,30,36),"Unsupported SDK and unknown installed versions cannot trigger installation");
        String url="https://github.com/hao93/fancy-camera/releases/download/v1.19.0/FancyCamera-1.19.0.apk";
        check(UpdatePolicy.assetUrl(url,"v1.19.0","FancyCamera-1.19.0.apk"),"The expected release APK address is accepted");
        check(!UpdatePolicy.assetUrl(url.replace("hao93/","other/"),"v1.19.0","FancyCamera-1.19.0.apk"),"Another owner's download address is rejected");
        check(!UpdatePolicy.assetUrl(url.replace("/fancy-camera/","/other/"),"v1.19.0","FancyCamera-1.19.0.apk"),"Another repository cannot supply an update");
        check(!UpdatePolicy.assetUrl(url,"v1.20.0","FancyCamera-1.19.0.apk")&&!UpdatePolicy.assetUrl(url,"v1.19.0","other.apk"),"Asset tag and filename must match the checked release");
        check(!UpdatePolicy.assetUrl(url+"?next=evil","v1.19.0","FancyCamera-1.19.0.apk")&&!UpdatePolicy.assetUrl(url.replace("github.com/","github.com.evil/"),"v1.19.0","FancyCamera-1.19.0.apk"),"Queries and host lookalikes cannot alter an asset request");
        check(!UpdatePolicy.https("http://github.com/x")&&!UpdatePolicy.https("https://user@github.com/x")&&!UpdatePolicy.https("https://github.com:444/x")&&!UpdatePolicy.https("file:///tmp/update.apk"),"Plaintext, credentials, unusual ports and local URLs are rejected");
        check(UpdatePolicy.redirect("https://release-assets.githubusercontent.com/a?signature=test")&&UpdatePolicy.redirect("https://objects.githubusercontent.com/a"),"Official HTTPS release CDN redirects are accepted");
        check(!UpdatePolicy.redirect("https://evil.com/a")&&!UpdatePolicy.redirect("https://githubusercontent.com.evil/a")&&!UpdatePolicy.redirect("https://api.github.com/user"),"Download redirects cannot escape to arbitrary hosts or API resources");
        check(UpdatePolicy.digest(UpdatePolicy.CERT)&&!UpdatePolicy.digest("sha256:"+UpdatePolicy.CERT)&&!UpdatePolicy.digest("0"),"Only complete SHA256 hex values are valid manifest digests");
        check(UpdatePolicy.archive(UpdatePolicy.PACKAGE,24,"1.19.0",UpdatePolicy.CERT,24,"1.19.0"),"Matching package, version and original signer can proceed");
        check(!UpdatePolicy.archive("com.meitu.meiyancamera",24,"1.19.0",UpdatePolicy.CERT,24,"1.19.0"),"A compatibility or unrelated APK cannot replace the standard app");
        check(!UpdatePolicy.archive(UpdatePolicy.PACKAGE,25,"1.19.0",UpdatePolicy.CERT,24,"1.19.0")&&!UpdatePolicy.archive(UpdatePolicy.PACKAGE,24,"1.20.0",UpdatePolicy.CERT,24,"1.19.0"),"An archive must match both metadata version fields exactly");
        check(!UpdatePolicy.archive(UpdatePolicy.PACKAGE,24,"1.19.0",String.join("",Collections.nCopies(64,"0")),24,"1.19.0"),"A different APK signing certificate is rejected");
        check(UpdatePolicy.MAX_APK==209715200,"Downloads are capped at 200MB independently of server metadata");
        UpdateTrigger trigger=new UpdateTrigger();trigger.external();check(trigger.enter(),"Initial launch checks even when first-run permissions are requested");
        check(!trigger.enter(),"Repeated lifecycle callbacks in one opening do not check again");
        trigger.resume();check(!trigger.enter(),"Returning focus without leaving the app does not check");
        trigger.leave();check(trigger.enter(),"Opening the app from the background checks once again");
        trigger.external();trigger.leave();check(!trigger.enter(),"Returning from a photo picker or install permission skips an extra check");
        trigger.resume();trigger.leave();check(trigger.enter(),"A later genuine opening checks after the external return was consumed");
        trigger.external();trigger.resume();trigger.leave();check(trigger.enter(),"An external overlay that never stopped the app cannot suppress the next genuine opening");
        trigger.leave();trigger.enter();check(!trigger.enter(),"Lens switches and internal tab changes need no additional lifecycle check");
        check(UpdateTrigger.DEFAULT_AUTO_CHECK,"Existing installations keep startup checking enabled until the user changes the switch");
        UpdateTrigger disabled=new UpdateTrigger();check(!disabled.enter(false),"A disabled startup switch suppresses the initial automatic request");
        check(!disabled.enter(true),"Turning startup checking on does not initiate a check in the same opening");
        disabled.leave();check(disabled.enter(true),"The next genuine opening checks after the switch is enabled");
        disabled.external();disabled.leave();check(!disabled.enter(false)&&!disabled.enter(true),"Returning from an external activity still consumes its skip when startup checking is disabled");
        disabled.resume();disabled.leave();check(disabled.enter(true),"External return bookkeeping cannot suppress a later enabled opening");
        check(!UpdateTrigger.allowed(true,false)&&UpdateTrigger.allowed(false,false),"Disabling startup checking rejects automatic requests/results while preserving manual checks");
        check(UpdateTrigger.allowed(true,true)&&UpdateTrigger.allowed(false,true),"Enabling startup checking allows both automatic and manual requests");
        File cache=new File(args[0]);cache.mkdirs();File pending=UpdatePolicy.pending(cache);check(pending.getName().equals("pending-update.apk")&&pending.getParentFile().getName().equals("updates"),"The provider exposes a fixed private APK path rather than caller-supplied paths");
        Files.write(pending.toPath(),new byte[]{'a','b','c'});check(UpdatePolicy.sha256(pending).equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),"Downloaded bytes produce the standard SHA256 digest");
        Files.write(pending.toPath(),new byte[]{'a','b','d'});check(!UpdatePolicy.sha256(pending).equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),"A one-byte change is detectable before requesting installation");
        check(UpdatePolicy.hex(new byte[]{0,15,(byte)128,(byte)255}).equals("000f80ff"),"Certificate fingerprint bytes retain leading zeros and unsigned values");
        System.out.println("ALL "+count+" RELEASE UPDATE / TRIGGER / INTEGRITY TESTS PASSED");
    }
}
