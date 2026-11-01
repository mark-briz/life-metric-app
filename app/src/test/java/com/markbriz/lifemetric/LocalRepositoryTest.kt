package com.markbriz.lifemetric

import com.markbriz.lifemetric.data.LocalRepository
import com.markbriz.lifemetric.data.UnreadableLocalDataException
import com.markbriz.lifemetric.data.WeightRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocalRepositoryTest {
    @Test
    fun saveThenLoadRoundTripsAndRemovesTheTempFile() {
        val dir = tempDir()
        val file = File(dir, "life-metric-local.json")
        val repo = LocalRepository(file)
        val saved = repo.mutate { store ->
            store.copy(
                profile = store.profile.copy(firstName = "Ada", lastName = "Lovelace"),
                weightRecords = listOf(WeightRecord("bw-1", "2026-10-02", "08:15", 180.0)),
            )
        }
        val loaded = repo.load()
        assertEquals("Ada", loaded.store.profile.firstName)
        assertEquals(saved.store.weightRecords, loaded.store.weightRecords)
        assertEquals(saved.store.plans, loaded.store.plans)
        assertFalse(File(dir, "life-metric-local.json.tmp").exists())
        assertTrue(file.readText().contains("Ada"))
    }

    @Test
    fun missingFileDoesNotCreateStarterData() {
        val file = File(tempDir(), "life-metric-local.json")
        val repo = LocalRepository(file)
        assertFalse(repo.load().disclaimerAccepted)
        assertFalse(file.exists())
    }

    @Test
    fun unreadableFileIsNotReplacedByStarterPlans() {
        val file = File(tempDir(), "life-metric-local.json")
        val original = """{"store":"""
        file.writeText(original)
        val repo = LocalRepository(file)
        assertThrowsUnreadable { repo.load() }
        assertThrowsUnreadable { repo.mutate { it } }
        assertThrowsUnreadable { repo.acceptDisclaimer() }
        assertEquals(original, file.readText())
        assertFalse(File(file.parentFile, "life-metric-local.json.tmp").exists())
    }

    @Test
    fun legacyAccountThatCannotBeDecodedIsLeftUntouched() {
        val file = File(tempDir(), "life-metric-local.json")
        val original = """{"accounts":[{"id":"a","store":{"profile":{"firstName":1}}}],"sessionUserId":"a"}"""
        file.writeText(original)
        val repo = LocalRepository(file)
        assertThrowsUnreadable { repo.load() }
        assertEquals(original, file.readText())
    }

    @Test
    fun legacySignedInAccountIsMigrated() {
        val file = File(tempDir(), "life-metric-local.json")
        file.writeText(
            """
            {
              "accounts": [
                {"id":"other","store":{"profile":{"firstName":"Grace","lastName":"Hopper"},"themeId":"dark"}},
                {"id":"signed-in","store":{"profile":{"firstName":"Ada","lastName":"Lovelace"},"themeId":"ocean"}}
              ],
              "sessionUserId": "signed-in"
            }
            """.trimIndent(),
        )
        val repo = LocalRepository(file)
        val migrated = repo.load()
        assertTrue(migrated.disclaimerAccepted)
        assertEquals("Ada", migrated.store.profile.firstName)
        assertEquals("ocean", migrated.store.themeId)
        val again = repo.load()
        assertEquals("Ada", again.store.profile.firstName)
        assertEquals("ocean", again.store.themeId)
        assertFalse(file.readText().contains("accounts"))
    }

    private fun tempDir(): File = java.nio.file.Files.createTempDirectory("life-metric-repo").toFile()

    private fun assertThrowsUnreadable(block: () -> Unit) {
        try {
            block()
            org.junit.Assert.fail("Expected unreadable data")
        } catch (err: UnreadableLocalDataException) {
            assertEquals("Couldn't read saved data.", err.message)
        }
    }
}
