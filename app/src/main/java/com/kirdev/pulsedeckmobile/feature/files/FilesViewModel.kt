package com.kirdev.pulsedeckmobile.feature.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kirdev.pulsedeckmobile.data.remote.FileItem
import com.kirdev.pulsedeckmobile.data.remote.FileRoot
import com.kirdev.pulsedeckmobile.data.remote.PcApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FilesState(
    val roots: List<FileRoot> = emptyList(),
    /** null: the list of drives and folders the PC offers. */
    val path: String? = null,
    val parent: String = "",
    val items: List<FileItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    /** «Скачивание file.zip», 0..1 or -1 when the size is unknown. */
    val transfer: Pair<String, Float>? = null,
)

class FilesViewModel(private val api: PcApi) : ViewModel() {
    private val _state = MutableStateFlow(FilesState())
    val state: StateFlow<FilesState> = _state.asStateFlow()
    private val _messages = MutableStateFlow<String?>(null)
    val messages: StateFlow<String?> = _messages.asStateFlow()
    private var load: Job? = null

    fun consumed() { _messages.value = null }

    fun start() {
        if (_state.value.roots.isEmpty() && _state.value.path == null) roots()
    }

    fun roots() = run {
        _state.update { it.copy(path = null, items = emptyList(), query = "") }
        _state.update { it.copy(roots = api.roots()) }
    }

    fun open(path: String) = run { show(path) }

    fun up() {
        val s = _state.value
        if (s.parent.isNotBlank() && s.roots.none { sameFolder(it.path, s.path) }) open(s.parent) else roots()
    }

    fun reload() { _state.value.path?.let(::open) ?: roots() }

    fun search(query: String) = _state.update { it.copy(query = query) }

    fun mkdir(name: String) = act("Папка создана") { api.mkdir(_state.value.path!!, name) }
    fun rename(item: FileItem, name: String) = act("Переименовано") { api.rename(child(item), name) }
    fun delete(item: FileItem) = act(null) { api.delete(child(item)) }

    fun child(item: FileItem): String {
        val dir = _state.value.path.orEmpty()
        return if (dir.endsWith("\\")) dir + item.name else "$dir\\${item.name}"
    }

    /** PC → phone, into the document the user picked (Storage Access Framework, no storage permission). */
    fun download(item: FileItem, target: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            _state.update { it.copy(transfer = "Скачивание ${item.name}" to -1f) }
            _messages.value = try {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(target)?.use { out ->
                        api.download(child(item), out) { done, total -> progress("Скачивание ${item.name}", done, total) }
                    } ?: error("Не удалось создать файл на телефоне.")
                }
                "Сохранено на телефоне: ${item.name}"
            } catch (e: Exception) {
                runCatching { android.provider.DocumentsContract.deleteDocument(resolver, target) }   // no half-written file
                e.message ?: "Ошибка скачивания."
            }
            _state.update { it.copy(transfer = null) }
        }
    }

    /** Phone → PC. The PC stores uploads in its PulseDeck downloads folder only. */
    fun upload(source: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            val (name, size) = withContext(Dispatchers.IO) { describe(resolver, source) }
            _state.update { it.copy(transfer = "Отправка $name" to -1f) }
            _messages.value = try {
                api.upload(name, size, { resolver.openInputStream(source) ?: error("Файл недоступен.") }) { done, total ->
                    progress("Отправка $name", done, total)
                }.ifBlank { "Отправлено на ПК: $name" }
            } catch (e: Exception) {
                e.message ?: "Ошибка отправки."
            }
            _state.update { it.copy(transfer = null) }
        }
    }

    private fun progress(label: String, done: Long, total: Long) =
        _state.update { it.copy(transfer = label to if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else -1f) }

    private fun describe(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) return (c.getString(0) ?: "file") to (if (c.isNull(1)) -1L else c.getLong(1))
        }
        return "file" to -1L
    }

    private suspend fun show(path: String) {
        val folder = api.folder(path)
        _state.update { it.copy(path = folder.path, parent = folder.parent, items = folder.items, query = "") }
    }

    private fun run(block: suspend () -> Unit) {
        load?.cancel()
        load = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Ошибка.") }
            }
            _state.update { it.copy(loading = false) }
        }
    }

    private fun act(done: String?, block: suspend () -> String) {
        viewModelScope.launch {
            _messages.value = try {
                val answer = block()
                _state.value.path?.let { show(it) }
                answer.ifBlank { done }
            } catch (e: Exception) {
                e.message ?: "Ошибка."
            }
        }
    }

    private fun sameFolder(a: String, b: String?) = a.trimEnd('\\').equals(b?.trimEnd('\\'), ignoreCase = true)
}
