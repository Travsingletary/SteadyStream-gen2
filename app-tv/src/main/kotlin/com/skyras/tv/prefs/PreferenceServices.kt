package com.skyras.tv.prefs

import android.content.Context
import com.skyras.tv.BuildConfig
import com.skyras.tv.core.prefs.SupabaseConfig
import com.skyras.tv.core.prefs.SupabaseUserPreferencesClient
import com.skyras.tv.core.prefs.UserPreferencesRepository
import com.skyras.tv.core.prefs.UserPreferencesStore

object PreferenceServices {
    fun repository(context: Context): UserPreferencesRepository {
        val local = SharedPreferencesUserStore(context)
        val remote: UserPreferencesStore? = supabaseClientOrNull()
        return UserPreferencesRepository(local = local, remote = remote)
    }

    fun supabaseClientOrNull(): SupabaseUserPreferencesClient? {
        val config = SupabaseConfig(
            url = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY
        )
        if (!config.isConfigured()) return null
        return SupabaseUserPreferencesClient(config)
    }
}
