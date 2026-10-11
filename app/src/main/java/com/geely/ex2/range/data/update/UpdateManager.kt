package com.geely.ex2.range.data.update

import android.content.Context
import android.os.Process
import android.util.Log
import com.geely.ex2.range.BuildConfig
import com.geely.ex2.range.domain.model.UpdateInfo
import com.geely.ex2.range.domain.model.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Обновление приложения по сети: проверка последнего релиза, загрузка APK с проверкой
 * контрольной суммы и установка. Проверять можно в любой сборке, ставить — только в той, что
 * собрана для ГУ ([selfUpdateSupported]): debug-сборка подписана другим ключом, релизный APK
 * поверх неё не встанет. Сам ничего не ставит — установку всегда запускает пользователь.
 */
class UpdateManager(
    context: Context,
    /** Фоновый scope приложения (IO): сеть и запись файла идут в нём. */
    private val scope: CoroutineScope,
    private val installedVersionCode: Int = BuildConfig.VERSION_CODE,
    val selfUpdateSupported: Boolean = BuildConfig.SELF_UPDATE,
) {
    private val appContext = context.applicationContext
    private val client = UpdateClient()
    private val installer = ApkInstaller(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val updateDir = File(appContext.cacheDir, "update")

    /** Одна сетевая операция за раз: повторное нажатие во время проверки или загрузки игнорируется. */
    private val busy = AtomicBoolean(false)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Автопроверка: не чаще раза в сутки, чтобы не дёргать сеть на каждом запуске. */
    fun checkIfDue(nowMs: Long) {
        val last = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        if (nowMs - last >= CHECK_INTERVAL_MS || nowMs < last) check(nowMs)
    }

    fun check(nowMs: Long = System.currentTimeMillis()) {
        if (!busy.compareAndSet(false, true)) return
        scope.launch {
            try {
                _state.value = UpdateState.Checking
                val info = client.fetchManifest()
                prefs.edit().putLong(KEY_LAST_CHECK_MS, nowMs).apply()
                _state.value = if (info != null && info.isNewerThan(installedVersionCode)) {
                    UpdateState.Available(info)
                } else {
                    UpdateState.UpToDate
                }
            } catch (e: IOException) {
                Log.w(TAG, "update check failed", e)
                _state.value = UpdateState.Failed("Не удалось проверить обновления — нет связи с сервером", null)
            } finally {
                busy.set(false)
            }
        }
    }

    /** Качает и ставит найденное обновление. Работает из «доступно» и из неудачной попытки. */
    fun downloadAndInstall() {
        if (!selfUpdateSupported) return
        val info = when (val current = _state.value) {
            is UpdateState.Available -> current.info
            is UpdateState.Failed -> current.info ?: return
            else -> return
        }
        if (!busy.compareAndSet(false, true)) return
        scope.launch {
            try {
                _state.value = UpdateState.Downloading(info, null)
                val apk = File(updateDir, "update.apk")
                client.download(info, apk) { progress ->
                    _state.value = UpdateState.Downloading(info, progress)
                }
                _state.value = UpdateState.Installing(info)
                // Отметка переживает перезапуск: по ней новая версия поймёт, что её поставили
                // по сети и предкомпиляцию никто не сделал.
                prefs.edit().putInt(KEY_PENDING_COMPILE, info.versionCode).apply()
                installer.install(apk)
            } catch (e: CorruptUpdateException) {
                Log.w(TAG, "update file rejected", e)
                fail("Файл обновления повреждён: ${e.message}", info)
            } catch (e: IOException) {
                Log.w(TAG, "update download failed", e)
                fail("Не удалось скачать обновление — проверьте связь", info)
            } catch (t: Throwable) {
                Log.w(TAG, "update install failed", t)
                fail("Не удалось установить обновление: ${t.message ?: t.javaClass.simpleName}", info)
            } finally {
                busy.set(false)
            }
        }
    }

    /** Система отказалась ставить APK (несовпадение подписи, понижение версии и т. п.). */
    fun onInstallFailed(message: String?) {
        val info = (_state.value as? UpdateState.Installing)?.info
        fail("Система не установила обновление" + (message?.let { ": $it" } ?: ""), info)
    }

    /**
     * Первый запуск после обновления по сети. Установка через систему не компилирует код заранее,
     * и на ГУ интерфейс без этого заметно медленнее (см. docs/system-install.md), поэтому
     * компилируем сами — тем же `cmd package compile`, что вручную через adb. Скомпилированный
     * код подхватит только новый процесс: если приложение не на экране и машина на парковке,
     * процесс завершается, и система сразу поднимает сервис заново. Иначе код подхватится при
     * следующем запуске.
     */
    fun finishUpdateIfNeeded(isUiVisible: () -> Boolean, isParked: () -> Boolean) {
        updateDir.deleteRecursively()
        if (prefs.getInt(KEY_PENDING_COMPILE, 0) != installedVersionCode) return
        prefs.edit().remove(KEY_PENDING_COMPILE).apply()
        if (!selfUpdateSupported) return
        Thread({
            if (!precompile()) return@Thread
            if (!isUiVisible() && isParked()) {
                Log.i(TAG, "restarting to pick up precompiled code")
                Process.killProcess(Process.myPid())
            }
        }, "update-precompile").apply { isDaemon = true }.start()
    }

    private fun precompile(): Boolean {
        return try {
            val process = ProcessBuilder("cmd", "package", "compile", "-m", "speed", "-f", appContext.packageName)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val exit = process.waitFor()
            Log.i(TAG, "precompile exit=$exit output=$output")
            exit == 0 && output.contains("Success", ignoreCase = true)
        } catch (t: Throwable) {
            Log.w(TAG, "precompile failed", t)
            false
        }
    }

    private fun fail(message: String, info: UpdateInfo?) {
        prefs.edit().remove(KEY_PENDING_COMPILE).apply()
        _state.value = UpdateState.Failed(message, info)
    }

    private companion object {
        const val TAG = "Ex2RangeUpdate"
        const val PREFS_NAME = "update"
        const val KEY_LAST_CHECK_MS = "lastCheckMs"
        const val KEY_PENDING_COMPILE = "pendingCompileVersionCode"
        const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
