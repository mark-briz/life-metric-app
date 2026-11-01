package com.markbriz.lifemetric.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** The on-disk file exists but could not be read. Callers must not replace it. */
class UnreadableLocalDataException(cause: Throwable) : Exception("Couldn't read saved data.", cause)

class LocalRepository internal constructor(private val file: File) {
    constructor(context: Context) : this(File(context.filesDir, "life-metric-local.json"))

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /**
     * Loads the local file, migrating the pre-1.1 multi-account layout if present.
     * A missing file starts empty. A file that exists but cannot be read throws, so a later
     * save cannot replace it with starter plans.
     */
    @Synchronized
    fun load(): LocalFile {
        if (!file.exists()) return fresh()
        val raw = try {
            file.readText()
        } catch (err: IOException) {
            throw UnreadableLocalDataException(err)
        }
        return try {
            decode(raw)
        } catch (err: UnreadableLocalDataException) {
            throw err
        } catch (err: SerializationException) {
            throw UnreadableLocalDataException(err)
        } catch (err: IllegalArgumentException) {
            throw UnreadableLocalDataException(err)
        }
    }

    /**
     * Replaces the file only after the new contents are flushed. A crash while writing leaves
     * the previous file in place; [File.writeText] would have already truncated it.
     */
    @Synchronized
    fun save(data: LocalFile) {
        val directory = file.parentFile ?: throw IOException("Data file has no directory.")
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Couldn't create the data directory.")
        }
        val tmp = File(directory, "${file.name}.tmp")
        var published = false
        try {
            FileOutputStream(tmp).use { out ->
                out.write(json.encodeToString(LocalFile.serializer(), data).toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            if (!tmp.renameTo(file)) throw IOException("Couldn't replace saved data.")
            published = true
        } finally {
            if (!published) tmp.delete()
        }
    }

    @Synchronized
    fun mutate(transform: (FitnessStore) -> FitnessStore): LocalFile {
        val next = load().let { it.copy(store = transform(it.store)) }
        save(next)
        return next
    }

    @Synchronized
    fun acceptDisclaimer(): LocalFile {
        val next = load().copy(disclaimerAccepted = true)
        save(next)
        return next
    }

    /** Removes every entry and resets to the starter plans. The disclaimer acceptance is kept. */
    @Synchronized
    fun eraseAll(): LocalFile {
        val next = load().copy(store = starterStore())
        save(next)
        return next
    }

    private fun fresh() = LocalFile(store = starterStore())

    private fun starterStore() = FitnessStore(plans = ActivityCatalog.starterPlans(), themeId = "mint")

    private fun decode(raw: String): LocalFile {
        val root = json.parseToJsonElement(raw).jsonObject
        if (root.containsKey("accounts")) {
            val migrated = migrateLegacy(raw)
            save(migrated)
            return migrated
        }
        return json.decodeFromString(LocalFile.serializer(), raw)
    }

    /**
     * Pre-1.1 builds kept a list of password-protected accounts. Carry over the account that was
     * signed in (or the first one), and treat the disclaimer as accepted since sign-up required it.
     * A file that cannot be decoded throws instead of being replaced with starter plans.
     */
    private fun migrateLegacy(raw: String): LocalFile {
        val legacy = json.decodeFromString<LegacyAccountsFile>(raw)
        val account = legacy.accounts.find { it.id == legacy.sessionUserId } ?: legacy.accounts.firstOrNull()
        return LocalFile(
            store = account?.store ?: starterStore(),
            disclaimerAccepted = legacy.accounts.isNotEmpty(),
        )
    }

    @Serializable
    private data class LegacyAccount(val id: String, val store: FitnessStore)

    @Serializable
    private data class LegacyAccountsFile(
        val accounts: List<LegacyAccount> = emptyList(),
        val sessionUserId: String? = null,
    )
}
