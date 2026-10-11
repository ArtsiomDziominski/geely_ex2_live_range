package com.geely.ex2.range.domain.model

/** Сборка, опубликованная в релизе: содержимое его `update.json`. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    /** Имя файла APK в том же релизе — только имя, без пути и адреса. */
    val apkName: String,
    /** SHA-256 файла APK, 64 шестнадцатеричных символа в нижнем регистре. */
    val sha256: String,
    /** Размер APK в байтах; 0 — в манифесте не указан. */
    val sizeBytes: Long,
    val notes: String,
) {
    val label: String
        get() = "$versionName ($versionCode)"

    fun isNewerThan(installedVersionCode: Int): Boolean = versionCode > installedVersionCode

    companion object {
        private val APK_NAME = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*\\.apk$")
        private val SHA_256 = Regex("^[0-9a-f]{64}$")
        private const val MAX_APK_BYTES = 200L * 1024 * 1024
        private const val MAX_NAME_LENGTH = 40
        private const val MAX_NOTES_LENGTH = 2_000

        /**
         * Разбор манифеста: [field] отдаёт значение по ключу. Null — манифест не годится.
         * Имя APK проверяется строго: приложение качает файл только из своего релиза, и манифест
         * не должен уметь увести загрузку на другой адрес или путь.
         */
        fun parse(field: (String) -> Any?): UpdateInfo? {
            val versionCode = (field("versionCode") as? Number)?.toInt()?.takeIf { it > 0 } ?: return null
            val versionName = (field("versionName") as? String)
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it.length <= MAX_NAME_LENGTH }
                ?: return null
            val apkName = (field("apk") as? String)?.takeIf(APK_NAME::matches) ?: return null
            val sha256 = (field("sha256") as? String)?.trim()?.lowercase()?.takeIf(SHA_256::matches) ?: return null
            val sizeBytes = (field("sizeBytes") as? Number)?.toLong() ?: 0L
            if (sizeBytes < 0L || sizeBytes > MAX_APK_BYTES) return null
            val notes = (field("notes") as? String)?.trim().orEmpty().take(MAX_NOTES_LENGTH)
            return UpdateInfo(versionCode, versionName, apkName, sha256, sizeBytes, notes)
        }
    }
}

/** Что сейчас с обновлением — для блока в «Настройках» и отметки на пункте меню. */
sealed interface UpdateState {
    /** С запуска ещё не проверяли. */
    data object Idle : UpdateState

    data object Checking : UpdateState

    /** Проверили: новее установленной сборки ничего не опубликовано. */
    data object UpToDate : UpdateState

    data class Available(val info: UpdateInfo) : UpdateState

    /** [progress] 0…1; null — размер файла неизвестен. */
    data class Downloading(val info: UpdateInfo, val progress: Float?) : UpdateState

    /** Файл проверен и передан системе; на этом шаге приложение перезапускается. */
    data class Installing(val info: UpdateInfo) : UpdateState

    /** [info] не null — проверка прошла, не удалась загрузка или установка: её можно повторить. */
    data class Failed(val message: String, val info: UpdateInfo?) : UpdateState
}
