package com.flowspark.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowspark.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "flowspark_settings")

/**
 * AI 供应商配置（v2.1 核心：可运行时切换，零代码改动）。
 */
data class AiProviderConfig(
    val baseUrl: String = BuildConfig.AI_PROXY_BASE_URL,
    val apiKey: String = BuildConfig.AI_PROXY_API_KEY,
    val llmModel: String = BuildConfig.DEFAULT_LLM_MODEL,
    val imageModel: String = BuildConfig.DEFAULT_IMAGE_MODEL,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("ai_base_url")
        val API_KEY = stringPreferencesKey("ai_api_key")
        val LLM_MODEL = stringPreferencesKey("ai_llm_model")
        val IMAGE_MODEL = stringPreferencesKey("ai_image_model")
        val USE_PROXY = booleanPreferencesKey("use_proxy")
        val LOW_POWER_WARNING = booleanPreferencesKey("low_power_warning")
    }

    val aiProvider: Flow<AiProviderConfig> = context.dataStore.data.map { prefs ->
        AiProviderConfig(
            baseUrl = prefs[Keys.BASE_URL] ?: BuildConfig.AI_PROXY_BASE_URL,
            apiKey = prefs[Keys.API_KEY] ?: BuildConfig.AI_PROXY_API_KEY,
            llmModel = prefs[Keys.LLM_MODEL] ?: BuildConfig.DEFAULT_LLM_MODEL,
            imageModel = prefs[Keys.IMAGE_MODEL] ?: BuildConfig.DEFAULT_IMAGE_MODEL,
        )
    }

    val lowPowerWarning: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOW_POWER_WARNING] ?: true
    }

    suspend fun updateProvider(provider: AiProviderConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = provider.baseUrl
            prefs[Keys.API_KEY] = provider.apiKey
            prefs[Keys.LLM_MODEL] = provider.llmModel
            prefs[Keys.IMAGE_MODEL] = provider.imageModel
        }
    }

    suspend fun setLowPowerWarning(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOW_POWER_WARNING] = enabled
        }
    }
}
