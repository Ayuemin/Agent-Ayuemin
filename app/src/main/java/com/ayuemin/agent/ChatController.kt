package com.ayuemin.agent
import android.app.*
import android.content.*
import android.os.*
import android.widget.*
import android.text.InputType
import org.json.*
import java.io.*
import java.util.*
import java.util.concurrent.Executors
/** Kotlin-only coordinator. UI, persistence, providers and retrieval live in separate classes. */
class ChatController(private val activity:Activity,private val refresh:()->Unit){
 private val prefs=activity.getSharedPreferences("settings",Context.MODE_PRIVATE)
 private val db=ChatDatabase(activity).writableDatabase
 private val messages=ChatRepository(db)
 private val usage=UsageRepository(db)
 private val api=ApiClient(prefs)
 private val embedding=EmbeddingClient(prefs)
 private val rag=HybridRag(RetrievalEngine(db),embedding,VectorStore(db),prefs)
 private val context=ContextEngine(prefs,rag)
 private val worker=Executors.newSingleThreadExecutor()
 private var chat=prefs.getString("chat","1").orEmpty()
 var busy=false;private set
 fun tr(ru:String,en:String)=if(Locale.getDefault().language=="ru")ru else en
 fun messages()=messages.messages(chat)
 fun chats()=messages.chats()
 fun contextFraction()=(context.estimate(messages(),messages.summary(chat)).toFloat()/context.maximum()).coerceIn(0f,1f)
 fun contextLabel():String{val used=context.estimate(messages(),messages.summary(chat));val max=context.maximum();return tr("Контекст: ","Context: ")+"$used / $max (${(100f*used/max).toInt()}%)"}
 fun selectChat(id:String){chat=id;prefs.edit().putString("chat",id).apply();refresh()}
 fun newChat()=selectChat(messages.newChat())
 fun send(body:String){
  if(busy||body.isBlank())return
  if(prefs.getString("key","").isNullOrEmpty()){openSection(2);return}
  val target=chat
  messages.add(target,"user",body.trim())
  busy=true;refresh()
  worker.execute{
   try{
    val all=messages.messages(target)
    var summary=messages.summary(target)
    if(context.estimate(all,summary)>=context.maximum()*prefs.getInt("threshold",80)/100){
     val recent=prefs.getInt("cycles",5).coerceAtLeast(1)*2
     val end=(all.size-recent).coerceAtLeast(0)
     if(end>0){
      val previous=messages.summaryUntil(target)
      val old=buildString{
       append(summary)
       all.take(end).filter{it.id>previous}.forEach{append("\n${it.role}: ${it.body}")}
      }
      if(old.isNotBlank()){
       val prompt=JSONArray().put(JSONObject().put("role","system").put("content","Summarize goals, constraints, decisions and TODOs without inventing facts.")).put(JSONObject().put("role","user").put("content",old))
       val result=api.call(prompt,(context.maximum()/10).coerceAtLeast(256))
       usage.record(target,result)
       summary=result.reply
       messages.updateSummary(target,summary,all[end-1].id)
      }
     }
    }
    val start=(all.size-prefs.getInt("cycles",5).coerceAtLeast(1)*2).coerceAtLeast(0)
    if(start>0)rag.index(all,all[start-1].id)
    val result=api.call(context.assemble(target,all,summary),1024)
    usage.record(target,result)
    messages.add(target,"assistant",result.reply)
    activity.runOnUiThread{busy=false;refresh()}
   }catch(e:Exception){activity.runOnUiThread{
    busy=false;refresh()
    AlertDialog.Builder(activity).setTitle(tr("Ошибка API","API error")).setMessage(e.message).setPositiveButton("OK",null).show()
   }}
  }
 }
 fun openSection(which:Int){when(which){0->settings(true);1->stats();2->settings(false)}}
 private fun settings(contextSettings:Boolean){
  val keys=if(contextSettings)arrayOf("context","threshold","cycles","rag") else arrayOf("base","key","model","input_price","output_price","cached_price","embedding_base","embedding_key","embedding_model")
  val labels=if(contextSettings)arrayOf("Рабочий контекст / tokens","Порог сжатия / %","Циклы вопрос-ответ","Фрагменты RAG") else arrayOf("API HTTPS URL","API Key","Chat model","Input USD / 1M","Output USD / 1M","Cached USD / 1M","Embeddings URL","Embeddings API key","Embeddings model")
  val defaults=if(contextSettings)arrayOf("8000","80","5","5") else arrayOf("https://api.deepseek.com/v1","","deepseek-chat","0.20","0.60","0.02","","","")
  val layout=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL;setPadding(24,12,24,12)}
  val controls=keys.indices.map{index->
   layout.addView(TextView(activity).apply{text=labels[index];setPadding(0,12,0,2)})
   EditText(activity).also{field->
    field.setSingleLine(true)
    val value=if(contextSettings)prefs.getInt(keys[index],defaults[index].toInt()).toString() else prefs.getString(keys[index],defaults[index]).orEmpty()
    field.setText(value)
    field.inputType=if(contextSettings)InputType.TYPE_CLASS_NUMBER else if(keys[index].contains("key"))InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
    layout.addView(field)
   }
  }
  val scroll=ScrollView(activity).apply{addView(layout)}
  AlertDialog.Builder(activity).setTitle(if(contextSettings)"Контекст" else "API и тарифы").setView(scroll)
   .setNegativeButton(tr("Отмена","Cancel"),null).setPositiveButton(tr("Сохранить","Save")){_,_->
    prefs.edit().apply{
     keys.indices.forEach{if(contextSettings)putInt(keys[it],controls[it].text.toString().toIntOrNull()?:defaults[it].toInt()) else putString(keys[it],controls[it].text.toString())}
    }.apply()
    refresh()
   }.show()
 }
 private fun stats(){
  val s=usage.stats(chat)
  AlertDialog.Builder(activity).setTitle(tr("Статистика чата","Chat statistics"))
   .setMessage("Requests: ${s.requests}\nInput: ${s.input}\nOutput: ${s.output}\nCached: ${s.cached}\nTotal: ${s.input+s.output}\nEstimated cost: $"+String.format(Locale.US,"%.6f",s.cost))
   .setPositiveButton("OK",null).show()
 }
 private val pick=201
 fun pickFile(){activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";addCategory(Intent.CATEGORY_OPENABLE)},pick)}
 fun onFileResult(request:Int,result:Int,data:Intent?,append:(String)->Unit){
  if(request!=pick||result!=Activity.RESULT_OK||data?.data==null)return
  try{
   val mime=activity.contentResolver.getType(data.data!!)?: ""
   require(mime.startsWith("text/")||mime.contains("json")){"Only text files supported"}
   val bytes=activity.contentResolver.openInputStream(data.data!!)?.use{it.readBytes()}?:return
   require(bytes.size<=200000){"File exceeds 200 KB"}
   append("\n[FILE]\n"+bytes.toString(Charsets.UTF_8)+"\n[/FILE]\n")
  }catch(e:Exception){Toast.makeText(activity,e.message,Toast.LENGTH_LONG).show()}
 }
}
