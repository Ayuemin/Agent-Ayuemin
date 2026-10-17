package com.ayuemin.agent

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(logger: DiagnosticLogger, onClose: () -> Unit, onExport: (Boolean) -> Unit) {
    var verbose by remember { mutableStateOf(logger.verbose()) }
    var confirm by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Диагностика", style = MaterialTheme.typography.titleLarge)
            Text("Записываются коды событий и метаданные без текста сообщений и ключей.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Подробный журнал")
                Switch(checked = verbose, onCheckedChange = { verbose = it; logger.setVerbose(it) })
            }
            Text("Перед передачей файла проверь его содержимое: метаданные также могут быть чувствительными.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onExport(false) }) { Text("Экспорт TXT") }
                OutlinedButton(onClick = { onExport(true) }) { Text("Экспорт JSON") }
            }
            TextButton(onClick = { confirm = true }) { Text("Очистить журнал") }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false },
        title = { Text("Очистить журнал?") },
        text = { Text("Все диагностические события будут удалены.") },
        confirmButton = { TextButton(onClick = { logger.clear(); confirm = false }) { Text("Очистить") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Отмена") } })
}
