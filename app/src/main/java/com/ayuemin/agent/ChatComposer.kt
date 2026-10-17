package com.ayuemin.agent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatComposer(draft: String, onDraft: (String)->Unit, busy:Boolean, onSend:()->Unit,
    onStop:()->Unit, onAttach:()->Unit, onMic:()->Unit) {
    Surface(shape=RoundedCornerShape(24.dp),tonalElevation=3.dp, modifier=Modifier.fillMaxWidth().padding(8.dp)) {
        Row(Modifier.padding(4.dp),verticalAlignment=Alignment.Bottom) {
            IconButton(onClick=onAttach) { Icon(Icons.Default.Add,contentDescription="Вложение") }
            OutlinedTextField(value=draft,onValueChange=onDraft,modifier=Modifier.weight(1f),maxLines=6,
                placeholder={Text("Сообщение…")},shape=RoundedCornerShape(18.dp))
            IconButton(onClick=onMic) {Icon(Icons.Default.Mic,contentDescription="Диктовка")}
            IconButton(onClick=if(busy)onStop else onSend,enabled=busy||draft.isNotBlank()) {
                if(busy) Icon(Icons.Default.Close,contentDescription="Остановить")
                else Icon(Icons.AutoMirrored.Filled.Send,contentDescription="Отправить")
            }
        }
    }
}
