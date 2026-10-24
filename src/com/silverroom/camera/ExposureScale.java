package com.silverroom.camera;

/** Soft -3..3 camera-control scale. Endpoints preserve each lens's previous +/-2EV-limited range. */
public final class ExposureScale {
    private ExposureScale(){}
    public static final float STEP=.1f;
    public static float min(ExposureLimits l){return l.supported()&&l.min()<0?-3:0;}
    public static float max(ExposureLimits l){return l.supported()&&l.max()>0?3:0;}
    public static float snap(float value,ExposureLimits l){return ExposureValue.clamp(Math.max(min(l),Math.min(max(l),Float.isFinite(value)?value:0)));}
    public static int progress(float value,ExposureLimits l){return Math.round((snap(value,l)-min(l))*10);}
    public static int count(ExposureLimits l){return Math.round((max(l)-min(l))*10);}
    public static float value(int progress,ExposureLimits l){return snap(min(l)+Math.max(0,Math.min(count(l),progress))/10f,l);}
    public static final class Plan {
        public final float display,target,hardware,fine;
        private Plan(float d,float t,float h){display=d;target=t;hardware=h;fine=t-h;}
    }
    public static Plan plan(float value,ExposureLimits l){float d=snap(value,l),target=d<0?-d/3*l.min():d/3*l.max();return new Plan(d,target,l.snap(target));}
    public static float previewFine(Plan plan,Float applied){return applied!=null&&Float.isFinite(applied)?plan.target-applied:plan.fine;}
}
