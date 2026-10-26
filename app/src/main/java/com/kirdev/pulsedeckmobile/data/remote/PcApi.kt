package com.kirdev.pulsedeckmobile.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/** A refusal from the PC, with its message (Russian, ready to show). [status] 0 = no connection. */
class ApiError(val status: Int, message: String) : Exception(message)

data class Program(val id: String, val name: String)
data class Scenario(val id: String, val name: String, val count: Int)
data class PcWindow(val hwnd: Long, val pid: Int, val title: String)
data class FileRoot(val name: String, val path: String)
data class FileItem(val name: String, val dir: Boolean, val size: Long)
data class Folder(val path: String, val parent: String, val items: List<FileItem>)
data class Transfer(val time: String, val name: String, val incoming: Boolean, val ok: Boolean)
/** [protected]: system process or PulseDeck itself; the PC refuses to end it, so no button. */
data class PcProcess(val pid: Int, val name: String, val cpu: Double, val memory: Long, val protected: Boolean = false)
data class PcEvent(val id: Int, val time: String, val title: String, val text: String)
data class PowerTimer(val action: String, val seconds: Int)

/**
 * HTTP part of API v1 (docs/PULSE_MOBILE_API_V1.md): same host, token and pinned TLS client as
 * the live link, so it works over USB and Wi-Fi alike. Only routes the PC really has.
 */
class PcApi(private val endpoint: StateFlow<Endpoint?>) {
    private val json = "application/json".toMediaType()

    // ---- Power and system ----
    suspend fun lock() = post("system/lock")
    suspend fun sleep() = post("system/sleep")
    suspend fun closeActiveWindow() = post("system/close_active")
    /** Shutdown or restart after [minutes] (0 = in 10 s, the PC's minimum); can be cancelled. */
    suspend fun shutdown(minutes: Int) = post("system/shutdown", JSONObject().put("minutes", minutes))
    suspend fun restart(minutes: Int) = post("system/restart", JSONObject().put("minutes", minutes))
    suspend fun cancelPower() = post("system/cancel")

    /** Pending shutdown/restart, from /status. */
    suspend fun powerTimer(): PowerTimer? =
        get("status").optJSONObject("power")?.let { PowerTimer(it.optString("action"), it.optInt("seconds")) }

    suspend fun setMuted(muted: Boolean) = post("volume", JSONObject().put("muted", muted))

    // ---- Windows, programs, scenarios, web ----
    suspend fun windows(): List<PcWindow> = get("windows").list("windows") {
        PcWindow(it.optLong("hwnd"), it.optInt("pid"), it.optString("title"))
    }
    /** [action]: focus, min, max or close. */
    suspend fun window(hwnd: Long, action: String) = post("windows/$hwnd/$action")
    suspend fun programs(): List<Program> = get("programs").list("programs") { Program(it.optString("id"), it.optString("name")) }
    suspend fun runProgram(id: String) = post("programs/${enc(id)}/run")
    suspend fun scenarios(): List<Scenario> = get("scenarios").list("scenarios") {
        Scenario(it.optString("id"), it.optString("name"), it.optInt("count"))
    }
    suspend fun runScenario(id: String) = post("scenarios/${enc(id)}/run")
    suspend fun openUrl(url: String) = post("open_url", JSONObject().put("url", url))
    suspend fun search(query: String) = post("search", JSONObject().put("query", query))

    // ---- Screen ----
    suspend fun monitors(): Int = get("monitors").optInt("count", 1)
    suspend fun screenshot(monitor: Int, width: Int): ByteArray =
        call(Request.Builder().url(url("screenshot", "monitor" to "$monitor", "width" to "$width")).get()) { it.body!!.bytes() }

    // ---- Processes ----
    suspend fun processes(): List<PcProcess> = get("processes").list("processes") {
        PcProcess(it.optInt("pid"), it.optString("name"), it.optDouble("cpu", 0.0), it.optLong("memory"), it.optBoolean("protected"))
    }
    suspend fun killProcess(pid: Int) = post("processes/$pid/kill")

    // ---- Files ----
    suspend fun roots(): List<FileRoot> = get("files/roots").list("roots") { FileRoot(it.optString("name"), it.optString("path")) }
    suspend fun folder(path: String): Folder {
        val data = get("files/list", "path" to path)
        return Folder(data.optString("path"), data.optString("parent"),
            data.list("items") { FileItem(it.optString("name"), it.optBoolean("dir"), it.optLong("size")) })
    }
    suspend fun mkdir(parent: String, name: String) = post("files/mkdir", JSONObject().put("path", parent).put("name", name))
    suspend fun rename(path: String, name: String) = post("files/rename", JSONObject().put("path", path).put("name", name))
    /** To the Recycle Bin on the PC, never a permanent delete. */
    suspend fun delete(path: String) = post("files/delete", JSONObject().put("path", path))
    suspend fun history(): List<Transfer> = get("history").list("history") {
        Transfer(it.optString("time"), it.optString("name"), it.optString("direction") == "incoming",
            it.optString("status") in setOf("sent", "received"))
    }

    /** Streams a PC file into [out]; [progress] gets (done, total or -1). */
    suspend fun download(path: String, out: OutputStream, progress: (Long, Long) -> Unit) {
        call(Request.Builder().url(url("files/download", "path" to path)).get(), timeoutSeconds = 600) { response ->
            val body = response.body!!
            val total = body.contentLength()
            body.byteStream().use { input -> copy(input, out, total, progress) }
        }
    }

    /** Sends a phone file to the PC's PulseDeck downloads folder (the only place the PC accepts uploads). */
    suspend fun upload(name: String, size: Long, open: () -> InputStream, progress: (Long, Long) -> Unit): String {
        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = size
            override fun writeTo(sink: BufferedSink) {
                open().use { input -> copy(input, sink.outputStream(), size, progress) }
            }
        }
        return call(Request.Builder().url(url("files/upload", "name" to name)).post(body), timeoutSeconds = 600) {
            JSONObject(it.body!!.string()).optString("message")
        }
    }

    // ---- Media sources and events ----
    data class Source(val id: String, val source: String, val title: String)
    suspend fun mediaSources(): List<Source> = get("player/sessions").list("sessions") {
        Source(it.optString("id"), it.optString("source"), it.optString("title"))
    }
    suspend fun events(after: Int): Pair<List<PcEvent>, Int> {
        val data = get("events", "after" to "$after")
        return data.list("events") { PcEvent(it.optInt("id"), it.optString("time"), it.optString("title"), it.optString("text")) } to
            data.optInt("last", after)
    }

    // ---- Plumbing ----
    private fun enc(part: String) = java.net.URLEncoder.encode(part, "UTF-8").replace("+", "%20")

    private fun url(path: String, vararg query: Pair<String, String>): okhttp3.HttpUrl {
        val base = endpoint.value ?: throw ApiError(0, "Нет связи с ПК.")
        return "${base.baseUrl}/api/v1/$path".toHttpUrl().newBuilder()
            .apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
    }

    private suspend fun get(path: String, vararg query: Pair<String, String>): JSONObject =
        call(Request.Builder().url(url(path, *query)).get()) { JSONObject(it.body!!.string()) }

    /** POST returning the PC's «message» (or empty). */
    private suspend fun post(path: String, body: JSONObject = JSONObject()): String =
        call(Request.Builder().url(url(path)).post(body.toString().toRequestBody(json))) {
            JSONObject(it.body!!.string()).optString("message")
        }

    private suspend fun <T> call(builder: Request.Builder, timeoutSeconds: Long = 15, read: (Response) -> T): T =
        withContext(Dispatchers.IO) {
            val base = endpoint.value ?: throw ApiError(0, "Нет связи с ПК.")
            val client = base.client.newBuilder().readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS).build()
            val request = builder.header("Authorization", "Bearer ${base.token}").build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw ApiError(response.code, errorText(response))
                    read(response)
                }
            } catch (e: ApiError) {
                throw e
            } catch (e: java.io.IOException) {
                throw ApiError(0, "Нет связи с ПК.")
            }
        }

    private fun errorText(response: Response): String {
        val text = runCatching { JSONObject(response.body?.string().orEmpty()).optString("error") }.getOrNull()
        return text?.takeIf { it.isNotBlank() } ?: when (response.code) {
            401 -> "Телефон отключён от ПК. Подключите заново."
            403 -> "ПК не разрешает это действие."
            404 -> "ПК не поддерживает эту функцию. Обновите PulseDeck."
            423 -> "ПК заблокирован. Управление недоступно."
            else -> "Ошибка ПК (${response.code})."
        }
    }

    private fun copy(input: InputStream, out: OutputStream, total: Long, progress: (Long, Long) -> Unit) {
        val buffer = ByteArray(64 * 1024)
        var done = 0L
        var reported = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            done += read
            if (done - reported >= 256 * 1024) {      // a few hundred updates per 100 MB, not one per chunk
                reported = done
                progress(done, total)
            }
        }
        out.flush()
        progress(done, total)
    }

    private fun <T> JSONObject.list(key: String, item: (JSONObject) -> T): List<T> {
        val array: JSONArray = optJSONArray(key) ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(item) }
    }
}
