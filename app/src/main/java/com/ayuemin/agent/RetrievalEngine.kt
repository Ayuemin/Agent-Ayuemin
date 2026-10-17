package com.ayuemin.agent
import android.database.sqlite.SQLiteDatabase
import java.util.Locale
class RetrievalEngine(private val db:SQLiteDatabase){
 fun retrieve(chat:String,query:String,older:List<Msg>,start:Int,limit:Int):String{
  if(limit<=0||start<=0)return ""
  val terms=query.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]")," ").split(Regex("\\s+")).filter{it.length>=4}.distinct().take(6)
  if(terms.isEmpty())return ""
  val upper=older[start-1].id
  val match=terms.joinToString(" OR "){"\"$it\""}
  val result=StringBuilder()
  try{
   db.rawQuery("SELECT m.id,m.role,m.body FROM search JOIN messages m ON m.id=search.rowid WHERE search MATCH ? AND m.chat=? AND m.id<=? ORDER BY m.id DESC LIMIT ?",arrayOf(match,chat,"$upper","$limit")).use{c->
    while(c.moveToNext())result.append("#").append(c.getInt(0)).append(" ").append(c.getString(1)).append(": ").append(c.getString(2).take(1300)).append("\n")
   }
  }catch(_:Exception){}
  return result.toString()
 }
}
