package com.example.lession_equipo1.model

data class EmgSample(
    val sensor: String,
    val metric: String,
    val valueMv: Float,
    val unit: String,
    val adcRaw: Int,
    val status: String,
    val timestampMs: Long,
    val sequence: Long
)

data class EmgSession(
    val latestSample: EmgSample? = null,
    val sampleCount: Long = 0,
    val sampleValuesMv: List<Float> = emptyList(),
    val averageMv: Double? = null,
    val maxSessionMv: Float? = null,
    val referenceRestMv: Float? = null,
    val referenceMaxMv: Float? = null,
    val lastTimestampMs: Long? = null
) {
    val performancePercent: Float?
        get() {
            val rest = referenceRestMv ?: return null
            val maximum = referenceMaxMv ?: return null
            val current = latestSample?.valueMv ?: return null
            if (!rest.isFinite() || !maximum.isFinite() || maximum <= rest) return null
            return ((current - rest) / (maximum - rest) * 100f).coerceIn(0f, 100f)
        }

    val maximumEffortReached: Boolean
        get() {
            val maximum = referenceMaxMv ?: return false
            val rest = referenceRestMv ?: return false
            val current = latestSample?.valueMv ?: return false
            return maximum.isFinite() && rest.isFinite() && maximum > rest && current >= maximum
        }

    fun accept(sample: EmgSample): EmgSession {
        if (sample.sensor != "emg_module" || sample.metric != "emgSignal" ||
            sample.status != "valid" || sample.unit != "mV" ||
            !sample.valueMv.isFinite() || sample.valueMv < 0f ||
            sample.adcRaw !in 0..4095 ||
            lastTimestampMs?.let { sample.timestampMs <= it } == true
        ) {
            return this
        }

        val nextCount = sampleCount + 1
        val previousTotal = (averageMv ?: 0.0) * sampleCount
        return copy(
            latestSample = sample,
            sampleCount = nextCount,
            sampleValuesMv = (sampleValuesMv + sample.valueMv).takeLast(60),
            averageMv = (previousTotal + sample.valueMv) / nextCount,
            maxSessionMv = maxOf(maxSessionMv ?: sample.valueMv, sample.valueMv),
            lastTimestampMs = sample.timestampMs
        )
    }

    fun resetSession(): EmgSession = copy(
        latestSample = null,
        sampleCount = 0,
        sampleValuesMv = emptyList(),
        averageMv = null,
        maxSessionMv = null,
        lastTimestampMs = null
    )
}
