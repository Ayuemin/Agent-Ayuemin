package com.ayuemin.agent

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Compose presentation layer. Existing message database, RAG and API pipeline are
 * delegated to the legacy controller during a staged, non-destructive migration.
 */
class ComposeChatActivity : MainActivity() {
    private val messagesState = mutableStateOf<List<Msg>>(emptyList())
    private val titleState = mutableStateOf("")
    private val contextState = mutableFloatStateOf(0f)
    private val contextLabelState = mutableStateOf("")
    private val workingState = mutableStateOf(false)
    private val inputState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateViewState()
        setContent {
            AyueminTheme {
                ChatScreen()
            }
        }
    }

    override fun render() {
        super.render()
        updateViewState()
    }

    private fun updateViewState() {
        messagesState.value = read().toList()
        titleState.value = title.text.toString()
        contextState.floatValue = (ring.value / 100f).coerceIn(0f, 1f)
        contextLabelState.value = progress.text.toString()
        workingState.value = busy
        inputState.value = input.text.toString()
    }

    @Composable
    private fun ChatScreen() {
        val currentMessages = messagesState.value
        val currentTitle = titleState.value
        val contextFraction = contextState.floatValue
        val contextLabel = contextLabelState.value
        val pending = workingState.value
        var draft by remember { mutableStateOf(inputState.value) }
        LaunchedEffect(inputState.value) { draft = inputState.value }
        Scaffold(
            topBar = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { chatList() }) {
                            Icon(Icons.Default.Menu, contentDescription = t("Диалоги", "Chats"))
                        }
                        Text(currentTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                            CircularProgressIndicator(progress = { contextFraction },
                                modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Text("${(contextFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { chatInfo() }) {
                            Icon(Icons.Default.Settings, contentDescription = t("Информация о чате", "Chat information"))
                        }
                    }
                    Text(contextLabel, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                }
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().imePadding().padding(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(onClick = { pickFile() }) {
                            Icon(Icons.Default.Add, contentDescription = t("Прикрепить файл", "Attach file"))
                        }
                        OutlinedTextField(
                            value = draft, onValueChange = {
                                draft = it
                                input.setText(it)
                            },
                            modifier = Modifier.weight(1f), maxLines = 5,
                            placeholder = { Text(t("Сообщение...", "Message...")) },
                            shape = RoundedCornerShape(20.dp)
                        )
                        IconButton(onClick = {
                            input.setText(draft)
                            send()
                            if (input.text.isEmpty()) draft = ""
                        }, enabled = !pending && draft.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = t("Отправить", "Send"))
                        }
                    }
                }
            }
        ) { padding ->
            if (currentMessages.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(t("Начни новый диалог", "Start a conversation"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentMessages, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (item.role == "user") Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (item.role == "user") MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(0.90f)
                            ) {
                                SelectionContainer {
                                    Text(item.body, modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Legacy document picker callback retained until attachment extraction.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updateViewState()
    }
}
