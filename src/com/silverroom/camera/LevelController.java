package com.silverroom.camera;
import android.content.Context;
import android.hardware.*;
import android.os.SystemClock;

final class LevelController implements SensorEventListener {
    private final MainActivity a;private final SensorManager manager;private final Sensor sensor;private boolean listening,seeded,aligned;private final float[] gravity=new float[3];private long last;
    LevelController(MainActivity owner){a=owner;manager=(SensorManager)a.getSystemService(Context.SENSOR_SERVICE);Sensor chosen=manager==null?null:manager.getDefaultSensor(Sensor.TYPE_GRAVITY);if(chosen==null&&manager!=null)chosen=manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);sensor=chosen;}
    boolean available(){return sensor!=null;}
    void update(){boolean needed=a.resumed&&a.page==0&&(a.activePhotoViewer==null||!a.activePhotoViewer.isShowing())&&a.composition.on&&a.composition.level&&sensor!=null;if(needed&&!listening){seeded=aligned=false;last=0;listening=manager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI);}else if(!needed&&listening){manager.unregisterListener(this);listening=false;}if(!needed&&a.guides!=null){a.guides.level=null;a.guides.invalidate();}}
    void stop(){if(listening)manager.unregisterListener(this);listening=false;seeded=aligned=false;if(a.guides!=null){a.guides.level=null;a.guides.invalidate();}}
    @Override public void onSensorChanged(SensorEvent event){if(!listening||!a.resumed||a.page!=0||event.values.length<3)return;for(int i=0;i<3;i++)if(!Float.isFinite(event.values[i]))return;for(int i=0;i<3;i++)gravity[i]=seeded?gravity[i]*.8f+event.values[i]*.2f:event.values[i];seeded=true;long now=SystemClock.elapsedRealtime();if(now-last<50)return;last=now;LevelMath.Reading reading=LevelMath.read(gravity[0],gravity[1],gravity[2],aligned);aligned=reading.aligned;a.guides.level=reading;a.guides.invalidate();}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
}
