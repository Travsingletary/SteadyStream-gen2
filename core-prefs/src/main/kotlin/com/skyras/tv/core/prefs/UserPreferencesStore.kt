package com.skyras.tv.core.prefs

interface UserPreferencesStore {
    suspend fun get(username: String): UserPreferencesRecord?
    suspend fun upsert(record: UserPreferencesRecord)
}

class InMemoryUserPreferencesStore : UserPreferencesStore {
    private val records = linkedMapOf<String, UserPreferencesRecord>()

    override suspend fun get(username: String): UserPreferencesRecord? {
        return records[username.trim().lowercase()]
    }

    override suspend fun upsert(record: UserPreferencesRecord) {
        val key = record.username.trim().lowercase()
        records[key] = record.copy(username = record.username.trim())
    }

    fun clear() {
        records.clear()
    }

    fun size(): Int = records.size
}

/**
 * Loads remote first (when configured), always mirrors to local so the APK
 * works without Supabase keys and still hydrates after a later key is added.
 */
class UserPreferencesRepository(
    private val local: UserPreferencesStore,
    private val remote: UserPreferencesStore? = null
) {
    suspend fun load(username: String): UserPreferencesRecord? {
        val remoteStore = remote
        val remoteRecord = if (remoteStore != null) {
            runCatching { remoteStore.get(username) }.getOrNull()
        } else {
            null
        }
        if (remoteRecord != null) {
            local.upsert(remoteRecord)
            return remoteRecord
        }
        return local.get(username)
    }

    suspend fun save(record: UserPreferencesRecord) {
        val stamped = record.copy(
            updatedAtEpochMillis = if (record.updatedAtEpochMillis > 0) {
                record.updatedAtEpochMillis
            } else {
                System.currentTimeMillis()
            }
        )
        local.upsert(stamped)
        val remoteStore = remote
        if (remoteStore != null) {
            runCatching { remoteStore.upsert(stamped) }
        }
    }
}
