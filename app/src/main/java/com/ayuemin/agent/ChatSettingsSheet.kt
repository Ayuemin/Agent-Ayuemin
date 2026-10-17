package com.ayuemin.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Material 3 settings forms, independent from persistence and API layers. */
private data class SettingField(val key:String,val label:String,val default:String,val secret:Boolean=false)
private val contextFields=listOf(
 SettingField("context","Максимальный контекст, токены","8000"),
 SettingField("threshold","Порог сжатия, %","80"),
 SettingField("cycles","Последние циклы","5"),
 SettingField("rag","Максимум фрагментов RAG","5")
)
private val apiFields=listOf(
 SettingField("base","HTTPS API URL","https://api.deepseek.com/v1"),
 SettingField("key","API key","",true),
 SettingField("model","Модель чата","deepseek-chat"),
 SettingField("input_price","Input USD / 1M","0.20"),
 SettingField("output_price","Output USD / 1M","0.60"),
 SettingField("cached_price","Cached USD / 1M","0.02"),
 SettingField("embedding_base","Embeddings URL",""),
 SettingField("embedding_key","Embeddings API key","",true),
 SettingField("embedding_model","Embeddings model","")
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsSheet(section:Int,controller:ChatController,onClose:()->Unit){
 val fields=if(section==0)contextFields else apiFields
 val state=remember(section){fields.associate{it.key to mutableStateOf(controller.getSetting(it.key,it.default))}}
 ModalBottomSheet(onDismissRequest=onClose){
  Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal=24.dp)){
   Text(if(section==0)"Настройки контекста" else "Подключение API и тарифы",style=MaterialTheme.typography.titleLarge)
   Spacer(Modifier.height(12.dp))
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
    fields.forEach{f->
     OutlinedTextField(
      value=state.getValue(f.key).value,
      onValueChange={state.getValue(f.key).value=it},
      label={Text(f.label)},
      singleLine=true,
      visualTransformation=if(f.secret)PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
      modifier=Modifier.fillMaxWidth()
     )
    }
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
    TextButton(onClick=onClose){Text("Отмена")}
    Button(onClick={
     controller.saveSettings(state.mapValues{it.value.value})
     onClose()
    }){Text("Сохранить")}
   }
   Spacer(Modifier.height(24.dp))
  }
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatUsageSheet(usage:ChatUsage,onClose:()->Unit){
 ModalBottomSheet(onDismissRequest=onClose){
  Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text("Статистика чата",style=MaterialTheme.typography.titleLarge)
   listOf(
    "Запросов" to usage.requests.toString(),
    "Входящие" to usage.input.toString(),
    "Исходящие" to usage.output.toString(),
    "Кэшированные входящие" to usage.cached.toString(),
    "Всего" to (usage.input+usage.output).toString(),
    "Оценка стоимости" to "$"+String.format(Locale.US,"%.6f",usage.cost)
   ).forEach{(name,value)-> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(name);Text(value)} }
   Spacer(Modifier.height(20.dp))
  }
 }
}
