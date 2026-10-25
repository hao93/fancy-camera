import javax.tools.*;import java.nio.file.*;import java.util.*;import java.net.*;

/** Optional real desktop GLES pixel verification; no Android device or ADB is involved. */
public final class NativePreviewRegression {
 public static void main(String[] args)throws Exception{
  Path root=Paths.get(args[0]).toAbsolutePath().normalize(),classes=root.resolve("build/native-preview-tests/classes"),json=root.getParent().resolve("toolchain/json/json-20240303.jar");Files.createDirectories(classes);
  List<String> compile=new ArrayList<>(Arrays.asList("--release","8","-encoding","UTF-8","-cp",json.toString(),"-d",classes.toString()));
  for(String file:new String[]{"tests/native/android/opengl/GLES20.java","src/com/silverroom/camera/PreviewHold.java","src/com/silverroom/camera/PreviewFramebuffer.java","src/com/silverroom/camera/PreviewLatch.java","src/com/silverroom/camera/LogicalZoomState.java","src/com/silverroom/camera/LensPlan.java","src/com/silverroom/camera/ZoomScale.java","tests/NativePreviewProbe.java"})compile.add(root.resolve(file).toString());
  if(ToolProvider.getSystemJavaCompiler().run(null,null,null,compile.toArray(new String[0]))!=0)throw new AssertionError("Native preview compilation");
  try(URLClassLoader loader=new URLClassLoader(new URL[]{classes.toUri().toURL(),json.toUri().toURL()},null)){try{loader.loadClass("com.silverroom.camera.NativePreviewProbe").getMethod("run",String.class,String.class,String.class).invoke(null,args[1],root.resolve("tests/native/gles_bridge.py").toString(),args[2]);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError("Native preview pixels",e.getCause());}}
 }
}
