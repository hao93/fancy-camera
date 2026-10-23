package com.silverroom.camera;

import android.opengl.Matrix;

/** Geometry applied after the camera's SurfaceTexture coordinate transform. */
final class PreviewTransform {
    private PreviewTransform() {}

    // Raw YUV planes have no SurfaceTexture transform. Correct these exactly once.
    static void yuvModel(float[] out,int sensorDegrees,int displayDegrees,boolean front,
                         int bufferWidth,int bufferHeight,int viewWidth,int viewHeight){
        YuvGeometry.model(out,sensorDegrees,displayDegrees,front,bufferWidth,bufferHeight,viewWidth,viewHeight);
    }

    static void model(float[] out, int sensorDegrees, int displayDegrees,
                      int bufferWidth, int bufferHeight, int viewWidth, int viewHeight) {
        int relative = (sensorDegrees - displayDegrees + 360) % 360;
        float imageAspect = relative % 180 == 0
                ? bufferWidth / (float) bufferHeight : bufferHeight / (float) bufferWidth;
        float viewAspect = viewWidth / (float) viewHeight;
        Matrix.setIdentityM(out, 0);
        Matrix.scaleM(out, 0, imageAspect > viewAspect ? imageAspect / viewAspect : 1,
                imageAspect < viewAspect ? viewAspect / imageAspect : 1, 1);
        // SurfaceTexture.getTransformMatrix() already includes Camera2's sensor
        // rotation and automatic front-camera mirror. Reapplying either here
        // double-transforms the image. Only the display's rotation is additional.
        Matrix.rotateM(out, 0, displayDegrees, 0, 0, 1);
    }
}
