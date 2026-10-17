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
class ComposeChatActivity : androidx.activity.ComponentActivity() {
    private lateinit var controller: ChatController
    private val showSheet = mutableStateOf(false)
    private val showDrawer = mutableStateOf(false)
    private val showContext = mutableStateOf(false)
    private val messagesState = mutableStateOf<List<Msg>>(emptyList())
    private val titleState = mutableStateOf("")
    private val contextState = mutableFloatStateOf(0f)
    private val contextLabelState = mutableStateOf("")
    private val workingState = mutableStateOf(false)
    private val inputState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = ChatController(this) { updateViewState() }
        updateViewState()
        setContent {
            AyueminTheme {
                ChatScreen()
            }
        }
    }

    private fun updateViewState() {
        val all = controller.messages()
        messagesState.value = all
        titleState.value = all.firstOrNull()?.body?.take(26) ?: controller.tr("Новый чат", "New chat")
        contextState.floatValue = controller.contextFraction()
        contextLabelState.value = controller.contextLabel()
        workingState.value = controller.busy
    }
    private fun t(ru: String, en: String) = controller.tr(ru, en)

    @Composable
    private fun ChatScreen() {
        val currentMessages = messagesState.value
        val currentTitle = titleState.value
        val contextFraction = contextState.floatValue
        val contextLabel = contextLabelState.value
        val pending = workingState.value
        var draft by remember { mutableStateOf(inputState.value) }
        LaunchedEffect(inputState.value) { draft = inputState.value }
        if (showContext.value) {
            ModalBottomSheet(onDismissRequest = { showContext.value = false }) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(t("Контекст чата", "Chat context"), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Text(contextLabel)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { contextFraction }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
        if (showSheet.value) {
            ModalBottomSheet(onDismissRequest = { showSheet.value = false }) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(t("Информация о чате", "Chat information"), style = MaterialTheme.typography.titleLarge)
                    listOf(t("Настройки контекста", "Context settings"),t("Статистика чата", "Chat statistics"),t("Подключение API и тарифы", "API connection and pricing")).forEachIndexed { index, item ->
                        TextButton(onClick = { showSheet.value=false; controller.openSection(index) }, modifier=Modifier.fillMaxWidth()) { Text(item) }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        if (showDrawer.value) {
            ModalBottomSheet(onDismissRequest = { showDrawer.value = false }) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(t("Диалоги", "Chats"), style=MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { showDrawer.value=false; controller.newChat() }) { Text(t("+ Новый чат","+ New chat")) }
                    controller.chats().forEach { chat ->
                        TextButton(onClick={ showDrawer.value=false; controller.selectChat(chat.id) },modifier=Modifier.fillMaxWidth()) { Text(chat.title) }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showDrawer.value = true }) {
                            Icon(Icons.Default.Menu, contentDescription = t("Диалоги", "Chats"))
                        }
                        Text(currentTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).clickable { showContext.value = true }) {
                            CircularProgressIndicator(progress = { contextFraction },
                                modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Text("${(contextFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { showSheet.value = true }) {
                            Icon(Icons.Default.Settings, contentDescription = t("Информация о чате", "Chat information"))
                        }
                    }
                    HorizontalDivider()
                }
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().imePadding().padding(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        IconButton(onClick = { controller.pickFile() }) {
                            Icon(Icons.Default.Add, contentDescription = t("Прикрепить файл", "Attach file"))
                        }
                        OutlinedTextField(
                            value = draft, onValueChange = {
                                draft = it

                            },
                            modifier = Modifier.weight(1f), maxLines = 5,
                            placeholder = { Text(t("Сообщение...", "Message...")) },
                            shape = RoundedCornerShape(20.dp)
                        )
                        IconButton(onClick = {
                            controller.send(draft)
                            draft = ""
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

    @Deprecated("Legacy activity result retained until modern file picker.")
    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        controller.onFileResult(requestCode,resultCode,data) { inputState.value=it }
    }
}
