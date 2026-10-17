package com.ayuemin.agent
import android.content.SharedPreferences
class HybridRag(private val lexical:RetrievalEngine,private val embedding:EmbeddingClient,private val store:VectorStore,private val prefs:SharedPreferences){
 fun index(all:List<Msg>,upper:Int){
  if(!embedding.configured())return
  val batch=prefs.getInt("embedding_batch",4).coerceIn(1,20)
  var count=0
  for(m in all){
   if(m.id>upper||m.body.isBlank()||store.exists(m.id,embedding.model()))continue
   try{store.upsert(m.id,embedding.model(),embedding.embed(m.body.take(6000)));count++}catch(_:Exception){break}
   if(count>=batch)break
  }
 }
 fun retrieve(chat:String,q:String,all:List<Msg>,start:Int,k:Int):String{
  if(start<1||k<1)return ""
  val selected=linkedMapOf<Int,String>()
  if(embedding.configured()){
   try{
    val matches=store.search(chat,all[start-1].id,embedding.model(),embedding.embed(q),maxOf(k*2,8))
    for(m in matches)if(m.score>=0.20&&selected.size<k)selected[m.id]="#${m.id} ${m.role}: ${m.body.take(1300)}"
   }catch(_:Exception){}
  }
  val older=lexical.retrieve(chat,q,all,start,maxOf(k,8))
  older.lineSequence().filter{it.startsWith("#")}.forEach{line->
   val id=line.substringAfter("#").substringBefore(" ").toIntOrNull()
   if(id!=null&&selected.size<k)selected.putIfAbsent(id,line)
  }
  return selected.values.joinToString("\n")
 }
}
