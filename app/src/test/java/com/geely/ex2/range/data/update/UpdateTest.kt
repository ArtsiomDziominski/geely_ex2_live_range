package com.geely.ex2.range.data.update

import com.geely.ex2.range.domain.model.UpdateInfo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class UpdateInfoTest {
    private val sha = "a".repeat(64)

    private fun manifest(vararg overrides: Pair<String, Any?>): (String) -> Any? {
        val fields = mutableMapOf<String, Any?>(
            "versionCode" to 30,
            "versionName" to "0.1.1",
            "apk" to "geely-ex2-range-system-platform-signed.apk",
            "sha256" to sha,
            "sizeBytes" to 3_456_789L,
            "notes" to "  Зарядка по данным машины  ",
        )
        overrides.forEach { (key, value) -> fields[key] = value }
        return fields::get
    }

    @Test
    fun parsesValidManifest() {
        val info = UpdateInfo.parse(manifest())!!
        assertEquals(30, info.versionCode)
        assertEquals("0.1.1", info.versionName)
        assertEquals("geely-ex2-range-system-platform-signed.apk", info.apkName)
        assertEquals(sha, info.sha256)
        assertEquals(3_456_789L, info.sizeBytes)
        assertEquals("Зарядка по данным машины", info.notes)
        assertEquals("0.1.1 (30)", info.label)
    }

    @Test
    fun newerOnlyWhenVersionCodeIsHigher() {
        val info = UpdateInfo.parse(manifest())!!
        assertTrue(info.isNewerThan(29))
        assertFalse(info.isNewerThan(30))
        assertFalse(info.isNewerThan(31))
    }

    /** Манифест задаёт только имя файла в том же релизе — увести загрузку в сторону он не должен. */
    @Test
    fun rejectsApkNameThatIsNotAPlainFileName() {
        for (name in listOf(
            "https://evil.example/app.apk",
            "../other/app.apk",
            "sub/app.apk",
            "app.apk?x=1",
            "app.zip",
            ".apk",
            "",
        )) {
            assertNull(name, UpdateInfo.parse(manifest("apk" to name)))
        }
    }

    @Test
    fun rejectsBrokenFields() {
        assertNull(UpdateInfo.parse(manifest("versionCode" to 0)))
        assertNull(UpdateInfo.parse(manifest("versionCode" to "30")))
        assertNull(UpdateInfo.parse(manifest("versionName" to " ")))
        assertNull(UpdateInfo.parse(manifest("sha256" to "abc")))
        assertNull(UpdateInfo.parse(manifest("sha256" to null)))
        assertNull(UpdateInfo.parse(manifest("sizeBytes" to -1)))
        assertNull(UpdateInfo.parse(manifest("sizeBytes" to 500L * 1024 * 1024)))
    }

    @Test
    fun optionalFieldsHaveDefaults() {
        val info = UpdateInfo.parse(manifest("sizeBytes" to null, "notes" to null, "sha256" to sha.uppercase()))
        assertNotNull(info)
        assertEquals(0L, info!!.sizeBytes)
        assertEquals("", info.notes)
        assertEquals(sha, info.sha256)
    }
}

class CopyVerifiedTest {
    private val payload = ByteArray(200_000) { index -> (index % 251).toByte() }
    private val sha = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }

    @Test
    fun copiesMatchingFileAndReportsProgress() {
        val out = ByteArrayOutputStream()
        val progress = mutableListOf<Float?>()
        copyVerified(ByteArrayInputStream(payload), out, sha, payload.size.toLong(), progress::add)
        assertArrayEquals(payload, out.toByteArray())
        assertEquals(0f, progress.first()!!, 0f)
        assertEquals(1f, progress.last()!!, 0f)
        assertEquals(progress.filterNotNull().sorted(), progress.filterNotNull())
    }

    @Test
    fun unknownSizeStillChecksTheHash() {
        val progress = mutableListOf<Float?>()
        copyVerified(ByteArrayInputStream(payload), ByteArrayOutputStream(), sha.uppercase(), 0L, progress::add)
        assertEquals(listOf<Float?>(null), progress)
    }

    @Test
    fun rejectsWrongHash() {
        assertThrows(CorruptUpdateException::class.java) {
            copyVerified(ByteArrayInputStream(payload), ByteArrayOutputStream(), "0".repeat(64), payload.size.toLong())
        }
    }

    @Test
    fun rejectsTruncatedFile() {
        assertThrows(CorruptUpdateException::class.java) {
            copyVerified(ByteArrayInputStream(payload), ByteArrayOutputStream(), sha, payload.size + 1L)
        }
    }

    /** Лишние байты обрывают загрузку сразу — до того, как файл целиком лёг на диск. */
    @Test
    fun stopsAsSoonAsFileIsLargerThanDeclared() {
        val out = ByteArrayOutputStream()
        assertThrows(CorruptUpdateException::class.java) {
            copyVerified(ByteArrayInputStream(payload), out, sha, 1_000L)
        }
        assertTrue(out.size() < payload.size)
    }
}
