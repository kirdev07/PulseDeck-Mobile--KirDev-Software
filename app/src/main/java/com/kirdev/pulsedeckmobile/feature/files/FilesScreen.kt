package com.kirdev.pulsedeckmobile.feature.files

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kirdev.pulsedeckmobile.core.theme.Tone
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.ui.appear
import com.kirdev.pulsedeckmobile.core.ui.enter
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ConfirmDialog
import com.kirdev.pulsedeckmobile.core.ui.IconButtonSmall
import com.kirdev.pulsedeckmobile.core.ui.InputDialog
import com.kirdev.pulsedeckmobile.core.ui.LocalMessages
import com.kirdev.pulsedeckmobile.core.ui.Placeholder
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.remote.FileItem

/** «Файлы» (approved mockup): drives with how full they are, the path as chips, coloured file types, actions in a sheet. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(app: PulseDeskApp, go: (String) -> Unit) {
    val vm: FilesViewModel = viewModel(factory = viewModelFactory { initializer { FilesViewModel(app.api) } })
    val s by vm.state.collectAsStateWithLifecycle()
    val message by vm.messages.collectAsStateWithLifecycle()
    val connection by app.connection.state.collectAsStateWithLifecycle()
    val metrics by app.connection.metrics.collectAsStateWithLifecycle()
    val snack = LocalMessages.current
    val resolver = LocalContext.current.contentResolver
    // Kept in the ViewModel: the phone may rotate while the system «Save as» window is open.
    var pendingDownload by vm.pendingDownload
    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    var sheetFor by remember { mutableStateOf<FileItem?>(null) }
    val actions = com.kirdev.pulsedeckmobile.core.ui.rememberActions()

    val saveTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val item = pendingDownload
        if (uri != null && item != null) vm.download(item, uri, resolver)
        pendingDownload = null
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.upload(uri, resolver)
    }

    LaunchedEffect(connection is ConnectionState.Connected) { if (connection is ConnectionState.Connected) vm.start() }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.consumed() } }
    BackHandler(enabled = s.path != null) { vm.up() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopTitle("Файлы") {
            if (s.path != null) IconButtonSmall(AppIcons.Plus, "Новая папка", onClick = {
                dialog = { InputDialog("Новая папка", "", "Создать", vm::mkdir) { dialog = null } }
            })
            IconButtonSmall(AppIcons.Refresh, "Обновить", onClick = vm::reload)
        }
        if (s.path != null) {
            // Material 3 search bar: a pill on the high surface.
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
                .background(Palette.Surface2).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButtonSmall(AppIcons.Back, "Вверх", onClick = vm::up)
                androidx.compose.foundation.text.BasicTextField(
                    s.query, vm::search, singleLine = true, modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Palette.Text),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Palette.accent),
                    decorationBox = { field ->
                        if (s.query.isEmpty()) Text("Поиск в папке", fontSize = 16.sp, color = Palette.Text2)
                        field()
                    },
                )
                Icon(AppIcons.Search, null, tint = Palette.Text2, modifier = Modifier.padding(end = 12.dp).size(22.dp))
            }
            // Where we are: the last three folders as chips, the current one tonal (it gets the room).
            val parts = s.path!!.trimEnd('\\').split('\\').filter { it.isNotBlank() }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val shown = parts.takeLast(3)
                shown.forEachIndexed { i, part ->
                    val last = i == shown.lastIndex
                    if (i > 0) Text("›", fontSize = 14.sp, color = Palette.Text2, modifier = Modifier.padding(horizontal = 4.dp))
                    Text(part, fontSize = 13.sp, fontWeight = if (last) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1,
                        color = if (last) Palette.OnTonal else Palette.Text,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(if (last) 3f else 1f, fill = false).clip(RoundedCornerShape(10.dp))
                            .background(if (last) Palette.Tonal else Palette.Surface2).padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
        s.transfer?.let { (label, fraction) ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(label + if (fraction >= 0) " · ${(fraction * 100).toInt()}%" else "", fontSize = 12.sp, color = Palette.Text2)
                if (fraction >= 0) LinearProgressIndicator({ fraction }, Modifier.fillMaxWidth(), color = Palette.accent, trackColor = Palette.Track)
                else LinearProgressIndicator(Modifier.fillMaxWidth(), color = Palette.accent, trackColor = Palette.Track)
            }
        }
        when {
            connection !is ConnectionState.Connected -> { val connState = connection; androidx.compose.foundation.layout.Box(Modifier.verticalScroll(rememberScrollState())) { com.kirdev.pulsedeckmobile.core.ui.NoConnection(connState, onRetry = { app.connection.restart() },
                onChoose = { go(if (connState == com.kirdev.pulsedeckmobile.data.model.ConnectionState.NotPaired) com.kirdev.pulsedeckmobile.navigation.Routes.PAIR else com.kirdev.pulsedeckmobile.navigation.Routes.DEVICES) }) } }
            s.error != null -> Placeholder(s.error!!, action = "Повторить", onAction = vm::reload)
            s.loading && s.items.isEmpty() && s.roots.isEmpty() -> Placeholder("Загрузка…")
            s.path == null -> LazyColumn(Modifier.padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                val drives = s.roots.filter { it.path.length <= 3 }
                val folders = s.roots - drives.toSet()
                if (drives.isNotEmpty()) item {
                    val few = drives.size <= 2
                    Row(Modifier.fillMaxWidth().then(if (few) Modifier else Modifier.horizontalScroll(rememberScrollState()))
                        .padding(horizontal = 8.dp, vertical = 4.dp).enter(0), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        drives.forEachIndexed { i, root ->
                            val disk = metrics?.disks?.firstOrNull { it.name.trimEnd('\\').equals(root.path.trimEnd('\\'), true) }
                            DriveCard(root.name.ifBlank { root.path }, disk, first = i == 0,
                                modifier = if (few) Modifier.weight(1f) else Modifier.width(160.dp)) { vm.open(root.path) }
                        }
                    }
                }
                if (folders.isNotEmpty()) item { SectionTitle("Папки", Modifier.padding(horizontal = 8.dp).enter(1)) }
                itemsIndexed(folders, key = { _, it -> "${it.name}|${it.path}" }) { i, root ->
                    TypedRow(AppIcons.Folder, Tone.Violet, root.name, root.path, Modifier.enter(i + 2), onMore = null) { vm.open(root.path) }
                }
                item {
                    Text("Отправленные с телефона файлы попадают в папку загрузок PulseDeck на ПК.", fontSize = 11.sp,
                        color = Palette.Text3, modifier = Modifier.padding(12.dp))
                }
            }
            else -> {
                val shown = if (s.query.isBlank()) s.items else s.items.filter { it.name.contains(s.query, ignoreCase = true) }
                if (shown.isEmpty()) Placeholder(if (s.query.isBlank()) "Папка пуста." else "Ничего не найдено.")
                else Text("${shown.size} ${plural(shown.size)}", fontSize = 12.sp, color = Palette.Text2,
                    modifier = Modifier.padding(start = 20.dp, top = 2.dp, bottom = 4.dp))
                // Room at the bottom: the last row stays reachable under the «Отправить на ПК» button.
                LazyColumn(Modifier.padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                    itemsIndexed(shown, key = { _, it -> it.name }) { i, item ->
                        val (icon, tone) = kind(item)
                        TypedRow(icon, tone, item.name, if (item.dir) "Папка" else Format.bytes(item.size), Modifier.enter(i.coerceAtMost(8)),
                            onMore = { sheetFor = item }) { if (item.dir) vm.open(vm.child(item)) else sheetFor = item }
                    }
                }
            }
        }
    }
    sheetFor?.let { item ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { sheetFor = null }, containerColor = Palette.Surface,
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Palette.Outline) }) {
            val full = vm.child(item)
            val onPc: (suspend () -> String?) -> Unit = { block ->
                actions.run {
                    try { block() } catch (e: com.kirdev.pulsedeckmobile.data.remote.ApiError) {
                        if (e.status == 404) "PulseDeck на ПК не умеет это делать. Обновите PulseDeck." else throw e
                    }
                }
            }
            FileSheet(item, s.path.orEmpty(),
                onDownload = { sheetFor = null; pendingDownload = item; saveTo.launch(item.name) },
                onOpen = { sheetFor = null; vm.open(full) },
                onOpenOnPc = {
                    sheetFor = null
                    val run = { onPc { app.api.openFile(full) } }
                    if (runs(item)) dialog = {
                        ConfirmDialog("Запустить «${item.name}» на ПК?", "Это программа или скрипт: она запустится на ПК от имени вашей учётной записи.",
                            "Запустить", run, danger = false) { dialog = null }
                    } else run()
                },
                onReveal = { sheetFor = null; onPc { app.api.revealFile(full) } },
                onInfo = {
                    sheetFor = null
                    actions.run {
                        val info = try { app.api.fileInfo(full) } catch (e: com.kirdev.pulsedeckmobile.data.remote.ApiError) {
                            if (e.status == 404) return@run "PulseDeck на ПК не умеет показывать свойства. Обновите PulseDeck." else throw e
                        }
                        dialog = { InfoDialog(info) { dialog = null } }
                        null
                    }
                },
                onRename = { sheetFor = null; dialog = { InputDialog("Переименовать", item.name, "Сохранить", { vm.rename(item, it) }) { dialog = null } } },
                onDelete = {
                    sheetFor = null
                    dialog = {
                        ConfirmDialog("Удалить «${item.name}»?", "Файл попадёт в корзину на ПК — его можно будет восстановить.",
                            "Удалить", { vm.delete(item) }) { dialog = null }
                    }
                })
        }
    }
    dialog?.invoke()
    // Extended FAB: the main action of this screen; it pops in.
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Box(Modifier.fillMaxSize().then(if (landscape) Modifier.navigationBarsPadding() else Modifier).padding(16.dp),
        contentAlignment = Alignment.BottomEnd) {
        if (connection is ConnectionState.Connected) {
            val pop = appear(450, 450)
            Row(
                Modifier.height(56.dp).graphicsLayer { scaleX = 0.6f + 0.4f * pop; scaleY = 0.6f + 0.4f * pop; alpha = pop }
                    .clip(RoundedCornerShape(16.dp)).background(Palette.Container)
                    .clickable { pick.launch(arrayOf("*/*")) }.padding(start = 16.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Upload, null, tint = Palette.OnContainer, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(10.dp))
                Text("Отправить на ПК", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.OnContainer)
            }
        }
    }
}

private fun plural(n: Int): String {
    val d = n % 10
    val dd = n % 100
    return when {
        d == 1 && dd != 11 -> "объект"
        d in 2..4 && dd !in 12..14 -> "объекта"
        else -> "объектов"
    }
}

/** Icon and colour by what the file is. */
private fun kind(item: FileItem): Pair<ImageVector, Tone> {
    if (item.dir) return AppIcons.Folder to Tone.Violet
    return when (item.name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg", "ico" -> AppIcons.Image to Tone.Teal
        "mp4", "mkv", "avi", "mov", "webm", "wmv" -> AppIcons.Play to Tone.Pink
        "mp3", "flac", "wav", "ogg", "m4a", "aac", "opus" -> AppIcons.Music to Tone.Pink
        "doc", "docx", "pdf", "txt", "rtf", "odt", "xls", "xlsx", "csv", "ppt", "pptx", "md", "log" -> AppIcons.File to Tone.Blue
        "zip", "rar", "7z", "tar", "gz", "iso" -> AppIcons.Disk to Tone.Amber
        "exe", "msi", "apk", "bat", "cmd", "lnk" -> AppIcons.Apps to Tone.Violet
        else -> AppIcons.File to Tone.Violet
    }
}

@Composable
private fun TypedRow(icon: ImageVector, tone: Tone, title: String, sub: String, modifier: Modifier, onMore: (() -> Unit)?, onClick: () -> Unit) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(tone.bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tone.fg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 12.sp, color = Palette.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onMore != null) IconButtonSmall(AppIcons.Menu, "Действия", size = 40.dp, onClick = onMore)
        else Icon(AppIcons.Chevron, null, tint = Palette.Text3, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DriveCard(name: String, disk: com.kirdev.pulsedeckmobile.data.model.Disk?, first: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (first) Palette.Container else Palette.Surface
    val fg = if (first) Palette.OnContainer else Palette.Text
    val bar = if (first) Palette.accent else Palette.accent
    val track = if (first) Palette.onAccent.copy(alpha = 0.35f) else Palette.Track
    val grow = appear(350)
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name.trimEnd('\\'), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (disk != null) Text("${(disk.usedFraction * 100).toInt()}%", fontSize = 12.sp, color = fg.copy(alpha = 0.8f))
        }
        Canvas(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(6.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            val f = (disk?.usedFraction ?: 0f) * grow
            if (f > 0f) drawRoundRect(bar, size = Size((size.width * f).coerceAtLeast(size.height), size.height), cornerRadius = r)
        }
        Text(disk?.let { "свободно ${Format.bytes(it.free)} из ${Format.bytes(it.total)}" } ?: "открыть", fontSize = 12.sp,
            color = fg.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Programs and scripts: opening them on the PC runs them, so the phone asks first. */
private fun runs(item: FileItem): Boolean = !item.dir &&
    item.name.substringAfterLast('.', "").lowercase() in setOf("exe", "msi", "bat", "cmd", "ps1", "vbs", "js", "lnk", "scr", "com", "reg")

/** What can be done with one file or folder (approved «Файл · действия» mockup). */
@Composable
private fun FileSheet(
    item: FileItem, folder: String, onDownload: () -> Unit, onOpen: () -> Unit, onOpenOnPc: () -> Unit, onReveal: () -> Unit,
    onInfo: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
) {
    val (icon, tone) = kind(item)
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
        Row(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(tone.bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tone.fg, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontSize = 17.sp, color = Palette.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text((if (item.dir) "Папка" else Format.bytes(item.size)) + " · " + folder.trimEnd('\\'), fontSize = 13.sp, color = Palette.Text2,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.dir) {
                BigAction(AppIcons.Folder, "Открыть здесь", true, Modifier.weight(1f), onOpen)
                BigAction(AppIcons.Monitor, "Открыть на ПК", false, Modifier.weight(1f), onOpenOnPc)
            } else {
                BigAction(AppIcons.Monitor, if (runs(item)) "Запустить на ПК" else "Открыть на ПК", true, Modifier.weight(1f), onOpenOnPc)
                BigAction(AppIcons.Download, "На телефон", false, Modifier.weight(1f), onDownload)
            }
            BigAction(AppIcons.Folder, "В папке", false, Modifier.weight(1f), onReveal)
        }
        Spacer(Modifier.height(8.dp))
        SheetRow(AppIcons.Edit, "Переименовать", Palette.Text, onRename)
        SheetRow(AppIcons.Info, "Свойства", Palette.Text, onInfo)
        SheetRow(AppIcons.Trash, "Удалить в корзину ПК", Palette.Error, onDelete)
    }
}

@Composable
private fun BigAction(icon: ImageVector, label: String, strong: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (strong) Palette.Container else Palette.Surface2
    val fg = if (strong) Palette.OnContainer else Palette.Text
    Column(modifier.height(84.dp).clip(RoundedCornerShape(20.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** «Свойства»: what the PC says about the file, nothing guessed. */
@Composable
private fun InfoDialog(info: com.kirdev.pulsedeckmobile.data.remote.FileInfo, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss, containerColor = Palette.Surface2, shape = RoundedCornerShape(28.dp),
        title = { Text(info.name, color = Palette.Text, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoLine("Тип", if (info.dir) "Папка" else info.name.substringAfterLast('.', "").uppercase().ifBlank { "Файл" })
                if (info.dir) InfoLine("Внутри", when (val n = info.items) { null, -1 -> Format.UNKNOWN; else -> if (n >= 10000) "больше 10 000" else "$n" })
                else InfoLine("Размер", Format.bytes(info.size))
                InfoLine("Изменён", info.modified.ifBlank { Format.UNKNOWN })
                InfoLine("Создан", info.created.ifBlank { Format.UNKNOWN })
                InfoLine("Путь", info.path)
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Готово", color = Palette.accent, fontWeight = FontWeight.SemiBold) } },
    )
}

@Composable
private fun InfoLine(title: String, value: String) {
    Column {
        Text(title, fontSize = 12.sp, color = Palette.Text2)
        Text(value, fontSize = 14.sp, color = Palette.Text)
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = color)
    }
}
