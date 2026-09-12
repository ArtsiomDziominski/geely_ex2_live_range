package com.geely.ex2.range.data.store

import com.geely.ex2.range.domain.model.AppThemeMode
import com.geely.ex2.range.domain.model.BufferPoint
import com.geely.ex2.range.domain.model.DriveStatsSnapshot
import com.geely.ex2.range.domain.model.EngineCheckpoint
import com.geely.ex2.range.domain.model.PeriodSnapshot
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.SettingsSnapshot
import com.geely.ex2.range.domain.model.TripSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class JsonStores(private val dir: File) {
    private val periodFile = File(dir, "period.json")
    private val settingsFile = File(dir, "settings.json")
    private val bufferFile = File(dir, "buffer-checkpoint.json")
    private val driveStatsFile = File(dir, "drive-stats.json")
    private var lastDriveStatsText: String? = null

    fun loadSettings(): SettingsSnapshot {
        val json = readObject(settingsFile) ?: return SettingsSnapshot()
        val capacity = json.optDouble("usableCapacityKwh", Double.NaN).takeIf { it.isFinite() && it > 0.0 }
            ?: RangeConstants.EX2_DEFAULT_USABLE_CAPACITY_KWH
        val reserve = json.optDouble("reserveSocPercent", RangeConstants.RESERVE_SOC_PERCENT)
        val overlayEnabled = json.optBoolean("overlayEnabled", false)
        val overlayX = json.optInt("overlayX").takeIf { json.has("overlayX") && !json.isNull("overlayX") }
        val overlayY = json.optInt("overlayY").takeIf { json.has("overlayY") && !json.isNull("overlayY") }
        val themeMode = AppThemeMode.fromStorageKey(json.optString("themeMode", null))
        return SettingsSnapshot(
            usableCapacityKwh = capacity,
            reserveSocPercent = reserve,
            overlayEnabled = overlayEnabled,
            overlayX = overlayX,
            overlayY = overlayY,
            themeMode = themeMode,
        )
    }

    fun saveSettings(settings: SettingsSnapshot) {
        val json = JSONObject()
            .put("usableCapacityKwh", settings.usableCapacityKwh ?: JSONObject.NULL)
            .put("reserveSocPercent", settings.reserveSocPercent)
            .put("overlayEnabled", settings.overlayEnabled)
            .put("overlayX", settings.overlayX ?: JSONObject.NULL)
            .put("overlayY", settings.overlayY ?: JSONObject.NULL)
            .put("themeMode", settings.themeMode.storageKey)
        atomicWrite(settingsFile, json.toString())
    }

    fun loadPeriod(): PeriodSnapshot {
        val json = readObject(periodFile) ?: return PeriodSnapshot(0.0, 0.0, 0L, null)
        return PeriodSnapshot(
            distanceKm = json.optDouble("distanceKm", 0.0),
            socUsedPoints = json.optDouble("socUsedPoints", 0.0),
            updatedAtMs = json.optLong("updatedAtMs", 0L),
            parkSessionId = json.optLong("parkSessionId").takeIf { json.has("parkSessionId") && !json.isNull("parkSessionId") },
        )
    }

    fun savePeriod(period: PeriodSnapshot) {
        val json = JSONObject()
            .put("distanceKm", period.distanceKm)
            .put("socUsedPoints", period.socUsedPoints)
            .put("updatedAtMs", period.updatedAtMs)
            .put("parkSessionId", period.parkSessionId ?: JSONObject.NULL)
        atomicWrite(periodFile, json.toString())
    }

    fun loadCheckpoint(): EngineCheckpoint? {
        val json = readObject(bufferFile) ?: return null
        val period = PeriodSnapshot(
            distanceKm = json.optDouble("periodDistanceKm", 0.0),
            socUsedPoints = json.optDouble("periodSocUsedPoints", 0.0),
            updatedAtMs = json.optLong("periodUpdatedAtMs", 0L),
            parkSessionId = json.optLong("parkSessionId").takeIf { json.has("parkSessionId") && !json.isNull("parkSessionId") },
        )
        val tripJson = json.optJSONObject("trip")
        val trip = tripJson?.let {
            val soc0 = if (it.has("soc0") && !it.isNull("soc0")) {
                it.optDouble("soc0").toFloat()
            } else {
                null
            }
            TripSnapshot(
                active = it.optBoolean("active"),
                soc0 = soc0?.takeIf { value -> value.isFinite() },
                km0 = it.optDouble("km0", 0.0),
                startedAtMs = it.optLong("startedAtMs", 0L),
                lastDistanceKm = it.optDouble("lastDistanceKm", 0.0),
                lastSocUsedPoints = it.optDouble("lastSocUsedPoints", 0.0),
                speedSumKmh = it.optDouble("speedSumKmh", 0.0),
                speedSamples = it.optInt("speedSamples", 0),
                tempSumC = it.optDouble("tempSumC", 0.0),
                tempSamples = it.optInt("tempSamples", 0),
            )
        }
        val points = json.optJSONArray("points") ?: JSONArray()
        val buffer = buildList {
            for (index in 0 until points.length()) {
                val item = points.optJSONObject(index) ?: continue
                add(
                    BufferPoint(
                        elapsedRealtimeMs = item.optLong("elapsed"),
                        wallClockMs = item.optLong("wall"),
                        cumulativeKm = item.optDouble("km"),
                        socPercent = item.optDouble("soc").toFloat(),
                        speedKmh = if (item.has("speed") && !item.isNull("speed")) {
                            item.optDouble("speed").toFloat().takeIf { it.isFinite() && it >= 0f }
                        } else {
                            null
                        },
                        outsideTempC = if (item.has("temp") && !item.isNull("temp")) {
                            item.optDouble("temp").toFloat().takeIf { it.isFinite() }
                        } else {
                            null
                        },
                        chargingLikely = item.optBoolean("charging"),
                        gap = item.optBoolean("gap"),
                    ),
                )
            }
        }
        return EngineCheckpoint(
            period = period,
            buffer = buffer,
            trip = trip,
            totalKm = json.optDouble("totalKm", 0.0),
            lastDrivingSoc = if (json.has("lastDrivingSoc") && !json.isNull("lastDrivingSoc")) {
                json.optDouble("lastDrivingSoc").toFloat().takeIf { it.isFinite() }
            } else {
                null
            },
        )
    }

    fun saveCheckpoint(checkpoint: EngineCheckpoint) {
        val points = JSONArray()
        checkpoint.buffer.forEach { point ->
            points.put(
                JSONObject()
                    .put("elapsed", point.elapsedRealtimeMs)
                    .put("wall", point.wallClockMs)
                    .put("km", point.cumulativeKm)
                    .put("soc", point.socPercent.toDouble())
                    .put("speed", point.speedKmh?.toDouble() ?: JSONObject.NULL)
                    .put("temp", point.outsideTempC?.toDouble() ?: JSONObject.NULL)
                    .put("charging", point.chargingLikely)
                    .put("gap", point.gap),
            )
        }
        val trip = checkpoint.trip?.let {
            JSONObject()
                .put("active", it.active)
                .put("soc0", it.soc0 ?: JSONObject.NULL)
                .put("km0", it.km0)
                .put("startedAtMs", it.startedAtMs)
                .put("lastDistanceKm", it.lastDistanceKm)
                .put("lastSocUsedPoints", it.lastSocUsedPoints)
                .put("speedSumKmh", it.speedSumKmh)
                .put("speedSamples", it.speedSamples)
                .put("tempSumC", it.tempSumC)
                .put("tempSamples", it.tempSamples)
        } ?: JSONObject.NULL
        val json = JSONObject()
            .put("periodDistanceKm", checkpoint.period.distanceKm)
            .put("periodSocUsedPoints", checkpoint.period.socUsedPoints)
            .put("periodUpdatedAtMs", checkpoint.period.updatedAtMs)
            .put("parkSessionId", checkpoint.period.parkSessionId ?: JSONObject.NULL)
            .put("totalKm", checkpoint.totalKm)
            .put("lastDrivingSoc", checkpoint.lastDrivingSoc ?: JSONObject.NULL)
            .put("trip", trip)
            .put("points", points)
        atomicWrite(bufferFile, json.toString())
    }

    fun loadDriveStats(): DriveStatsSnapshot {
        val json = readObject(driveStatsFile) ?: return DriveStatsSnapshot()
        lastDriveStatsText = json.toString()
        return DriveStatsSnapshot(
            maxTripKm = finiteKm(json.optDouble("maxTripKm", 0.0)),
            maxChargeCycleKm = finiteKm(json.optDouble("maxChargeCycleKm", 0.0)),
            maxChargeCycleSocUsedPercent = finiteKm(json.optDouble("maxChargeCycleSocUsedPercent", 0.0)),
            maxChargeCycleAvgSpeedKmh = optNullableDouble(json, "maxChargeCycleAvgSpeedKmh"),
            maxChargeCycleAvgTempC = optNullableDouble(json, "maxChargeCycleAvgTempC"),
            openChargeCycleKm = finiteKm(json.optDouble("openChargeCycleKm", 0.0)),
            openChargeCycleSocUsedPercent = finiteKm(json.optDouble("openChargeCycleSocUsedPercent", 0.0)),
            openChargeCycleSpeedWeightedSum = json.optDouble("openChargeCycleSpeedWeightedSum", 0.0),
            openChargeCycleSpeedWeightKm = finiteKm(json.optDouble("openChargeCycleSpeedWeightKm", 0.0)),
            openChargeCycleTempWeightedSum = json.optDouble("openChargeCycleTempWeightedSum", 0.0),
            openChargeCycleTempWeightKm = finiteKm(json.optDouble("openChargeCycleTempWeightKm", 0.0)),
            chargingSession = json.optBoolean("chargingSession", false),
        )
    }

    fun saveDriveStats(snapshot: DriveStatsSnapshot) {
        val json = JSONObject()
            .put("maxTripKm", snapshot.maxTripKm)
            .put("maxChargeCycleKm", snapshot.maxChargeCycleKm)
            .put("maxChargeCycleSocUsedPercent", snapshot.maxChargeCycleSocUsedPercent)
            .put("maxChargeCycleAvgSpeedKmh", snapshot.maxChargeCycleAvgSpeedKmh ?: JSONObject.NULL)
            .put("maxChargeCycleAvgTempC", snapshot.maxChargeCycleAvgTempC ?: JSONObject.NULL)
            .put("openChargeCycleKm", snapshot.openChargeCycleKm)
            .put("openChargeCycleSocUsedPercent", snapshot.openChargeCycleSocUsedPercent)
            .put("openChargeCycleSpeedWeightedSum", snapshot.openChargeCycleSpeedWeightedSum)
            .put("openChargeCycleSpeedWeightKm", snapshot.openChargeCycleSpeedWeightKm)
            .put("openChargeCycleTempWeightedSum", snapshot.openChargeCycleTempWeightedSum)
            .put("openChargeCycleTempWeightKm", snapshot.openChargeCycleTempWeightKm)
            .put("chargingSession", snapshot.chargingSession)
        val text = json.toString()
        if (text == lastDriveStatsText) return
        atomicWrite(driveStatsFile, text)
        lastDriveStatsText = text
    }

    private fun optNullableDouble(json: JSONObject, key: String): Double? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optDouble(key).takeIf { it.isFinite() }
    }

    private fun finiteKm(value: Double): Double {
        return if (value.isFinite() && value > 0.0) value else 0.0
    }

    private fun readObject(file: File): JSONObject? {
        if (!file.exists()) return null
        return try {
            JSONObject(file.readText())
        } catch (_: Throwable) {
            null
        }
    }

    private fun atomicWrite(file: File, text: String) {
        dir.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }
}
