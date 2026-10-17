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
 * delegated to independent Kotlin repositories and services.
 */
class ComposeChatActivity : androidx.activity.ComponentActivity() {
    private lateinit var controller: ChatController
    private val showSheet = mutableStateOf(false)
    private val showDiagnostics = mutableStateOf(false)
    private val showMicInfo = mutableStateOf(false)
    private var exportJson = false
    private val exportCode = 202
    private val showDrawer = mutableStateOf(false)
    private val showContext = mutableStateOf(false)
    private val section = mutableIntStateOf(-1)
    private val messagesState = mutableStateOf<List<Msg>>(emptyList())
    private val titleState = mutableStateOf("")
    private val contextState = mutableFloatStateOf(0f)
    private val contextLabelState = mutableStateOf("")
    private val workingState = mutableStateOf(false)
    private val errorState = mutableStateOf<String?>(null)
    private val inputState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = ChatController(this) { updateViewState() }
        updateViewState()
        setContent {
            AyueminTheme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { ChatScreen() }
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
        errorState.value = controller.lastError
    }
    private fun t(ru: String, en: String) = controller.tr(ru, en)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ChatScreen() {
        val currentMessages = messagesState.value
        val currentTitle = titleState.value
        val contextFraction = contextState.floatValue
        val contextLabel = contextLabelState.value
        val pending = workingState.value
        var draft by remember { mutableStateOf(inputState.value) }
        LaunchedEffect(inputState.value) { draft = inputState.value }
        if (showDiagnostics.value) DiagnosticsScreen(controller.diagnostics, {showDiagnostics.value=false}) { json ->
            exportJson=json
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type=if(json)"application/json" else "text/plain"
                putExtra(Intent.EXTRA_TITLE,if(json)"ayuemin-diagnostics.json" else "ayuemin-diagnostics.txt")
            },exportCode)
        }
        if(showMicInfo.value) AlertDialog(onDismissRequest={showMicInfo.value=false},title={Text("Диктовка")},text={Text("Офлайн-движок ещё не установлен. Модель Vosk будет добавлена отдельным этапом.")},confirmButton={TextButton(onClick={showMicInfo.value=false}){Text("OK")}})
        if (section.intValue==0 || section.intValue==2) ChatSettingsSheet(section.intValue,controller){section.intValue=-1}
        if (section.intValue==1) ChatUsageSheet(controller.statistics()){section.intValue=-1}
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
        var menuExpanded by remember { mutableStateOf(false) }
        ModalNavigationDrawer(
            drawerState = rememberDrawerState(if(showDrawer.value) DrawerValue.Open else DrawerValue.Closed),
            gesturesEnabled = showDrawer.value,
            drawerContent = {
                ModalDrawerSheet {
                    Text(t("Диалоги","Chats"),modifier=Modifier.padding(20.dp),style=MaterialTheme.typography.titleLarge)
                    NavigationDrawerItem(label={Text(t("Новый чат","New chat"))},selected=false,onClick={controller.newChat();showDrawer.value=false})
                    controller.chats().forEach { item ->
                        NavigationDrawerItem(label={Text(item.title)},selected=false,onClick={controller.selectChat(item.id);showDrawer.value=false})
                    }
                    TextButton(onClick={showDrawer.value=false}){Text(t("Закрыть","Close"))}
                }
            }
        ) {
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
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.Settings, contentDescription = t("Информация о чате", "Chat information"))
                            }
                            DropdownMenu(expanded=menuExpanded,onDismissRequest={menuExpanded=false}) {
                                listOf(t("Настройки контекста","Context settings"),t("Статистика","Statistics"),t("Подключение API","API settings"),t("Диагностика","Diagnostics")).forEachIndexed { index,label ->
                                    DropdownMenuItem(text={Text(label)},onClick={
                                        menuExpanded=false
                                        if(index==3) showDiagnostics.value=true else section.intValue=index
                                    })
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
            },
            bottomBar = {
                Column(Modifier.imePadding()) {
                    errorState.value?.let { err ->
                        Text("Ошибка: $err", color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(horizontal=16.dp))
                        TextButton(onClick={controller.retry()}) {Text(t("Повторить","Retry"))}
                    }
                    ChatComposer(draft=draft,onDraft={draft=it},busy=pending,
                        onSend={
                            if(controller.configured()) {controller.send(draft);draft=""}
                            else section.intValue=2
                        }, onStop={controller.stop()},onAttach={controller.pickFile()},onMic={showMicInfo.value=true})
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
    }

    @Deprecated("Legacy activity result retained until modern file picker.")
    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==exportCode && resultCode==RESULT_OK && data?.data!=null) {
            val text=if(exportJson) controller.diagnostics.exportJsonLines() else controller.diagnostics.exportTxt()
            runCatching {contentResolver.openOutputStream(data.data!!)?.bufferedWriter()?.use{it.write(text)} }
        } else controller.onFileResult(requestCode,resultCode,data) { inputState.value=it }
    }
}
