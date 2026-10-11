package com.geely.ex2.range.data.update

import com.geely.ex2.range.BuildConfig
import com.geely.ex2.range.domain.model.UpdateInfo
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Откуда берутся обновления: файлы последнего релиза репозитория на GitHub. Адрес зашит в
 * приложение, манифест задаёт только имя APK в том же релизе.
 */
internal object UpdateSource {
    const val BASE_URL = "https://github.com/ArtsiomDziominski/geely_ex2_live_range/releases/latest/download/"
    const val MANIFEST_NAME = "update.json"
}

/** Файл обновления не совпал с манифестом — ставить его нельзя. */
internal class CorruptUpdateException(message: String) : IOException(message)

internal class UpdateClient(private val baseUrl: String = UpdateSource.BASE_URL) {
    /**
     * Манифест последнего релиза. Null — релиза с манифестом нет (ещё ничего не опубликовано).
     * @throws IOException нет связи, ответ сервера не разобран.
     */
    fun fetchManifest(): UpdateInfo? {
        val connection = open(baseUrl + UpdateSource.MANIFEST_NAME)
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> return null
                else -> throw IOException("HTTP $code")
            }
            val text = connection.inputStream.use { readLimited(it, MAX_MANIFEST_BYTES) }
            val json = try {
                JSONObject(text)
            } catch (e: JSONException) {
                throw IOException("update.json: не JSON", e)
            }
            return UpdateInfo.parse { key -> json.opt(key) }
                ?: throw IOException("update.json: неверные поля")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Качает APK релиза в [target], сверяя размер и SHA-256 с манифестом.
     * @throws CorruptUpdateException файл не совпал с манифестом.
     * @throws IOException нет связи.
     */
    fun download(info: UpdateInfo, target: File, onProgress: (Float?) -> Unit) {
        val connection = open(baseUrl + info.apkName)
        val partial = File(target.parentFile, target.name + ".part")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            target.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    copyVerified(input, output, info.sha256, info.sizeBytes, onProgress)
                }
            }
            target.delete()
            if (!partial.renameTo(target)) throw IOException("не удалось сохранить файл обновления")
        } finally {
            partial.delete()
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "geely-ex2-range/" + BuildConfig.VERSION_NAME)
        return connection
    }

    private fun readLimited(input: InputStream, limit: Int): String {
        val collected = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            collected.write(buffer, 0, read)
            if (collected.size() > limit) throw IOException("update.json слишком большой")
        }
        return collected.toString("UTF-8")
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_MANIFEST_BYTES = 64 * 1024
    }
}

/**
 * Копирует поток, считая SHA-256 на лету. Если манифест знает размер ([expectedSize] > 0), лишние
 * байты обрывают загрузку сразу, а не после того, как диск заполнен.
 * @throws CorruptUpdateException размер или контрольная сумма не совпали.
 */
internal fun copyVerified(
    input: InputStream,
    output: OutputStream,
    expectedSha256: String,
    expectedSize: Long,
    onProgress: (Float?) -> Unit = {},
) {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    var reportedPercent = -1
    onProgress(if (expectedSize > 0L) 0f else null)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (expectedSize > 0L && total > expectedSize) {
            throw CorruptUpdateException("файл обновления больше, чем указано в манифесте")
        }
        digest.update(buffer, 0, read)
        output.write(buffer, 0, read)
        if (expectedSize > 0L) {
            val percent = (total * 100 / expectedSize).toInt()
            if (percent != reportedPercent) {
                reportedPercent = percent
                onProgress(percent / 100f)
            }
        }
    }
    if (expectedSize > 0L && total != expectedSize) {
        throw CorruptUpdateException("файл обновления загружен не полностью")
    }
    val actual = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    if (!actual.equals(expectedSha256, ignoreCase = true)) {
        throw CorruptUpdateException("контрольная сумма файла обновления не совпала")
    }
}
