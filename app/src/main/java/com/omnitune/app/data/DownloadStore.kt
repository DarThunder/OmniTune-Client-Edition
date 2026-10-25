package com.omnitune.app.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class DownloadStore @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val sp = ctx.getSharedPreferences("downloads", Context.MODE_PRIVATE)

    private val _uris = MutableStateFlow(
        sp.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
    )
    val uris: StateFlow<Map<String, String>> = _uris.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _uris.value.forEach { (key, uri) -> if (!exists(uri.toUri())) remove(key) }
        }
    }

    fun put(key: String, uri: Uri) {
        sp.edit { putString(key, uri.toString()) }
        _uris.update { it + (key to uri.toString()) }
    }

    fun thumbFile(key: String): java.io.File? =
        if (key in _uris.value) java.io.File(ctx.filesDir, "thumbs/$key").takeIf { it.exists() } else null

    fun remove(key: String) {
        sp.edit { remove(key) }
        _uris.update { it - key }
        java.io.File(ctx.filesDir, "thumbs/$key").delete()
    }

    fun localUri(key: String): Uri? = _uris.value[key]?.toUri()

    private fun exists(uri: Uri): Boolean = try {
        ctx.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
    }
}