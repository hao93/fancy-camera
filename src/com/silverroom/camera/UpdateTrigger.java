package com.silverroom.camera;

/** One automatic check per app foreground opening; external pickers/install settings don't count. */
public final class UpdateTrigger {
    public static final String PREF_AUTO_CHECK="check_updates_on_start";
    public static final boolean DEFAULT_AUTO_CHECK=true;
    private boolean foreground,skipReturn;
    public boolean enter(boolean enabled){return enter()&&enabled;}
    public static boolean allowed(boolean automatic,boolean enabled){return !automatic||enabled;}
    public boolean enter(){if(foreground)return false;foreground=true;if(skipReturn){skipReturn=false;return false;}return true;}
    public void leave(){foreground=false;}
    public void external(){if(foreground)skipReturn=true;}
    public void resume(){if(foreground)skipReturn=false;}
}
