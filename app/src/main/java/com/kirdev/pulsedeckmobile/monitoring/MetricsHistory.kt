package com.kirdev.pulsedeckmobile.monitoring

import com.kirdev.pulsedeckmobile.data.model.Metrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Last [SIZE] values of each series for the charts; built from the live stream, nothing extra asked. */
data class Series(val cpu: List<Float>, val gpu: List<Float>, val ram: List<Float>, val down: List<Float>, val up: List<Float>) {
    companion object { val Empty = Series(emptyList(), emptyList(), emptyList(), emptyList(), emptyList()) }
}

class MetricsHistory(metrics: StateFlow<Metrics?>, scope: CoroutineScope) {
    private val _series = MutableStateFlow(Series.Empty)
    val series: StateFlow<Series> = _series.asStateFlow()

    init {
        scope.launch {
            metrics.collect { m ->
                // null = link lost or app in background: the chart restarts rather than joining old and new data.
                _series.value = if (m == null) Series.Empty else _series.value.let {
                    Series(
                        it.cpu.push(m.cpu.load), it.gpu.push(m.gpu.load), it.ram.push(m.ram.percent),
                        it.down.push(m.netDown), it.up.push(m.netUp),
                    )
                }
            }
        }
    }

    private fun List<Float>.push(value: Double?): List<Float> = (this + (value?.toFloat() ?: 0f)).takeLast(SIZE)

    companion object { const val SIZE = 60 }
}
