package com.geely.ex2.range.data.vhal

import android.content.Context
import android.util.Log
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Thin reflection adapter to `android.car.Car` / `CarPropertyManager`.
 * Proven connect/get/callback pattern from Geely EX2 Tools — without writes or Tools UI.
 */
class CarClient(context: Context) {
    private val appContext = context.applicationContext
    private var carClass: Class<*>? = null
    private var car: Any? = null
    private var propertyManager: Any? = null

    val isReady: Boolean
        get() = propertyManager != null

    fun ensureConnected(debug: StringBuilder? = null): Boolean {
        return try {
            ensureInitialized(debug)
            propertyManager != null
        } catch (t: Throwable) {
            Log.w(TAG, "CarPropertyManager init failed", t)
            debug?.append('\n')?.append(shortError(t))
            false
        }
    }

    fun readFloat(propertyId: Int, areaId: Int = VhalIds.GLOBAL_AREA_ID): Probe<Float> {
        val manager = propertyManager ?: return Probe.err(propertyId, "CarPropertyManager is null")
        return try {
            val method: Method = manager.javaClass.getMethod(
                "getFloatProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            when (val value = method.invoke(manager, propertyId, areaId)) {
                is Float -> Probe.ok(propertyId, value)
                else -> Probe.err(propertyId, "not Float: $value")
            }
        } catch (t: Throwable) {
            Probe.err(propertyId, shortError(t))
        }
    }

    fun readInt(propertyId: Int, areaId: Int = VhalIds.GLOBAL_AREA_ID): Probe<Int> {
        val manager = propertyManager ?: return Probe.err(propertyId, "CarPropertyManager is null")
        return try {
            val method: Method = manager.javaClass.getMethod(
                "getIntProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            when (val value = method.invoke(manager, propertyId, areaId)) {
                is Int -> Probe.ok(propertyId, value)
                else -> Probe.err(propertyId, "not Int: $value")
            }
        } catch (t: Throwable) {
            Probe.err(propertyId, shortError(t))
        }
    }

    fun registerFloatCallback(
        propertyId: Int,
        updateRateHz: Float,
        onValue: (Float) -> Unit,
        onError: (String) -> Unit,
    ): Any? = registerCallback(propertyId, updateRateHz, onError) { raw ->
        parseNumber(raw)?.toFloat()?.let(onValue)
    }

    fun registerIntCallback(
        propertyId: Int,
        updateRateHz: Float,
        onValue: (Int) -> Unit,
        onError: (String) -> Unit,
    ): Any? = registerCallback(propertyId, updateRateHz, onError) { raw ->
        parseNumber(raw)?.toInt()?.let(onValue)
    }

    fun unregister(callback: Any?) {
        val manager = propertyManager ?: return
        val callbackObject = callback ?: return
        try {
            val callbackClass = Class.forName(
                "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
            )
            manager.javaClass.getMethod("unregisterCallback", callbackClass).invoke(manager, callbackObject)
        } catch (t: Throwable) {
            Log.w(TAG, "unregisterCallback failed", t)
        }
    }

    fun close() {
        try {
            car?.javaClass?.getMethod("disconnect")?.invoke(car)
        } catch (_: Throwable) {
        }
        car = null
        propertyManager = null
    }

    private fun registerCallback(
        propertyId: Int,
        updateRateHz: Float,
        onError: (String) -> Unit,
        onChange: (Any?) -> Unit,
    ): Any? {
        val manager = propertyManager ?: return null
        return try {
            val callbackClass = Class.forName(
                "android.car.hardware.property.CarPropertyManager\$CarPropertyEventCallback",
            )
            val callback = Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { proxy, method, args ->
                when (method.name) {
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.getOrNull(0)
                    "toString" -> "RangeCarCallback(0x${propertyId.toString(16)})"
                    "onChangeEvent" -> {
                        onChange(args?.getOrNull(0))
                        null
                    }
                    "onErrorEvent" -> {
                        val propId = args?.getOrNull(0) as? Int ?: propertyId
                        val zone = args?.getOrNull(1) as? Int ?: 0
                        onError("property 0x${propId.toString(16)} zone $zone")
                        null
                    }
                    else -> null
                }
            }
            val accepted = manager.javaClass.getMethod(
                "registerCallback",
                callbackClass,
                Int::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            ).invoke(manager, callback, propertyId, updateRateHz) as? Boolean
            if (accepted == false) {
                Log.w(TAG, "registerCallback rejected 0x${propertyId.toString(16)}")
                null
            } else {
                callback
            }
        } catch (t: Throwable) {
            Log.w(TAG, "registerCallback failed 0x${propertyId.toString(16)}", t)
            null
        }
    }

    private fun ensureInitialized(debug: StringBuilder?) {
        if (propertyManager != null) {
            debug?.append("CarPropertyManager: OK cached")
            return
        }
        if (carClass == null) {
            carClass = Class.forName("android.car.Car")
            debug?.append("android.car.Car: OK")
        }
        val classRef = carClass ?: return
        if (car == null) {
            car = classRef.getMethod("createCar", Context::class.java).invoke(null, appContext)
            debug?.append("\ncreateCar: ")?.append(if (car == null) "NULL" else "OK")
        }
        val carObject = car ?: return
        try {
            val connected = (classRef.getMethod("isConnected").invoke(carObject) as? Boolean) == true
            debug?.append("\nisConnected: ")?.append(connected)
            if (!connected) {
                classRef.getMethod("connect").invoke(carObject)
                debug?.append("\nconnect: called")
            }
        } catch (e: IllegalStateException) {
            debug?.append("\nconnect: ")?.append(e.message)
        } catch (_: NoSuchMethodException) {
            debug?.append("\nconnect: no method")
        }
        propertyManager = classRef.getMethod("getCarManager", String::class.java)
            .invoke(carObject, VhalIds.CAR_MANAGER_PROPERTY)
        debug?.append("\ngetCarManager(property): ")
            ?.append(if (propertyManager == null) "NULL" else "OK")
    }

    private fun parseNumber(carPropertyValue: Any?): Number? {
        if (carPropertyValue == null) return null
        return try {
            when (val value = carPropertyValue.javaClass.getMethod("getValue").invoke(carPropertyValue)) {
                is Number -> value
                else -> null
            }
        } catch (t: Throwable) {
            Log.w(TAG, "CarPropertyValue parse failed", t)
            null
        }
    }

    data class Probe<T>(
        val propertyId: Int,
        val ok: Boolean,
        val value: T?,
        val error: String?,
    ) {
        companion object {
            fun <T> ok(propertyId: Int, value: T): Probe<T> = Probe(propertyId, true, value, null)
            fun <T> err(propertyId: Int, error: String): Probe<T> = Probe(propertyId, false, null, error)
        }
    }

    companion object {
        private const val TAG = "Ex2RangeCar"

        fun shortError(error: Throwable): String {
            var cause = error
            while (cause.cause != null) {
                cause = cause.cause!!
            }
            val message = cause.message
            return if (message.isNullOrEmpty()) cause.javaClass.simpleName else "${cause.javaClass.simpleName}: $message"
        }
    }
}
