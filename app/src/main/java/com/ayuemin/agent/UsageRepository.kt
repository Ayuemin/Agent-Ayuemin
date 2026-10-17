package com.ayuemin.agent
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
data class ChatUsage(val requests:Long,val input:Long,val output:Long,val cached:Long,val cost:Double)
class UsageRepository(private val db:SQLiteDatabase){
 fun record(chat:String,r:ApiResult){
  val v=ContentValues().apply{put("chat",chat);put("input",r.input);put("output",r.output);put("cached",r.cached);put("expense",r.cost)}
  db.insert("usages",null,v)
 }
 fun stats(chat:String):ChatUsage=db.rawQuery("SELECT COUNT(*),COALESCE(SUM(input),0),COALESCE(SUM(output),0),COALESCE(SUM(cached),0),COALESCE(SUM(expense),0) FROM usages WHERE chat=?",arrayOf(chat)).use{
  if(it.moveToFirst())ChatUsage(it.getLong(0),it.getLong(1),it.getLong(2),it.getLong(3),it.getDouble(4)) else ChatUsage(0,0,0,0,0.0)
 }
}
