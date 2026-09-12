package com.geely.ex2.range.data.store

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi

/**
 * Mirrors small JSON blobs into the shared Downloads collection (`Download/GeelyEX2Range/`) so
 * they survive an app uninstall — unlike files under [Context.getFilesDir], which Android deletes
 * together with the app. Reading/writing rows this app itself created needs no runtime permission
 * on API 29+ (scoped storage exempts an app's own MediaStore entries); older devices are skipped
 * entirely — [isSupported] tells callers whether to bother, and every call is best-effort so a
 * failure here never breaks the normal internal-storage save/load path.
 */
class SharedJsonStore(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/GeelyEX2Range/"

    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun read(displayName: String): String? {
        if (!isSupported) return null
        return try {
            readInternal(displayName)
        } catch (_: Throwable) {
            null
        }
    }

    fun write(displayName: String, text: String) {
        if (!isSupported) return
        try {
            writeInternal(displayName, text)
        } catch (_: Throwable) {
            // Best-effort mirror — internal storage stays the working copy either way.
        }
    }

    fun delete(displayName: String) {
        if (!isSupported) return
        try {
            findUri(displayName)?.let { resolver.delete(it, null, null) }
        } catch (_: Throwable) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readInternal(displayName: String): String? {
        val uri = findUri(displayName) ?: return null
        return resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeInternal(displayName: String, text: String) {
        val uri = findUri(displayName) ?: insertUri(displayName) ?: return
        resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findUri(displayName: String): Uri? {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val args = arrayOf(displayName, relativePath)
        resolver.query(collection, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertUri(displayName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        }
        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
    }
}
