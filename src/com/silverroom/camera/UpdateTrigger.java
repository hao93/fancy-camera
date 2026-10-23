package com.silverroom.camera;

/** One automatic check per app foreground opening; external pickers/install settings don't count. */
public final class UpdateTrigger {
    private boolean foreground,skipReturn;
    public boolean enter(){if(foreground)return false;foreground=true;if(skipReturn){skipReturn=false;return false;}return true;}
    public void leave(){foreground=false;}
    public void external(){if(foreground)skipReturn=true;}
    public void resume(){if(foreground)skipReturn=false;}
}
