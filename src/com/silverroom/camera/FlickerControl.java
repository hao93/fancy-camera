package com.silverroom.camera;

/** Camera2's advertised anti-banding modes; never guesses an unsupported frequency. */
final class FlickerControl {
    static int safe(int choice){return choice>=0&&choice<=2?choice:0;}
    static String label(int choice){return new String[]{"自动","50Hz","60Hz"}[safe(choice)];}
    static int mode(int choice){return new int[]{3,1,2}[safe(choice)];}
    private static boolean has(int[] modes,int value){if(modes!=null)for(int m:modes)if(m==value)return true;return false;}
    static int choose(int choice,int[] sensor,int[] owner){int desired=mode(choice);if(has(sensor,desired)&&has(owner,desired))return desired;return has(sensor,3)&&has(owner,3)?3:-1;}
    static String status(int choice,int actual){String desired=label(choice);return actual<0?desired+" · 当前镜头不支持":actual==mode(choice)?desired+" · 已请求":desired+" · 当前镜头使用自动";}
}
