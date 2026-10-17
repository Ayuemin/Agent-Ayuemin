package com.ayuemin.agent;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;
/** Lexical retrieval baseline; semantic vector search is a planned addition. */
public final class RetrievalEngine {
 final SQLiteDatabase db; final MainActivity owner;
 RetrievalEngine(SQLiteDatabase db,MainActivity owner){this.db=db;this.owner=owner;}
  String retrieve(String q,List<MainActivity.Msg> all,int boundary,int k) {
    if(k==0||boundary<=0)return "";
    // local FTS: tokenize plain words; semantic embeddings will be a separate upgrade
    String clean=q.replaceAll("[^\\p{L}\\p{N} ]"," ").trim().toLowerCase(Locale.ROOT);
    String[] words=clean.split("\\s+"); LinkedHashSet<String> terms=new LinkedHashSet<>();
    for(String w:words)if(w.length()>=4&&terms.size()<6)terms.add("\""+w.replace("\"","")+"\"");
    if(terms.isEmpty())return "";
    String match=String.join(" OR ",terms);int upper=all.get(boundary-1).id;
    StringBuilder out=new StringBuilder();
    try(Cursor c=db.rawQuery("SELECT m.id,m.role,m.body FROM search JOIN messages m ON m.id=search.rowid WHERE search MATCH ? AND m.chat=? AND m.id<=? ORDER BY m.id DESC LIMIT ?",new String[]{match,owner.chat,String.valueOf(upper),String.valueOf(k)})){
      while(c.moveToNext())out.append("#").append(c.getInt(0)).append(" ").append(c.getString(1)).append(": ").append(c.getString(2),0,Math.min(1300,c.getString(2).length())).append("\n");
    }catch(Exception ignored){}
    return out.toString();
  }

}
