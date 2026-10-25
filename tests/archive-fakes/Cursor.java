package android.database;
import org.json.*;
public final class Cursor implements AutoCloseable{
 public static final int FIELD_TYPE_INTEGER=1;final JSONArray rows,columns;int at=-1;
 public Cursor(JSONObject reply){rows=reply.getJSONArray("rows");columns=reply.getJSONArray("columns");}
 public boolean moveToFirst(){at=0;return rows.length()>0;}public boolean moveToNext(){return ++at<rows.length();}
 public String[] getColumnNames(){String[] names=new String[columns.length()];for(int i=0;i<names.length;i++)names[i]=columns.getString(i);return names;}
 public int getColumnIndexOrThrow(String n){for(int i=0;i<columns.length();i++)if(columns.getString(i).equals(n))return i;throw new IllegalArgumentException(n);}
 public Object value(int i){return rows.getJSONArray(at).get(i);}public String getString(int i){return value(i)==JSONObject.NULL?null:String.valueOf(value(i));}
 public int getType(int i){return value(i) instanceof Number?1:3;}public long getLong(int i){return ((Number)value(i)).longValue();}public void close(){}
}
