package com.silverroom.camera;
import org.json.*;
import java.text.*;
import java.util.*;

/** Labels always come from this photograph, not the currently selected lens/film. */
public final class PrintMetadata {
    public final String date,focal,film,exifDate;
    public PrintMetadata(String id,JSONObject j){String exif="",day="";long time=j.optLong("captured_at",0);if(time>0){SimpleDateFormat format=new SimpleDateFormat("yyyy:MM:dd HH:mm:ss",Locale.US);format.setTimeZone(new SimpleTimeZone(j.optInt("capture_offset_minutes",0)*60000,"capture"));exif=format.format(new Date(time));day=exif.substring(0,10).replace(':','.');}else{String source=j.optString("source_datetime","");if(validDate(source)){exif=source;day=source.substring(0,10).replace(':','.');}else if(!j.optBoolean("imported",false)&&!j.optString("lens","").contains("导入")&&NegativeStore.valid(id)){String candidate=id.substring(0,4)+":"+id.substring(4,6)+":"+id.substring(6,8)+" "+id.substring(9,11)+":"+id.substring(11,13)+":"+id.substring(13,15);if(validDate(candidate)){exif=candidate;day=candidate.substring(0,10).replace(':','.');}}}date=day;exifDate=exif;JSONObject capture=j.optJSONObject("capture");float mm=capture==null?0:(float)capture.optDouble("mm",0);if(mm<=0){java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(\\d+(?:\\.\\d+)?)mm").matcher(j.optString("lens",""));if(m.find())try{mm=Float.parseFloat(m.group(1));}catch(Exception ignored){}}focal=mm>0&&Float.isFinite(mm)?ZoomScale.label(mm)+"mm":"";int stock=j.optInt("stock",-1);film=FilmCatalog.valid(stock)?FilmCatalog.NAMES[stock]:"";}
    static boolean validDate(String value){if(value==null||!value.matches("\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}"))return false;try{SimpleDateFormat format=new SimpleDateFormat("yyyy:MM:dd HH:mm:ss",Locale.US);format.setLenient(false);format.parse(value);return true;}catch(ParseException e){return false;}}
    public String[] captions(PrintOptions options){if(options.style==0)return new String[0];List<String> lines=new ArrayList<>();if(options.date&&!date.isEmpty())lines.add(date);String detail=(options.focal?focal:"");if(options.film&&!film.isEmpty())detail+=(detail.isEmpty()?"":"  /  ")+film;if(!detail.isEmpty())lines.add(detail);return lines.toArray(new String[0]);}
}
