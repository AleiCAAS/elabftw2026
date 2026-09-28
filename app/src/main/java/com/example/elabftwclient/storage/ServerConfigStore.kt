package com.example.elabftwclient.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "elabftw_config")

class ServerConfigStore(private val context: Context) {
    private val keyServerUrl = stringPreferencesKey("server_url")

    val serverUrlFlow: Flow<String> = context.dataStore.data.map { it[keyServerUrl] ?: "" }

    suspend fun getServerUrl(): String = context.dataStore.data.first()[keyServerUrl] ?: ""

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[keyServerUrl] = url.trim().trimEnd('/') }
    }
}