package com.ayuemin.agent;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
/** Optional independent OpenAI-compatible embedding API. Network calls only on worker threads. */
final class EmbeddingClient {
 private final SharedPreferences prefs;
 EmbeddingClient(SharedPreferences p){prefs=p;}
 boolean configured(){return !prefs.getString("embedding_model","").trim().isEmpty() && !key().isEmpty();}
 String model(){return prefs.getString("embedding_model","").trim();}
 private String key(){String k=prefs.getString("embedding_key","");return k.isEmpty()?prefs.getString("key",""):k;}
 double[] embed(String text)throws Exception{
   String base=prefs.getString("embedding_base","").trim();
   if(base.isEmpty())base=prefs.getString("base","https://api.deepseek.com/v1");
   if(!base.startsWith("https://"))throw new IOException("Embedding endpoint must use HTTPS");
   base=base.replaceAll("/+$","");
   URL url=new URL(base.endsWith("/embeddings")?base:base+"/embeddings");
   JSONObject body=new JSONObject().put("model",model()).put("input",text);
   HttpURLConnection conn=(HttpURLConnection)url.openConnection();
   conn.setRequestMethod("POST");conn.setConnectTimeout(15000);conn.setReadTimeout(40000);conn.setDoOutput(true);
   conn.setRequestProperty("Authorization","Bearer "+key());conn.setRequestProperty("Content-Type","application/json");
   try{
     try(OutputStream output=conn.getOutputStream()){output.write(body.toString().getBytes(StandardCharsets.UTF_8));}
     InputStream stream=conn.getResponseCode()<400?conn.getInputStream():conn.getErrorStream();
     if(stream==null)throw new IOException("No embedding response");
     ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;
     while((n=stream.read(bytes))!=-1){if(buffer.size()+n>2000000)throw new IOException("Embedding response too large");buffer.write(bytes,0,n);}
     String raw=new String(buffer.toByteArray(),StandardCharsets.UTF_8);
     if(conn.getResponseCode()>=400)throw new IOException("Embedding HTTP "+conn.getResponseCode()+": "+raw.substring(0,Math.min(160,raw.length())));
     JSONArray vector=new JSONObject(raw).getJSONArray("data").getJSONObject(0).getJSONArray("embedding");
     if(vector.length()<8||vector.length()>8192)throw new IOException("Invalid embedding dimensions");
     double[] result=new double[vector.length()];
     for(int i=0;i<result.length;i++)result[i]=vector.getDouble(i);
     return result;
   }finally{conn.disconnect();}
 }
}
