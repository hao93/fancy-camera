package android.database.sqlite;
import android.content.Context;
public abstract class SQLiteOpenHelper {
 private SQLiteDatabase database;private final Context context;private final String name;
 public SQLiteOpenHelper(Context c,String n,Object factory,int version){context=c;name=n;}
 public SQLiteDatabase getWritableDatabase(){if(database==null){database=new SQLiteDatabase(new java.io.File(context.path,name).toString());onCreate(database);}return database;}
 public SQLiteDatabase getReadableDatabase(){return getWritableDatabase();}public void close(){if(database!=null){database.close();database=null;}}
 public abstract void onCreate(SQLiteDatabase db);public abstract void onUpgrade(SQLiteDatabase db,int a,int b);
}
