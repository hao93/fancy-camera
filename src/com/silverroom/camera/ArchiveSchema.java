package com.silverroom.camera;

/** Additive archive schema. Existing negative files remain the canonical photo source. */
public final class ArchiveSchema {
    private ArchiveSchema(){}
    public static final String[] CREATE={
        "CREATE TABLE IF NOT EXISTS rolls(id TEXT PRIMARY KEY,name TEXT NOT NULL,kind TEXT NOT NULL,status INTEGER NOT NULL DEFAULT 0,pinned INTEGER NOT NULL DEFAULT 0,rank INTEGER NOT NULL DEFAULT 0,created INTEGER NOT NULL,data TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS items(id TEXT PRIMARY KEY,roll_id TEXT NOT NULL,kind TEXT NOT NULL,created INTEGER NOT NULL,exposure_count INTEGER NOT NULL DEFAULT 1,data TEXT NOT NULL)",
        "CREATE INDEX IF NOT EXISTS items_roll ON items(roll_id,created)",
        "CREATE TABLE IF NOT EXISTS equipment(id TEXT PRIMARY KEY,kind TEXT NOT NULL,name TEXT NOT NULL,data TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS film_profiles(id TEXT PRIMARY KEY,name TEXT NOT NULL,data TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS flags(id TEXT PRIMARY KEY,value TEXT NOT NULL)"
    };
    public static final String[] STATUSES={"拍摄中","已拍完","冲洗中","已完成"};
    public static long photoTime(String id,long fallback){try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS",java.util.Locale.US);f.setLenient(false);return id!=null&&id.matches("\\d{8}_\\d{6}_\\d{3}")?f.parse(id).getTime():fallback;}catch(Exception ignored){return fallback;}}
    public static String status(int n){return STATUSES[Math.max(0,Math.min(3,n))];}
    public static String csv(String text){String value=text==null?"":text;if(value.matches("^[=+@\\-].*"))value="'"+value;return "\""+value.replace("\"","\"\"")+"\"";}
}
