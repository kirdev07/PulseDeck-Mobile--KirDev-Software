package com.kirdev.pulsedeckmobile.feature.files

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
    var pendingDownload by remember { mutableStateOf<FileItem?>(null) }
    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val saveTo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
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
            IconButtonSmall(AppIcons.Upload, "Отправить на ПК", onClick = { pick.launch(arrayOf("*/*")) })
            if (s.path != null) IconButtonSmall(AppIcons.Plus, "Новая папка", onClick = {
                dialog = { InputDialog("Новая папка", "", "Создать", vm::mkdir) { dialog = null } }
            })
            IconButtonSmall(AppIcons.Refresh, "Обновить", onClick = vm::reload)
        }
        if (s.path != null) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButtonSmall(AppIcons.Back, "Вверх", onClick = vm::up)
                Text(s.path!!, fontSize = 12.sp, color = Palette.Text2, maxLines = 1, modifier = Modifier.weight(1f))
            }
            PanelTextField(s.query, vm::search, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), placeholder = "Поиск в этой папке")
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
            s.path == null -> LazyColumn(Modifier.padding(horizontal = 8.dp)) {
                items(s.roots, key = { it.path }) { root ->
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
                LazyColumn(Modifier.padding(horizontal = 8.dp)) {
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
