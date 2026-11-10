@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.klark231.mhguqe

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

const val AUTHOR = "Klark231"

@Composable
fun EditorApp(vm: AppViewModel) {
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) { vm.message?.let { snack.showSnackbar(it); vm.message = null } }

    CompositionLocalProvider(LocalRev provides vm.rev, LocalQuestLang provides vm.questLang, LocalIcons provides { i -> vm.game.icon(i) }) {
        // item, monster, skill and map names follow the same language as the quest text
        val gameLang = appLang(null)
        SideEffect { vm.game.lang = gameLang }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val d = vm.doc
            when {
                vm.screen == Screen.SETTINGS -> {
                    BackHandler { vm.screen = if (d != null) Screen.EDITOR else Screen.HOME }
                    SettingsScreen(vm, snack)
                }
                vm.screen == Screen.EDITOR && d != null -> DocScreen(vm, d, snack)
                else -> HomeScreen(vm, snack)
            }
            if (vm.busy) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            vm.error?.let { e ->
                AlertDialog(
                    onDismissRequest = { vm.error = null },
                    icon = { Icon(Icons.Default.Error, null) },
                    title = { Text(stringResource(R.string.error)) },
                    text = { Text(e) },
                    confirmButton = { TextButton(onClick = { vm.error = null }) { Text(stringResource(R.string.close)) } },
                )
            }
        }
    }
}

@Composable
fun rememberOpener(onPick: (Uri) -> Unit): () -> Unit {
    val l = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) onPick(u) }
    return { l.launch(arrayOf("*/*")) }
}

@Composable
fun rememberCreator(onPick: (Uri) -> Unit): (String) -> Unit {
    val l = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { u -> if (u != null) onPick(u) }
    return { name -> l.launch(name) }
}

// home

private val LIST_LABELS = mapOf(
    "vq" to (R.string.list_vq to R.string.list_vq_d),
    "gq" to (R.string.list_gq to R.string.list_gq_d),
    "gq2" to (R.string.list_gq2 to R.string.list_gq2_d),
    "tq" to (R.string.list_tq to R.string.list_tq_d),
)

@Composable
fun HomeScreen(vm: AppViewModel, snack: SnackbarHostState) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val open = rememberOpener { vm.open(it) }
    val pickGame = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u -> if (u != null) vm.scanGame(u) }
    var langDialog by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var list by rememberSaveable { mutableStateOf("") }  // "" = all
    val lang = appLang(null)
    val g = vm.game
    val shown = remember(vm.quests, query, list, lang) {
        val t = query.trim()
        vm.quests.filter { q ->
            (list.isEmpty() || q.list == list) &&
                (t.isEmpty() || q.name(lang).contains(t, ignoreCase = true) || "%07d".format(q.id).contains(t) || q.names.any { it.contains(t, ignoreCase = true) })
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) }, scrollBehavior = scroll,
                actions = {
                    IconButton(onClick = { langDialog = true }) { Icon(Icons.Default.Translate, stringResource(R.string.quest_lang)) }
                    IconButton(onClick = { vm.screen = Screen.SETTINGS }) { Icon(Icons.Default.Settings, stringResource(R.string.settings)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = open, icon = { Icon(Icons.Default.FileOpen, null) }, text = { Text(stringResource(R.string.open_file)) })
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            vm.doc?.let { d ->
                item {
                    Card(
                        Modifier.fillMaxWidth().clickable { vm.screen = Screen.EDITOR },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Edit, null)
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.continue_editing), style = MaterialTheme.typography.titleMedium)
                                Text(d.name + if (vm.dirty) " · " + stringResource(R.string.unsaved) else "", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            if (vm.folderName == null) {
                item { FolderSetupCard { pickGame.launch(null) } }
                if (vm.recent.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.recent)) }
                    item { RecentCard(vm) }
                }
            } else {
                item {
                    OutlinedTextField(
                        query, { query = it }, Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.search_quests)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, stringResource(R.string.clear)) } },
                        singleLine = true,
                    )
                }
                item {
                    val keys = listOf("") + QuestIndex.LISTS.filter { k -> vm.quests.any { it.list == k } }
                    val labels = keys.map { k ->
                        val n = if (k.isEmpty()) vm.quests.size else vm.quests.count { it.list == k }
                        (if (k.isEmpty()) stringResource(R.string.list_all) else stringResource(LIST_LABELS.getValue(k).first)) + " $n"
                    }
                    ChipBar(labels, keys.indexOf(list).coerceAtLeast(0), { list = keys[it] })
                }
                if (list.isNotEmpty()) item { Hint(stringResource(LIST_LABELS.getValue(list).second), Modifier.padding(horizontal = 4.dp)) }
                if (shown.isEmpty()) item { Hint(stringResource(R.string.no_matches), Modifier.padding(4.dp)) }
                items(shown, key = { it.id }) { q -> QuestRow(vm, q, lang) }
            }
        }
    }
    if (langDialog) QuestLangDialog(vm) { langDialog = false }
}

@Composable
fun QuestLangDialog(vm: AppViewModel, close: () -> Unit) {
    val opts = listOf(Opt(-1, stringResource(R.string.quest_lang_auto))) + Lang.names.mapIndexed { i, n -> Opt(i, n) }
    PickerDialog(stringResource(R.string.quest_lang), opts, vm.questLang, { vm.changeQuestLang(it); close() }, close)
}

@Composable
private fun QuestRow(vm: AppViewModel, q: QuestInfo, lang: Int) {
    val g = vm.game
    val level = g.levels.firstOrNull { it.value == q.level }?.label?.substringAfter(": ") ?: ""
    val where = listOfNotNull(level.ifEmpty { null }, if (q.map > 0) g.mapName(q.map % 100) else null, "q%07d".format(q.id)).joinToString(" · ")
    val missing = q.file.isEmpty()
    val notFound = stringResource(R.string.quest_not_found, "q%07d.arc".format(q.id))
    ListItem(
        modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable {
            if (missing) vm.message = notFound else vm.open(Uri.parse(q.file))
        },
        leadingContent = {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                if (q.icon > 0) SmallIcon(q.icon - 1, 40) else Icon(Icons.Default.Description, null)
            }
        },
        headlineContent = { Text(q.name(lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(where, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = { if (missing) Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.outline) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    )
}

@Composable
private fun FolderSetupCard(pick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.FolderOpen, null, Modifier.size(36.dp))
            Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.game_folder_hint), style = MaterialTheme.typography.bodyMedium)
            for (k in QuestIndex.LISTS) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(LIST_LABELS.getValue(k).second), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(onClick = pick) { Text(stringResource(R.string.pick_game_folder)) }
            Hint(stringResource(R.string.open_hint))
        }
    }
}

@Composable
private fun RecentCard(vm: AppViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(vertical = 4.dp)) {
            vm.recent.forEach { (uri, name) ->
                ListItem(
                    modifier = Modifier.clickable { vm.open(Uri.parse(uri)) },
                    leadingContent = { Icon(if (QuestArc.isQuestArcName(name)) Icons.Default.Description else Icons.Default.Inventory2, null) },
                    headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingContent = { IconButton(onClick = { vm.forget(uri) }) { Icon(Icons.Default.Close, stringResource(R.string.remove)) } },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
fun QuestFolderCard(vm: AppViewModel) {
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u -> if (u != null) vm.scanFolder(u) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.quest_folder), style = MaterialTheme.typography.titleMedium)
                    Text(
                        vm.folderName?.let { it + " · " + stringResource(R.string.quests_found, vm.quests.size) } ?: stringResource(R.string.not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (vm.folderName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = { pickFolder.launch(null) }) {
                    Text(stringResource(if (vm.folderName == null) R.string.select else R.string.change))
                }
            }
            Hint(stringResource(R.string.quest_folder_hint))
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.auto_update), style = MaterialTheme.typography.titleMedium)
                    Hint(stringResource(R.string.auto_update_hint))
                }
                Switch(vm.autoUpdate && vm.folderName != null, { vm.changeAutoUpdate(it) }, enabled = vm.folderName != null)
            }
        }
    }
}

// settings

@Composable
private fun GameFolderCard(vm: AppViewModel) {
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u -> if (u != null) vm.scanGame(u) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (vm.folderName != null) stringResource(R.string.msg_scan_done, vm.quests.size, vm.mapCount) else stringResource(R.string.not_set),
                style = MaterialTheme.typography.titleMedium,
            )
            Hint(stringResource(R.string.game_folder_hint))
            Button(onClick = { pick.launch(null) }) { Text(stringResource(R.string.pick_game_folder)) }
        }
    }
}

// tags with a values-* folder, names in their own language
private val UI_LANGS = listOf(
    "en" to "English", "ca" to "Català", "da" to "Dansk", "de" to "Deutsch", "el" to "Ελληνικά", "es" to "Español",
    "fi" to "Suomi", "fr" to "Français", "hu" to "Magyar", "id" to "Bahasa Indonesia", "it" to "Italiano", "ja" to "日本語",
    "ko" to "한국어", "lt" to "Lietuvių", "nb" to "Norsk bokmål", "nl" to "Nederlands", "pl" to "Polski", "pt-BR" to "Português (Brasil)",
    "ro" to "Română", "ru" to "Русский", "sv" to "Svenska", "tr" to "Türkçe", "uk" to "Українська", "vi" to "Tiếng Việt",
    "zh-CN" to "简体中文", "zh-TW" to "繁體中文",
)

@Composable
private fun LanguageCard(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var ui by remember { mutableStateOf(false) }
    var quest by remember { mutableStateOf(false) }
    val current = UiLang.get(ctx)
    val uiName = UI_LANGS.firstOrNull { it.first == current }?.second ?: stringResource(R.string.ui_lang_system)
    val questName = if (vm.questLang in Lang.names.indices) Lang.names[vm.questLang] else stringResource(R.string.quest_lang_auto)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(vertical = 4.dp)) {
            ListItem(
                modifier = Modifier.clickable { ui = true },
                leadingContent = { Icon(Icons.Default.Language, null) },
                headlineContent = { Text(stringResource(R.string.ui_lang)) },
                supportingContent = { Text(uiName) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            ListItem(
                modifier = Modifier.clickable { quest = true },
                leadingContent = { Icon(Icons.Default.Translate, null) },
                headlineContent = { Text(stringResource(R.string.quest_lang)) },
                supportingContent = { Text(questName + "\n" + stringResource(R.string.quest_lang_hint)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
    if (ui) {
        val opts = listOf(Opt(-1, stringResource(R.string.ui_lang_system))) + UI_LANGS.mapIndexed { i, (_, n) -> Opt(i, n) }
        PickerDialog(stringResource(R.string.ui_lang), opts, UI_LANGS.indexOfFirst { it.first == current }, { i ->
            ui = false
            UiLang.set(ctx, UI_LANGS.getOrNull(i)?.first ?: "")
        }, { ui = false })
    }
    if (quest) QuestLangDialog(vm) { quest = false }
}

@Composable
fun MapImagesCard(vm: AppViewModel) {
    val zip = rememberOpener { vm.importStage(it, tree = false) }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { u -> if (u != null) vm.importStage(u, tree = true) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (vm.mapCount > 0) stringResource(R.string.map_images_loaded, vm.mapCount) + (vm.stageName?.let { " · $it" } ?: "")
                else stringResource(R.string.map_images_none),
                style = MaterialTheme.typography.titleMedium,
            )
            Hint(stringResource(R.string.map_images_hint))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { folder.launch(null) }) { Text(stringResource(R.string.pick_folder)) }
                OutlinedButton(onClick = zip) { Text(stringResource(R.string.pick_zip)) }
                if (vm.mapCount > 0) TextButton(onClick = { vm.clearMaps() }) { Text(stringResource(R.string.clear)) }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, snack: SnackbarHostState) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { vm.screen = if (vm.doc != null) Screen.EDITOR else Screen.HOME }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel(stringResource(R.string.game_folder))
            GameFolderCard(vm)
            SectionLabel(stringResource(R.string.languages))
            LanguageCard(vm)
            SectionLabel(stringResource(R.string.manual_setup))
            QuestFolderCard(vm)
            if (vm.recent.isNotEmpty()) {
                SectionLabel(stringResource(R.string.recent))
                RecentCard(vm)
            }
            MapImagesCard(vm)
            SectionLabel(stringResource(R.string.about))
            Text(stringResource(R.string.app_name) + " " + appVersion(), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.about_author, AUTHOR),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Hint(stringResource(R.string.about_credits))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun appVersion(): String {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }
}

// editor shell

@Composable
fun DocScreen(vm: AppViewModel, d: Doc, snack: SnackbarHostState) {
    var askClose by remember { mutableStateOf(false) }
    var askRestore by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val saveAs = rememberCreator { vm.save(it) }
    val leave = { if (vm.dirty) askClose = true else vm.close() }
    BackHandler(onBack = leave)

    val tabs = when (d) {
        is Doc.Quest -> QuestTab.entries
        is Doc.Data -> QuestTab.entries.filter { it.dataOnly }
        is Doc.Arena -> emptyList()
    }
    var tab by rememberSaveable(d) { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            val title = (d as? Doc.Quest)?.q?.let { q -> tick(); q.text[appLang(q)]?.entries?.firstOrNull()?.takeIf { it.isNotBlank() } }
                            Text(title ?: d.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (title != null) Text(d.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (vm.dirty) Text(stringResource(R.string.unsaved), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = { IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                    actions = {
                        IconButton(onClick = { vm.save() }) { Icon(Icons.Default.Save, stringResource(R.string.save)) }
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_as)) }, leadingIcon = { Icon(Icons.Default.SaveAs, null) },
                                onClick = { menu = false; saveAs(vm.suggestedName()) },
                            )
                            if (d is Doc.Quest) DropdownMenuItem(
                                text = { Text(stringResource(R.string.update_list)) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                                enabled = vm.folderName != null,
                                onClick = { menu = false; vm.pushToQuestList() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.backup)) }, leadingIcon = { Icon(Icons.Default.Backup, null) },
                                onClick = { menu = false; vm.backup() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.restore)) }, leadingIcon = { Icon(Icons.Default.Restore, null) },
                                enabled = vm.backupTime != null,
                                onClick = { menu = false; askRestore = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings)) }, leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { menu = false; vm.screen = Screen.SETTINGS },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.close_file)) }, leadingIcon = { Icon(Icons.Default.Close, null) },
                                onClick = { menu = false; leave() },
                            )
                        }
                    },
                )
                if (tabs.isNotEmpty()) ScrollableTabRow(selectedTabIndex = tab.coerceIn(0, tabs.lastIndex), edgePadding = 8.dp) {
                    tabs.forEachIndexed { i, t ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t.title)) })
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (d) {
                is Doc.Quest -> QuestPage(vm, d.q.data, d.q, tabs[tab.coerceIn(0, tabs.lastIndex)])
                is Doc.Data -> QuestPage(vm, d.data, null, tabs[tab.coerceIn(0, tabs.lastIndex)])
                is Doc.Arena -> ArenaPage(vm, d.ac)
            }
        }
    }

    if (askRestore) ConfirmDialog(
        stringResource(R.string.restore_title), stringResource(R.string.restore_body, d.name, vm.backupDate()), stringResource(R.string.restore),
        { askRestore = false; vm.restore() }, { askRestore = false },
    )
    if (askClose) ConfirmDialog(
        stringResource(R.string.discard_title), stringResource(R.string.discard_body), stringResource(R.string.discard),
        { vm.close() }, { askClose = false },
    )
}
