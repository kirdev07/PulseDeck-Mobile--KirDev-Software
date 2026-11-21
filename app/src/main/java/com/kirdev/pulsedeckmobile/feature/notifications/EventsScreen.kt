package com.kirdev.pulsedeckmobile.feature.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.theme.Tone
import com.kirdev.pulsedeckmobile.core.ui.ChoiceChip
import com.kirdev.pulsedeckmobile.core.ui.enter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.Placeholder
import com.kirdev.pulsedeckmobile.core.ui.PollWhileVisible
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.data.remote.PcApi
import com.kirdev.pulsedeckmobile.data.remote.PcEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** PC events (lock/unlock, resource alerts). The PC keeps the last 100 in memory; we keep what we saw. */
class EventsFeed(private val api: PcApi) {
    private val _events = MutableStateFlow<List<PcEvent>>(emptyList())
    val events: StateFlow<List<PcEvent>> = _events.asStateFlow()
    private var last = -1
    private var source: Any? = null

    /** Asks only for events newer than the last one seen; starts over for another PC or a restarted one. */
    suspend fun refresh(pc: Any? = null) {
        if (pc != null && pc != source) {
            source = pc
            last = -1
            _events.value = emptyList()
        }
        var (fresh, newest) = api.events(last)
        if (newest < last) {                   // the PC program restarted: its numbers begin again at 1
            last = -1
            val again = api.events(last)
            fresh = again.first
            newest = again.second
        }
        if (fresh.isNotEmpty()) _events.value = (fresh.reversed() + _events.value).distinctBy { it.id }.take(100)
        last = newest
    }

    companion object { const val POLL_MS = 10_000L }
}

private enum class Kind(val title: String) { ALL("Все"), LOAD("Нагрузка"), LOCK("Блокировка") }

private fun kindOf(e: PcEvent): Kind {
    val t = (e.title + " " + e.text).lowercase()
    return when {
        "заблокир" in t || "разблокир" in t -> Kind.LOCK
        "%" in t || "мало" in t || "нагруз" in t || "порог" in t -> Kind.LOAD
        else -> Kind.ALL
    }
}

/** «События ПК» (approved mockup): a timeline with coloured badges and filters. */
@Composable
fun EventsScreen(app: PulseDeskApp, onBack: () -> Unit) {
    val events by app.events.events.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(Kind.ALL) }
    PollWhileVisible(EventsFeed.POLL_MS) { app.events.refresh((app.connection.state.value as? com.kirdev.pulsedeckmobile.data.model.ConnectionState.Connected)?.pc?.id) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("События ПК", onBack)
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Kind.entries.forEach { k -> ChoiceChip(k.title, k == filter) { filter = k } }
        }
        val shown = if (filter == Kind.ALL) events else events.filter { kindOf(it) == filter }
        if (shown.isEmpty()) Placeholder(if (events.isEmpty()) "Событий пока нет. Здесь появятся блокировка ПК и предупреждения о нагрузке."
            else "Таких событий нет.")
        LazyColumn(Modifier.padding(horizontal = 16.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 110.dp)) {
            itemsIndexed(shown, key = { _, it -> it.id }) { i, e -> EventRow(e, last = i == shown.lastIndex, modifier = Modifier.enter(i.coerceAtMost(8))) }
        }
    }
}

@Composable
fun EventRow(event: PcEvent, last: Boolean = true, modifier: Modifier = Modifier) {
    val (icon, tone) = when (kindOf(event)) {
        Kind.LOCK -> AppIcons.Lock to Tone.Violet
        Kind.LOAD -> AppIcons.Bell to Tone.Amber
        Kind.ALL -> AppIcons.Info to Tone.Teal
    }
    val title = if (event.title.isBlank() || event.title == "PulseDeck") event.text.ifBlank { "PulseDeck" } else event.title
    val sub = if (title == event.text) "" else event.text
    Row(modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tone.bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tone.fg, modifier = Modifier.size(20.dp))
            }
            if (!last) Box(Modifier.padding(vertical = 4.dp).width(2.dp).weight(1f).background(Palette.Surface2))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(top = 2.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 15.sp, color = Palette.Text, modifier = Modifier.weight(1f), maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(event.time, fontSize = 12.sp, color = Palette.Text2)
            }
            if (sub.isNotBlank()) Text(sub, fontSize = 13.sp, color = Palette.Text2)
        }
    }
}
