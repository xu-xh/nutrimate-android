package com.nutrimate.app.data.repository

import com.nutrimate.app.data.local.preferences.PreferencesDataSource
import com.nutrimate.app.data.security.KeyStoreSecretStore
import com.nutrimate.app.domain.repository.AiConfig
import com.nutrimate.app.domain.repository.AiConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiConfigRepositoryImpl @Inject constructor(
    private val preferences: PreferencesDataSource,
    private val keyStore: KeyStoreSecretStore
) : AiConfigRepository {

    override fun observeConfig(): Flow<AiConfig> =
        combine(
            preferences.observeAiTransport(),
            preferences.observeHasApiKey()
        ) { (provider, model, baseUrl), hasKey ->
            AiConfig(
                provider = provider,
                model = model,
                baseUrl = baseUrl,
                hasApiKey = hasKey
            )
        }

    override suspend fun saveTransport(provider: String, model: String, baseUrl: String?) {
        preferences.saveAiTransport(provider, model, baseUrl)
    }

    override suspend fun saveApiKey(key: String) {
        preferences.saveEncryptedApiKey(keyStore.write(key))
    }

    override suspend fun readApiKey(): String? {
        val blob = preferences.readEncryptedApiKey() ?: return null
        return keyStore.read(blob)
    }
}