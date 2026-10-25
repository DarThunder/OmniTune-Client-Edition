package com.omnitune.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryCache @Inject constructor(@ApplicationContext ctx: Context) {
    private val dir = File(ctx.filesDir, "library").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun <T> read(name: String, ser: KSerializer<T>): T? = try {
        File(dir, name).takeIf { it.exists() }?.readText()?.let { json.decodeFromString(ser, it) }
    } catch (e: Exception) {
        null
    }

    suspend fun <T> write(name: String, ser: KSerializer<T>, value: T) = withContext(Dispatchers.IO) {
        try {
            val tmp = File(dir, "$name.tmp")
            tmp.writeText(json.encodeToString(ser, value))
            tmp.renameTo(File(dir, name))
        } catch (_: Exception) {
        }
    }

    fun delete(name: String) { File(dir, name).delete() }

    fun coverFile(playlistId: String) = File(dir, "cover_$playlistId.webp")

    fun clear() { dir.listFiles()?.forEach { it.delete() } }
}