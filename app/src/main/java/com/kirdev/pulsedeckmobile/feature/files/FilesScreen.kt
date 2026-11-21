package com.kirdev.pulsedeckmobile.feature.files

import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.kirdev.pulsedeckmobile.core.ui.ListRow
import com.kirdev.pulsedeckmobile.core.ui.LocalMessages
import com.kirdev.pulsedeckmobile.core.ui.PanelTextField
import com.kirdev.pulsedeckmobile.core.ui.Placeholder
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.remote.FileItem

/** Screen 09: PC folders. Delete = Recycle Bin on the PC, always confirmed. */
@Composable
fun FilesScreen(app: PulseDeskApp) {
    val vm: FilesViewModel = viewModel(factory = viewModelFactory { initializer { FilesViewModel(app.api) } })
    val s by vm.state.collectAsStateWithLifecycle()
    val message by vm.messages.collectAsStateWithLifecycle()
    val connection by app.connection.state.collectAsStateWithLifecycle()
    val snack = LocalMessages.current
    val resolver = LocalContext.current.contentResolver
    // Kept in the ViewModel: the phone may rotate while the system «Save as» window is open.
    var pendingDownload by vm.pendingDownload
    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

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
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp))
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
            // Where we are: the last three folders of the path, the current one as a tonal chip (it gets the room).
            val parts = s.path!!.trimEnd('\\').split('\\').filter { it.isNotBlank() }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                parts.takeLast(3).forEachIndexed { i, part ->
                    val last = i == parts.takeLast(3).lastIndex
                    if (i > 0) Text("›", fontSize = 14.sp, color = Palette.Text2, modifier = Modifier.padding(horizontal = 4.dp))
                    Text(part, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1,
                        color = if (last) Palette.OnTonal else Palette.Text2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(if (last) 3f else 1f, fill = false).clip(RoundedCornerShape(8.dp))
                            .then(if (last) Modifier.background(Palette.Tonal) else Modifier).padding(horizontal = 10.dp, vertical = 6.dp))
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
            connection !is ConnectionState.Connected -> Placeholder("Нет связи с ПК.")
            s.error != null -> Placeholder(s.error!!, action = "Повторить", onAction = vm::reload)
            s.loading && s.items.isEmpty() && s.roots.isEmpty() -> Placeholder("Загрузка…")
            s.path == null -> LazyColumn(Modifier.padding(horizontal = 8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)) {
                items(s.roots, key = { "${it.name}|${it.path}" }) { root ->
                    ListRow(if (root.path.length <= 3) AppIcons.Disk else AppIcons.Folder, root.name, root.path, chevron = true,
                        onClick = { vm.open(root.path) })
                }
                item {
                    Text("Отправленные с телефона файлы попадают в папку загрузок PulseDeck на ПК.", fontSize = 11.sp,
                        color = Palette.Text3, modifier = Modifier.padding(12.dp))
                }
            }
            else -> {
                val shown = if (s.query.isBlank()) s.items else s.items.filter { it.name.contains(s.query, ignoreCase = true) }
                if (shown.isEmpty()) Placeholder(if (s.query.isBlank()) "Папка пуста." else "Ничего не найдено.")
                // Room at the bottom: the last row's menu stays reachable under the «Отправить на ПК» button.
                LazyColumn(Modifier.padding(horizontal = 8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)) {
                    items(shown, key = { it.name }) { item ->
                        FileRow(item,
                            onOpen = { if (item.dir) vm.open(vm.child(item)) },
                            onDownload = { pendingDownload = item; saveTo.launch(item.name) },
                            onRename = { dialog = { InputDialog("Переименовать", item.name, "Сохранить", { vm.rename(item, it) }) { dialog = null } } },
                            onDelete = {
                                dialog = {
                                    ConfirmDialog("Удалить «${item.name}»?", "Файл попадёт в корзину на ПК — его можно будет восстановить.",
                                        "Удалить", { vm.delete(item) }) { dialog = null }
                                }
                            })
                    }
                }
            }
        }
    }
    dialog?.invoke()
    // Extended FAB: the main action of this screen.
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Box(Modifier.fillMaxSize().then(if (landscape) Modifier.navigationBarsPadding() else Modifier).padding(16.dp),
        contentAlignment = Alignment.BottomEnd) {
        if (connection is ConnectionState.Connected) Row(
            Modifier.height(56.dp).clip(RoundedCornerShape(16.dp)).background(Palette.Container)
                .clickable { pick.launch(arrayOf("*/*")) }.padding(start = 16.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Upload, null, tint = Palette.OnContainer, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Text("Отправить на ПК", fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = Palette.OnContainer)
        }
    }
}

@Composable
private fun FileRow(item: FileItem, onOpen: () -> Unit, onDownload: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    ListRow(
        if (item.dir) AppIcons.Folder else AppIcons.File, item.name,
        if (item.dir) "Папка" else Format.bytes(item.size),
        onClick = if (item.dir) onOpen else onDownload,
        trailing = {
            Column {
                IconButtonSmall(AppIcons.Menu, "Действия", size = 36.dp, onClick = { menu = true })
                DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = Palette.Surface2) {
                    if (!item.dir) DropdownMenuItem({ Text("Скачать на телефон", color = Palette.Text) },
                        leadingIcon = { Icon(AppIcons.Download, null, tint = Palette.Text2, modifier = Modifier.size(20.dp)) },
                        onClick = { menu = false; onDownload() })
                    DropdownMenuItem({ Text("Переименовать", color = Palette.Text) },
                        leadingIcon = { Icon(AppIcons.Edit, null, tint = Palette.Text2, modifier = Modifier.size(20.dp)) },
                        onClick = { menu = false; onRename() })
                    DropdownMenuItem({ Text("Удалить", color = Palette.Text) },
                        leadingIcon = { Icon(AppIcons.Trash, null, tint = Palette.Text2, modifier = Modifier.size(20.dp)) },
                        onClick = { menu = false; onDelete() })
                }
            }
        },
    )
}
