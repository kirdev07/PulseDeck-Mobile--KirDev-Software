package com.kirdev.pulsedeckmobile.feature.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ListRow
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

    /** Asks only for events newer than the last one seen. */
    suspend fun refresh() {
        val (fresh, newest) = api.events(last)
        if (fresh.isNotEmpty()) _events.value = (fresh.reversed() + _events.value).distinctBy { it.id }.take(100)
        last = newest
    }

    companion object { const val POLL_MS = 10_000L }
}

@Composable
fun EventsScreen(app: PulseDeskApp, onBack: () -> Unit) {
    val events by app.events.events.collectAsStateWithLifecycle()
    PollWhileVisible(EventsFeed.POLL_MS) { app.events.refresh() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("События ПК", onBack)
        if (events.isEmpty()) Placeholder("Событий пока нет. Здесь появятся блокировка ПК и предупреждения о нагрузке.")
        LazyColumn(Modifier.padding(horizontal = 12.dp)) {
            items(events, key = { it.id }) { EventRow(it) }
        }
    }
}

@Composable
fun EventRow(event: PcEvent) {
    ListRow(AppIcons.Bell, event.title.ifBlank { "PulseDeck" }, listOf(event.time, event.text).filter { it.isNotBlank() }.joinToString(" · "))
}
