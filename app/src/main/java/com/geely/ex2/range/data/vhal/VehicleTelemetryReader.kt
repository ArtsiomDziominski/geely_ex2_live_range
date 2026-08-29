package com.geely.ex2.range.data.vhal

import android.content.Context
import android.os.SystemClock
import com.geely.ex2.range.domain.decode.GearDecoder
import com.geely.ex2.range.domain.decode.SocDecoder
import com.geely.ex2.range.domain.decode.SpeedDecoder
import com.geely.ex2.range.domain.decode.TemperatureDecoder
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

        val oemSoc = client.readFloat(VhalIds.PROP_ED_EV_BATTERY_PERCENTAGE)
        val level = client.readFloat(VhalIds.PROP_EV_BATTERY_LEVEL)
        val currentCap = client.readFloat(VhalIds.PROP_EV_CURRENT_BATTERY_CAPACITY)
        val nominalCap = client.readFloat(VhalIds.PROP_INFO_EV_BATTERY_CAPACITY)
        val speedProbe = client.readFloat(VhalIds.PROP_PERF_VEHICLE_SPEED)
        val odoFloat = client.readFloat(VhalIds.PROP_PERF_ODOMETER)
        val odoInt = if (odoFloat.ok) null else client.readInt(VhalIds.PROP_PERF_ODOMETER)
        val gearProbe = client.readInt(VhalIds.PROP_CURRENT_GEAR)
        val ambient = client.readInt(VhalIds.PROP_AC_AMBIENT_TEMP)
        val inside = client.readInt(VhalIds.PROP_AC_INSIDE_TEMP)
        val env = client.readFloat(VhalIds.PROP_ENV_OUTSIDE_TEMPERATURE)
        val peps = client.readInt(VhalIds.PROP_PEPS_POWER_MODE)

        val soc = SocDecoder.decodePercent(
            oemPercent = oemSoc.value,
            batteryLevel = level.value,
            currentCapacityWh = currentCap.value,
            nominalCapacityWh = nominalCap.value,
        )
        val speed = liveSpeed ?: SpeedDecoder.rawKmh(speedProbe.value)
        val odometer = odoFloat.value ?: odoInt?.value?.toFloat()
        val gearValue = liveGear ?: gearProbe.value
        val outside = TemperatureDecoder.outsideC(ambient.value, env.value)
        val cabin = inside.value?.let { TemperatureDecoder.fromFlymeRaw(it) }

        lines += oemSoc.line("OEM SOC %", "float 0–100, без округления", soc?.let { String.format(Locale.US, "%.3f %%", it) })
        lines += level.line("EV_BATTERY_LEVEL", "часто Wh, не %")
        lines += currentCap.line("EV_CURRENT_BATTERY_CAPACITY", "ожидание Wh")
        lines += nominalCap.line("INFO_EV_BATTERY_CAPACITY", "fallback C, Wh→кВт·ч")
        lines += speedProbe.line("PERF_VEHICLE_SPEED", "км/ч как raw, без +1", speed?.let { String.format(Locale.US, "%.2f км/ч", it) })
        lines += odoFloat.line("PERF_ODOMETER float", "проверить на авто", odometer?.let { String.format(Locale.US, "%.3f км", it) })
        if (odoInt != null) lines += odoInt.line("PERF_ODOMETER int", "fallback")
        lines += gearProbe.line("CURRENT_GEAR", "P=4 R=2 N=1 D=8", GearDecoder.fromVhal(gearValue)?.label)
        lines += ambient.line("AC_AMBIENT_TEMP", "(raw-80)/2", outside?.let { String.format(Locale.US, "%.1f °C", it) })
        lines += inside.line("AC_INSIDE_TEMP", "(raw-80)/2", cabin?.let { String.format(Locale.US, "%.1f °C", it) })
        lines += env.line("ENV_OUTSIDE_TEMPERATURE", "float °C fallback")
        lines += peps.line("PEPS_POWER_MODE", "0 = Acc Off")

        val tick = TelemetryTick(
            elapsedRealtimeMs = SystemClock.elapsedRealtime(),
            wallClockMs = wallClockMs,
            socPercent = soc,
            speedKmh = speed,
            odometerKm = odometer,
            gear = GearDecoder.fromVhal(gearValue),
            outsideTempC = outside,
            cabinTempC = cabin,
            pepsPowerMode = peps.value,
            currentCapacityWh = currentCap.value,
            nominalCapacityWh = nominalCap.value,
        )
        return TelemetryRead(
            tick = tick,
            raw = RawTelemetry(carReady = client.isReady, connectError = null, lines = lines),
        )
    }

    fun close() {
        callbacks.forEach(client::unregister)
        callbacks.clear()
        client.close()
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
            cabinTempC = null,
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
