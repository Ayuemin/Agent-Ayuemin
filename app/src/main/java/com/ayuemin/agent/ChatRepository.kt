package com.ayuemin.agent
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import java.util.UUID
data class Msg(val id:Int,val role:String,val body:String)
data class ChatItem(val id:String,val title:String)
class ChatRepository(private val db:SQLiteDatabase){
 fun messages(chat:String):List<Msg>{
  val items=mutableListOf<Msg>()
  db.rawQuery("SELECT id,role,body FROM messages WHERE chat=? ORDER BY id",arrayOf(chat)).use{c->while(c.moveToNext())items.add(Msg(c.getInt(0),c.getString(1),c.getString(2)))}
  return items
 }
 fun add(chat:String,role:String,body:String){
  val values=ContentValues().apply{put("chat",chat);put("role",role);put("body",body);put("time",System.currentTimeMillis())}
  db.insert("messages",null,values)
 }
 fun chats():List<ChatItem>{
  val list=mutableListOf<ChatItem>()
  db.rawQuery("SELECT chat,MIN(id) FROM messages GROUP BY chat ORDER BY MIN(id) DESC",null).use{c->while(c.moveToNext()){
   val id=c.getString(0)
   val text=messages(id).firstOrNull()?.body.orEmpty().take(45)
   list.add(ChatItem(id,text))
  }}
  return list
 }
 fun newChat()=UUID.randomUUID().toString()
 fun summary(chat:String):String=db.rawQuery("SELECT text FROM summaries WHERE chat=?",arrayOf(chat)).use{if(it.moveToFirst())it.getString(0) else ""}
 fun summaryUntil(chat:String)=db.rawQuery("SELECT until_id FROM summaries WHERE chat=?",arrayOf(chat)).use{if(it.moveToFirst())it.getInt(0) else 0}
 fun updateSummary(chat:String,summary:String,until:Int){db.execSQL("INSERT OR REPLACE INTO summaries(chat,text,until_id) VALUES(?,?,?)",arrayOf(chat,summary,until))}
}
