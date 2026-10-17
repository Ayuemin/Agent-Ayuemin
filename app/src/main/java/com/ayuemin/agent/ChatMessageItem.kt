package com.ayuemin.agent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatMessageItem(item:Msg){
 val user=item.role=="user"
 Row(Modifier.fillMaxWidth(),horizontalArrangement=if(user)Arrangement.End else Arrangement.Start){
  Surface(color=if(user)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
   shape=AyueminDesignTokens.item,modifier=Modifier.fillMaxWidth(if(user)0.88f else 0.96f)){
   SelectionContainer {Text(item.body,Modifier.padding(horizontal=14.dp,vertical=12.dp),
    style=MaterialTheme.typography.bodyLarge)}
  }
 }
}
