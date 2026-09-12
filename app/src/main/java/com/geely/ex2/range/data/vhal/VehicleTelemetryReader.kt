package com.geely.ex2.range.data.vhal

import android.content.Context
import android.os.SystemClock
import com.geely.ex2.range.domain.decode.GearDecoder
import com.geely.ex2.range.domain.decode.SocDecoder
import com.geely.ex2.range.domain.decode.SpeedDecoder
import com.geely.ex2.range.domain.decode.TemperatureDecoder
import com.geely.ex2.range.domain.model.RangeConstants
import com.geely.ex2.range.domain.model.RawPropertyLine
import com.geely.ex2.range.domain.model.RawTelemetry
import com.geely.ex2.range.domain.model.TelemetryTick
import java.util.Locale

data class TelemetryRead(
    val tick: TelemetryTick,
    val raw: RawTelemetry,
)

class VehicleTelemetryReader(context: Context) {
    private val client = CarClient(context)
    private val callbacks = mutableListOf<Any>()

    @Volatile
    private var liveSpeed: Float? = null

    @Volatile
    private var liveGear: Int? = null

    private var cachedOutsideC: Float? = null
    private var cachedAmbient: CarClient.Probe<Int>? = null
    private var cachedEnv: CarClient.Probe<Float>? = null
    private var lastOutsideTempElapsedMs: Long = Long.MIN_VALUE

    private var cachedSoc: Float? = null
    private var cachedOemSoc: CarClient.Probe<Float>? = null
    private var cachedLevel: CarClient.Probe<Float>? = null
    private var cachedCurrentCap: CarClient.Probe<Float>? = null
    private var cachedNominalCap: CarClient.Probe<Float>? = null
    private var lastSocElapsedMs: Long = Long.MIN_VALUE

    private var cachedOdometerKm: Float? = null
    private var cachedOdoFloat: CarClient.Probe<Float>? = null
    private var cachedOdoInt: CarClient.Probe<Int>? = null
    private var lastOdometerElapsedMs: Long = Long.MIN_VALUE

    private var cachedVehicleRangeRemainingKm: Float? = null
    private var cachedRangeRemainingFloat: CarClient.Probe<Float>? = null
    private var cachedRangeRemainingInt: CarClient.Probe<Int>? = null
    private var lastRangeRemainingElapsedMs: Long = Long.MIN_VALUE

    fun connect(): RawTelemetry {
        val debug = StringBuilder()
        val ready = client.ensureConnected(debug)
        return RawTelemetry(
            carReady = ready,
            connectError = if (ready) null else debug.toString().ifBlank { "Car API недоступен" },
            lines = emptyList(),
        )
    }

    @Volatile
    private var subscribed = false

    fun subscribeLive() {
        if (subscribed || !client.isReady) return
        subscribed = true
        client.registerFloatCallback(
            propertyId = VhalIds.PROP_PERF_VEHICLE_SPEED,
            updateRateHz = VhalIds.SPEED_RATE_HZ,
            onValue = { raw -> liveSpeed = SpeedDecoder.rawKmh(raw) },
            onError = {},
        )?.let(callbacks::add)

        for (propertyId in VhalIds.GEAR_CANDIDATES) {
            val callback = client.registerIntCallback(
                propertyId = propertyId,
                updateRateHz = VhalIds.ON_CHANGE_HZ,
                onValue = { value -> liveGear = value },
                onError = {},
            )
            if (callback != null) {
                callbacks.add(callback)
                break
            }
        }
    }

    fun read(wallClockMs: Long): TelemetryRead {
        val lines = mutableListOf<RawPropertyLine>()
        if (!client.isReady && !client.ensureConnected()) {
            return TelemetryRead(
                tick = emptyTick(wallClockMs),
                raw = RawTelemetry(false, "CarPropertyManager is null", emptyList()),
            )
        }
        subscribeLive()
        val elapsedMs = SystemClock.elapsedRealtime()
        val socBundle = readSoc(elapsedMs)
        val odoBundle = readOdometer(elapsedMs)
        val rangeRemainingBundle = readRangeRemaining(elapsedMs)
        val gearProbe = client.readInt(VhalIds.PROP_CURRENT_GEAR)
        val (ambient, env) = readOutsideTemperature(elapsedMs)
        val peps = client.readInt(VhalIds.PROP_PEPS_POWER_MODE)

        val soc = cachedSoc
        val speed = liveSpeed
        val odometer = cachedOdometerKm
        val vehicleRangeRemaining = cachedVehicleRangeRemainingKm
        val gearValue = liveGear ?: gearProbe.value
        val outside = cachedOutsideC

        lines += socBundle.oem.line("OEM SOC %", "опрос раз в 10 с", soc?.let { String.format(Locale.US, "%.3f %%", it) })
        lines += socBundle.level.line("EV_BATTERY_LEVEL", "часто Wh, не %")
        lines += socBundle.currentCap.line("EV_CURRENT_BATTERY_CAPACITY", "ожидание Wh")
        lines += socBundle.nominalCap.line("INFO_EV_BATTERY_CAPACITY", "fallback C, Wh→кВт·ч")
        lines += speedCallbackLine(speed)
        lines += odoBundle.odoFloat.line("PERF_ODOMETER float", "опрос раз в 10 с", odometer?.let { String.format(Locale.US, "%.3f км", it) })
        if (odoBundle.odoInt != null) lines += odoBundle.odoInt.line("PERF_ODOMETER int", "fallback")
        lines += rangeRemainingBundle.float.line(
            "RANGE_REMAINING float",
            "опрос раз в 10 с, спека AOSP: метры — сверить на авто",
            vehicleRangeRemaining?.let { String.format(Locale.US, "%.1f км", it) },
        )
        if (rangeRemainingBundle.int != null) {
            lines += rangeRemainingBundle.int.line("RANGE_REMAINING int", "fallback, как PERF_ODOMETER на этом авто")
        }
        lines += gearProbe.line("CURRENT_GEAR", "P=4 R=2 N=1 D=8", GearDecoder.fromVhal(gearValue)?.label)
        lines += ambient.line("AC_AMBIENT_TEMP", "опрос раз в 200 с, (raw-80)/2", outside?.let { String.format(Locale.US, "%.1f °C", it) })
        lines += env.line("ENV_OUTSIDE_TEMPERATURE", "float °C fallback")
        lines += peps.line("PEPS_POWER_MODE", "0 = Acc Off")

        val tick = TelemetryTick(
            elapsedRealtimeMs = elapsedMs,
            wallClockMs = wallClockMs,
            socPercent = soc,
            speedKmh = speed,
            odometerKm = odometer,
            gear = GearDecoder.fromVhal(gearValue),
            outsideTempC = outside,
            pepsPowerMode = peps.value,
            currentCapacityWh = socBundle.currentCap.value,
            nominalCapacityWh = socBundle.nominalCap.value,
            vehicleRangeRemainingKm = vehicleRangeRemaining,
        )
        return TelemetryRead(
            tick = tick,
            raw = RawTelemetry(carReady = client.isReady, connectError = null, lines = lines),
        )
    }

    fun close() {
        callbacks.forEach(client::unregister)
        callbacks.clear()
        subscribed = false
        liveSpeed = null
        liveGear = null
        cachedOutsideC = null
        cachedAmbient = null
        cachedEnv = null
        lastOutsideTempElapsedMs = Long.MIN_VALUE
        cachedSoc = null
        cachedOemSoc = null
        cachedLevel = null
        cachedCurrentCap = null
        cachedNominalCap = null
        lastSocElapsedMs = Long.MIN_VALUE
        cachedOdometerKm = null
        cachedOdoFloat = null
        cachedOdoInt = null
        lastOdometerElapsedMs = Long.MIN_VALUE
        cachedVehicleRangeRemainingKm = null
        cachedRangeRemainingFloat = null
        cachedRangeRemainingInt = null
        lastRangeRemainingElapsedMs = Long.MIN_VALUE
        client.close()
    }

    private data class SocBundle(
        val oem: CarClient.Probe<Float>,
        val level: CarClient.Probe<Float>,
        val currentCap: CarClient.Probe<Float>,
        val nominalCap: CarClient.Probe<Float>,
    )

    private data class OdoBundle(
        val odoFloat: CarClient.Probe<Float>,
        val odoInt: CarClient.Probe<Int>?,
    )

    private data class RangeRemainingBundle(
        val float: CarClient.Probe<Float>,
        val int: CarClient.Probe<Int>?,
    )

    private fun readSoc(elapsedMs: Long): SocBundle {
        val cached = cachedSocBundle()
        if (!pollDue(lastSocElapsedMs, elapsedMs, RangeConstants.SOC_ODOMETER_POLL_MS) && cached != null) {
            return cached
        }
        val oem = client.readFloat(VhalIds.PROP_ED_EV_BATTERY_PERCENTAGE)
        val level = client.readFloat(VhalIds.PROP_EV_BATTERY_LEVEL)
        val currentCap = client.readFloat(VhalIds.PROP_EV_CURRENT_BATTERY_CAPACITY)
        val nominalCap = client.readFloat(VhalIds.PROP_INFO_EV_BATTERY_CAPACITY)
        cachedOemSoc = oem
        cachedLevel = level
        cachedCurrentCap = currentCap
        cachedNominalCap = nominalCap
        val decoded = SocDecoder.decodePercent(
            oemPercent = oem.value,
            batteryLevel = level.value,
            currentCapacityWh = currentCap.value,
            nominalCapacityWh = nominalCap.value,
        )
        if (decoded != null) {
            cachedSoc = decoded
            lastSocElapsedMs = elapsedMs
        } else if (cachedSoc != null) {
            lastSocElapsedMs = elapsedMs
        }
        return SocBundle(oem, level, currentCap, nominalCap)
    }

    private fun cachedSocBundle(): SocBundle? {
        val oem = cachedOemSoc ?: return null
        val level = cachedLevel ?: return null
        val currentCap = cachedCurrentCap ?: return null
        val nominalCap = cachedNominalCap ?: return null
        return SocBundle(oem, level, currentCap, nominalCap)
    }

    private fun readOdometer(elapsedMs: Long): OdoBundle {
        val cached = cachedOdoBundle()
        if (!pollDue(lastOdometerElapsedMs, elapsedMs, RangeConstants.SOC_ODOMETER_POLL_MS) && cached != null) {
            return cached
        }
        val odoFloat = client.readFloat(VhalIds.PROP_PERF_ODOMETER)
        val odoInt = if (odoFloat.ok) null else client.readInt(VhalIds.PROP_PERF_ODOMETER)
        cachedOdoFloat = odoFloat
        cachedOdoInt = odoInt
        val decoded = odoFloat.value ?: odoInt?.value?.toFloat()
        if (decoded != null && decoded.isFinite()) {
            cachedOdometerKm = decoded
            lastOdometerElapsedMs = elapsedMs
        } else if (cachedOdometerKm != null) {
            lastOdometerElapsedMs = elapsedMs
        }
        return OdoBundle(odoFloat, odoInt)
    }

    private fun cachedOdoBundle(): OdoBundle? {
        val odoFloat = cachedOdoFloat ?: return null
        return OdoBundle(odoFloat, cachedOdoInt)
    }

    /**
     * PROP_RANGE_REMAINING is spec'd FLOAT/meters like PERF_ODOMETER — and on this vehicle
     * PERF_ODOMETER's HAL ignores that and answers as Int, so try Int the same way here.
     */
    private fun readRangeRemaining(elapsedMs: Long): RangeRemainingBundle {
        val cached = cachedRangeRemainingBundle()
        if (!pollDue(lastRangeRemainingElapsedMs, elapsedMs, RangeConstants.SOC_ODOMETER_POLL_MS) && cached != null) {
            return cached
        }
        val floatProbe = client.readFloat(VhalIds.PROP_RANGE_REMAINING)
        val intProbe = if (floatProbe.ok) null else client.readInt(VhalIds.PROP_RANGE_REMAINING)
        cachedRangeRemainingFloat = floatProbe
        cachedRangeRemainingInt = intProbe
        val meters = floatProbe.value ?: intProbe?.value?.toFloat()
        if (meters != null && meters.isFinite() && meters >= 0f) {
            cachedVehicleRangeRemainingKm = meters / 1000f
            lastRangeRemainingElapsedMs = elapsedMs
        } else if (cachedVehicleRangeRemainingKm != null) {
            lastRangeRemainingElapsedMs = elapsedMs
        }
        return RangeRemainingBundle(floatProbe, intProbe)
    }

    private fun cachedRangeRemainingBundle(): RangeRemainingBundle? {
        val floatProbe = cachedRangeRemainingFloat ?: return null
        return RangeRemainingBundle(floatProbe, cachedRangeRemainingInt)
    }

    private fun pollDue(lastElapsedMs: Long, nowElapsedMs: Long, intervalMs: Long): Boolean {
        return lastElapsedMs == Long.MIN_VALUE || nowElapsedMs - lastElapsedMs >= intervalMs
    }

    private fun readOutsideTemperature(elapsedMs: Long): Pair<CarClient.Probe<Int>, CarClient.Probe<Float>> {
        val cachedPair = cachedAmbient to cachedEnv
        val due = lastOutsideTempElapsedMs == Long.MIN_VALUE ||
            elapsedMs - lastOutsideTempElapsedMs >= RangeConstants.OUTSIDE_TEMP_POLL_MS
        if (!due) {
            val ambient = cachedPair.first
            val env = cachedPair.second
            if (ambient != null && env != null) return ambient to env
        }
        val ambient = client.readInt(VhalIds.PROP_AC_AMBIENT_TEMP)
        val env = client.readFloat(VhalIds.PROP_ENV_OUTSIDE_TEMPERATURE)
        cachedAmbient = ambient
        cachedEnv = env
        val decoded = TemperatureDecoder.outsideC(ambient.value, env.value)
        if (decoded != null) {
            cachedOutsideC = decoded
            lastOutsideTempElapsedMs = elapsedMs
        } else if (cachedOutsideC != null) {
            lastOutsideTempElapsedMs = elapsedMs
        }
        return ambient to env
    }

    private fun speedCallbackLine(speed: Float?): RawPropertyLine {
        return RawPropertyLine(
            name = "PERF_VEHICLE_SPEED",
            propertyHex = "0x${VhalIds.PROP_PERF_VEHICLE_SPEED.toString(16).uppercase(Locale.US)}",
            ok = speed != null,
            rawText = if (speed != null) "callback 1 Гц, без getFloat" else "ждём callback",
            decodedText = speed?.let { String.format(Locale.US, "%.2f км/ч", it) },
        )
    }

    private fun emptyTick(wallClockMs: Long): TelemetryTick {
        return TelemetryTick(
            elapsedRealtimeMs = SystemClock.elapsedRealtime(),
            wallClockMs = wallClockMs,
            socPercent = null,
            speedKmh = null,
            odometerKm = null,
            gear = null,
            outsideTempC = null,
            pepsPowerMode = null,
            currentCapacityWh = null,
            nominalCapacityWh = null,
        )
    }

    private fun CarClient.Probe<*>.line(
        name: String,
        hint: String,
        decoded: String? = null,
    ): RawPropertyLine {
        val rawText = if (ok) {
            String.format(Locale.US, "%s (%s)", value.toString(), hint)
        } else {
            error ?: "error"
        }
        return RawPropertyLine(
            name = name,
            propertyHex = "0x${propertyId.toString(16).uppercase(Locale.US)}",
            ok = ok,
            rawText = rawText,
            decodedText = decoded,
        )
    }
}
