package com.geely.ex2.range.data.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.geely.ex2.range.app.RangeApplication
import java.io.File

/**
 * Ставит APK поверх приложения через [PackageInstaller]. Сборка для ГУ работает под uid system,
 * поэтому система ставит обновление молча; подпись и package проверяет она же. Итог приходит в
 * [UpdateInstallReceiver] — при успехе процесс к этому моменту обычно уже завершён.
 */
internal class ApkInstaller(private val context: Context) {
    fun install(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                session.openWrite(SESSION_APK_NAME, 0, apk.length()).use { output ->
                    apk.inputStream().use { input -> input.copyTo(output) }
                    session.fsync(output)
                }
                val intent = Intent(context, UpdateInstallReceiver::class.java).setAction(ACTION_INSTALL_RESULT)
                // С API 31 PendingIntent обязан объявить изменяемость; системе нужно дописать статус.
                val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val result = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or mutable,
                )
                session.commit(result.intentSender)
            }
        } catch (t: Throwable) {
            runCatching { installer.abandonSession(sessionId) }
            throw t
        }
    }

    companion object {
        const val ACTION_INSTALL_RESULT = "com.geely.ex2.range.action.UPDATE_INSTALL_RESULT"
        private const val SESSION_APK_NAME = "base.apk"
    }
}

/** Итог установки обновления от системы. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ApkInstaller.ACTION_INSTALL_RESULT) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.i(TAG, "install status=$status message=$message")
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> Unit
            // Не системная установка: система просит пользователя подтвердить.
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            else -> {
                val container = (context.applicationContext as? RangeApplication)?.container ?: return
                container.updates.onInstallFailed(message)
            }
        }
    }

    private companion object {
        const val TAG = "Ex2RangeUpdate"
    }
}
