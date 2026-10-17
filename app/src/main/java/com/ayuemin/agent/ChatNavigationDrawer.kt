package com.ayuemin.agent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatNavigationDrawer(chats:List<ChatInfo>,onNew:()->Unit,onSelect:(String)->Unit,onClose:()->Unit){
 var query by remember { mutableStateOf("") }
 ModalDrawerSheet {
  Text("Agent-Ayuemin",Modifier.padding(18.dp),style=MaterialTheme.typography.titleLarge)
  OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(horizontal=12.dp),
   placeholder={Text("Поиск по чатам")},singleLine=true,shape=AyueminDesignTokens.field)
  Spacer(Modifier.height(8.dp))
  FilledTonalButton(onClick=onNew,modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp)){
   Text("Новый чат")
  }
  HorizontalDivider(Modifier.padding(vertical=8.dp))
  LazyColumn {
   items(chats.filter{it.title.contains(query,true)}){ chat ->
    NavigationDrawerItem(label={Text(chat.title,maxLines=2)},selected=false,onClick={onSelect(chat.id)},
     modifier=Modifier.padding(horizontal=8.dp))
   }
  }
  TextButton(onClick=onClose,modifier=Modifier.padding(12.dp)){Text("Закрыть")}
 }
}
