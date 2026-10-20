package com.markbriz.lifemetric.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

class LocalRepository(context: Context) {
    private val file = File(context.filesDir, "life-metric-local.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** Loads the local file, migrating the pre-1.1 multi-account layout if present. */
    @Synchronized
    fun load(): LocalFile {
        if (!file.exists()) return fresh()
        val raw = runCatching { file.readText() }.getOrNull() ?: return fresh()
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return fresh()
        if (root.containsKey("accounts")) {
            val migrated = migrateLegacy(raw)
            save(migrated)
            return migrated
        }
        return runCatching { json.decodeFromString<LocalFile>(raw) }.getOrElse { fresh() }
    }

    @Synchronized
    fun save(data: LocalFile) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(LocalFile.serializer(), data))
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

    /**
     * Pre-1.1 builds kept a list of password-protected accounts. Carry over the account that was
     * signed in (or the first one), and treat the disclaimer as accepted since sign-up required it.
     */
    private fun migrateLegacy(raw: String): LocalFile {
        val legacy = runCatching { json.decodeFromString<LegacyAccountsFile>(raw) }.getOrNull() ?: return fresh()
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
