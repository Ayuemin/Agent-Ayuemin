package com.ayuemin.agent
import android.content.SharedPreferences
import org.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
data class ApiResult(val reply:String,val input:Int,val output:Int,val cached:Int,val cost:Double)
class ApiClient(private val prefs:SharedPreferences){
 fun call(messages:JSONArray,limit:Int):ApiResult{
  val base=(prefs.getString("base","https://api.deepseek.com/v1")?:"").trim().trimEnd('/')
  require(base.startsWith("https://")){"HTTPS base URL required"}
  val url=if(base.endsWith("/chat/completions"))base else "$base/chat/completions"
  val body=JSONObject().put("model",prefs.getString("model","deepseek-chat")).put("messages",messages).put("max_tokens",limit).put("stream",false)
  val connection=URL(url).openConnection() as HttpURLConnection
  connection.requestMethod="POST";connection.connectTimeout=20000;connection.readTimeout=90000;connection.doOutput=true
  connection.setRequestProperty("Content-Type","application/json")
  connection.setRequestProperty("Authorization","Bearer "+(prefs.getString("key","")?:""))
  try{
   connection.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
   val status=connection.responseCode
   val raw=(if(status<400)connection.inputStream else connection.errorStream)?.bufferedReader()?.use{it.readText()}?:throw IOException("Empty API response")
   if(status>=400)throw IOException("HTTP $status: "+raw.take(350))
   val response=JSONObject(raw)
   val message=response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","")
   val usage=response.optJSONObject("usage")
   val input=usage?.optInt("prompt_tokens",0)?:0
   val output=usage?.optInt("completion_tokens",0)?:0
   val cached=usage?.optJSONObject("prompt_tokens_details")?.optInt("cached_tokens",0)?:usage?.optInt("prompt_cache_hit_tokens",0)?:0
   val cost=((input-cached).coerceAtLeast(0)*price("input_price",0.20)+cached*price("cached_price",0.02)+output*price("output_price",0.60))/1000000.0
   return ApiResult(message,input,output,cached,cost)
  }finally{connection.disconnect()}
 }
 private fun price(key:String,default:Double)=prefs.getString(key,"$default")?.toDoubleOrNull()?:default
}
