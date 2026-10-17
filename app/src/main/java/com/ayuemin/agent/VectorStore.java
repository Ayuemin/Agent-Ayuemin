package com.ayuemin.agent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import java.util.*;
/** Versioned SQLite vector storage. No neural network runs locally. */
final class VectorStore {
 static final class Match {final int messageId;final String role,body;final double similarity;Match(int id,String r,String b,double s){messageId=id;role=r;body=b;similarity=s;}}
 private final SQLiteDatabase db;
 VectorStore(SQLiteDatabase db){this.db=db;}
 boolean exists(int id,String model){try(Cursor c=db.rawQuery("SELECT 1 FROM vectors WHERE message_id=? AND model=? LIMIT 1",new String[]{""+id,model})){return c.moveToFirst();}}
 void upsert(int id,String model,double[] vector){
   JSONArray a=new JSONArray();try{for(double x:vector)a.put(x);}catch(org.json.JSONException error){throw new IllegalArgumentException("Invalid embedding values",error);}
   db.execSQL("INSERT OR REPLACE INTO vectors(message_id,model,dimensions,data) VALUES(?,?,?,?)",new Object[]{id,model,vector.length,a.toString()});
 }
 List<Match> search(String chat,int beforeId,String model,double[] query,int count){
   List<Match> matches=new ArrayList<>();double norm=norm(query);
   if(norm==0)return matches;
   try(Cursor c=db.rawQuery("SELECT m.id,m.role,m.body,v.data,v.dimensions FROM vectors v JOIN messages m ON m.id=v.message_id WHERE m.chat=? AND m.id<=? AND v.model=?",
     new String[]{chat,""+beforeId,model})){
     while(c.moveToNext()){
       if(c.getInt(4)!=query.length)continue;
       try{
         JSONArray data=new JSONArray(c.getString(3));double dot=0,length=0;
         for(int i=0;i<query.length;i++){double x=data.getDouble(i);dot+=x*query[i];length+=x*x;}
         if(length==0)continue;
         double similarity=dot/(norm*Math.sqrt(length));
         if(Double.isFinite(similarity))matches.add(new Match(c.getInt(0),c.getString(1),c.getString(2),similarity));
       }catch(Exception ignored){}
     }
   }
   matches.sort((a,b)->Double.compare(b.similarity,a.similarity));
   return new ArrayList<>(matches.subList(0,Math.min(count,matches.size())));
 }
 private double norm(double[] xs){double n=0;for(double x:xs)n+=x*x;return Math.sqrt(n);}
}
