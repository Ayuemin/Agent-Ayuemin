package com.ayuemin.agent;
import android.content.SharedPreferences;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** OpenAI-compatible Chat Completions client; pricing is an estimate. */
public final class ApiClient {
 final SharedPreferences prefs;
 ApiClient(SharedPreferences prefs){this.prefs=prefs;}
  MainActivity.ApiResult call(JSONArray messages,int limit)throws Exception{
    String base=prefs.getString("base","https://api.deepseek.com/v1").trim().replaceAll("/+$","");
    if(!base.startsWith("https://"))throw new IOException("HTTPS base URL required");
    String endpoint=base.endsWith("/chat/completions")?base:base+"/chat/completions";
    JSONObject payload=new JSONObject().put("model",prefs.getString("model","deepseek-chat")).put("messages",messages).put("max_tokens",limit).put("stream",false);
    HttpURLConnection conn=(HttpURLConnection)new URL(endpoint).openConnection();
    conn.setRequestMethod("POST");conn.setConnectTimeout(20000);conn.setReadTimeout(90000);conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("Authorization","Bearer "+prefs.getString("key",""));
    try {
      try(OutputStream out=conn.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
      int status=conn.getResponseCode();InputStream stream=status<400?conn.getInputStream():conn.getErrorStream();
      if(stream==null)throw new IOException("HTTP "+status);
      ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] part=new byte[8192];int read;while((read=stream.read(part))!=-1)b.write(part,0,read);
      String raw=new String(b.toByteArray(),StandardCharsets.UTF_8);
      if(status>=400)throw new IOException("HTTP "+status+": "+raw.substring(0,Math.min(500,raw.length())));
      JSONObject response=new JSONObject(raw);
      MainActivity.ApiResult result=new MainActivity.ApiResult();
      result.reply=response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","");
      JSONObject usage=response.optJSONObject("usage");
      if(usage!=null){
        result.in=usage.optInt("prompt_tokens",0);result.out=usage.optInt("completion_tokens",0);
        JSONObject detail=usage.optJSONObject("prompt_tokens_details");
        result.cached=detail==null?usage.optInt("prompt_cache_hit_tokens",0):detail.optInt("cached_tokens",0);
      }
      double inputPrice=price("input_price"),outputPrice=price("output_price"),cachedPrice=price("cached_price");
      result.cost=((Math.max(0,result.in-result.cached)*inputPrice)+(result.cached*cachedPrice)+(result.out*outputPrice))/1000000d;
      return result;
    }finally{conn.disconnect();}
  }
  double price(String key){try{return Double.parseDouble(prefs.getString(key,key.equals("cached_price")?"0.02":key.equals("output_price")?"0.60":"0.20"));}catch(Exception e){return 0;}}

}
