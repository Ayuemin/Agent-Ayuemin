package com.ayuemin.agent
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import kotlin.math.sqrt
data class VectorMatch(val id:Int,val role:String,val body:String,val score:Double)
class VectorStore(private val db:SQLiteDatabase){
 fun exists(id:Int,model:String)=db.rawQuery("SELECT 1 FROM vectors WHERE message_id=? AND model=? LIMIT 1",arrayOf("$id",model)).use{it.moveToFirst()}
 fun upsert(id:Int,model:String,v:DoubleArray){
  val a=JSONArray();v.forEach{a.put(it)}
  db.execSQL("INSERT OR REPLACE INTO vectors(message_id,model,dimensions,data) VALUES(?,?,?,?)",arrayOf(id,model,v.size,a.toString()))
 }
 fun search(chat:String,upper:Int,model:String,q:DoubleArray,count:Int):List<VectorMatch>{
  val norm=sqrt(q.sumOf{it*it});if(norm==0.0)return emptyList()
  val out=mutableListOf<VectorMatch>()
  db.rawQuery("SELECT m.id,m.role,m.body,v.data,v.dimensions FROM vectors v JOIN messages m ON m.id=v.message_id WHERE m.chat=? AND m.id<=? AND v.model=?",arrayOf(chat,"$upper",model)).use{c->
   while(c.moveToNext()){
    if(c.getInt(4)!=q.size)continue
    try{
     val a=JSONArray(c.getString(3));var dot=0.0;var length=0.0
     for(i in q.indices){val x=a.getDouble(i);dot+=x*q[i];length+=x*x}
     if(length>0){val score=dot/(norm*sqrt(length));if(score.isFinite())out.add(VectorMatch(c.getInt(0),c.getString(1),c.getString(2),score))}
    }catch(_:Exception){}
   }
  }
  return out.sortedByDescending{it.score}.take(count)
 }
}
