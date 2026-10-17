package com.ayuemin.agent
import android.content.SharedPreferences
import org.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
class EmbeddingClient(private val prefs:SharedPreferences){
 fun model()=prefs.getString("embedding_model","")?.trim().orEmpty()
 private fun key()=prefs.getString("embedding_key","").orEmpty().ifEmpty{prefs.getString("key","").orEmpty()}
 fun configured()=model().isNotEmpty()&&key().isNotEmpty()
 fun embed(text:String):DoubleArray{
  val base=prefs.getString("embedding_base","").orEmpty().ifEmpty{prefs.getString("base","https://api.deepseek.com/v1").orEmpty()}.trimEnd('/')
  require(base.startsWith("https://")){"Embeddings require HTTPS"}
  val url=if(base.endsWith("/embeddings"))base else "$base/embeddings"
  val connection=URL(url).openConnection() as HttpURLConnection
  connection.requestMethod="POST";connection.connectTimeout=15000;connection.readTimeout=40000;connection.doOutput=true
  connection.setRequestProperty("Authorization","Bearer "+key());connection.setRequestProperty("Content-Type","application/json")
  try{
   val request=JSONObject().put("model",model()).put("input",text)
   connection.outputStream.use{it.write(request.toString().toByteArray(Charsets.UTF_8))}
   val raw=(if(connection.responseCode<400)connection.inputStream else connection.errorStream)?.bufferedReader()?.use{it.readText()}?:throw IOException("No embeddings response")
   if(connection.responseCode>=400)throw IOException("Embedding HTTP ${connection.responseCode}: "+raw.take(200))
   val data=JSONObject(raw).getJSONArray("data").getJSONObject(0).getJSONArray("embedding")
   require(data.length() in 8..8192){"Invalid embeddings dimensions"}
   return DoubleArray(data.length()){data.getDouble(it)}
  }finally{connection.disconnect()}
 }
}
