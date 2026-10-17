package com.ayuemin.agent;
import android.content.SharedPreferences;
import java.util.*;
/** Hybrid old-message retrieval; FTS fallback always remains available. */
final class HybridRag {
 private final RetrievalEngine lexical;private final EmbeddingClient embeddings;private final VectorStore vectors;
 private final SharedPreferences prefs;
 HybridRag(RetrievalEngine lexical,EmbeddingClient embeddings,VectorStore vectors,SharedPreferences prefs){
  this.lexical=lexical;this.embeddings=embeddings;this.vectors=vectors;this.prefs=prefs;
 }
 void index(List<MainActivity.Msg> all,int upToId){
   if(!embeddings.configured())return;
   int limit=Math.max(1,Math.min(20,prefs.getInt("embedding_batch",4)));
   int done=0;
   for(MainActivity.Msg m:all){
     if(m.id>upToId||vectors.exists(m.id,embeddings.model())||m.body.trim().isEmpty())continue;
     try{vectors.upsert(m.id,embeddings.model(),embeddings.embed(m.body.substring(0,Math.min(6000,m.body.length()))));done++;}
     catch(Exception e){break;} // preserve missing index for a future retry
     if(done>=limit)break;
   }
 }
 String retrieve(String query,List<MainActivity.Msg> all,int start,int k){
   if(k<1||start<1)return "";
   int before=all.get(start-1).id;
   String fts=lexical.retrieve(query,all,start,Math.max(k,8));
   LinkedHashMap<Integer,String> selected=new LinkedHashMap<>();
   if(embeddings.configured()) {
     try {
       List<VectorStore.Match> matches=vectors.search(lexical.owner.chat,before,embeddings.model(),embeddings.embed(query),Math.max(k*2,8));
       for(VectorStore.Match m:matches){
         if(m.similarity>=0.20&&selected.size()<k){
           selected.put(m.messageId,"#"+m.messageId+" "+m.role+": "+m.body.substring(0,Math.min(1300,m.body.length())));
         }
       }
     }catch(Exception ignored){} // recover via FTS, even if embedding API is unavailable
   }
   for(String line:fts.split("\n")){
     if(selected.size()>=k)break;
     if(line.startsWith("#")){
       int space=line.indexOf(' ');
       try{int id=Integer.parseInt(line.substring(1,space));selected.putIfAbsent(id,line);}catch(Exception ignored){}
     }
   }
   StringBuilder answer=new StringBuilder();
   for(String value:selected.values())answer.append(value).append("\n");
   return answer.toString();
 }
}
