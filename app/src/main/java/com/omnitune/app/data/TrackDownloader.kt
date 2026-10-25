package com.omnitune.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.omnitune.app.data.net.OmniApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException


data class Saved(val name: String, val uri: Uri)
object TrackDownloader {

    private val invalidChars = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun saveToDownloads(context: Context, api: OmniApi, track: Track): Result<Saved> =
        withContext(Dispatchers.IO) {
            runCatching {
                val saved = saveAudio(context, api, track)
                saveThumbnail(context, api, track)
                saved
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveAudio(context: Context, api: OmniApi, track: Track): Saved {
        val resp = api.downloadFile(track.key)
        if (!resp.isSuccessful) error("Error ${resp.code()}")
        val body = resp.body() ?: error("Respuesta vacía")

        val name = "${track.displayArtist} - ${track.displayTitle}"
            .replace(invalidChars, "").trim().ifBlank { "track" } + ".opus"

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "audio/ogg")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Omnitune")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo")
        try {
            resolver.openOutputStream(uri)!!.use { out -> body.byteStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return Saved(name, uri)
    }

    private suspend fun saveThumbnail(context: Context, api: OmniApi, track: Track) {
        try {
            val resp = api.thumbnail(track.key)
            val body = resp.body()?.takeIf { resp.isSuccessful } ?: return
            val dir = File(context.filesDir, "thumbs").apply { mkdirs() }
            val tmp = File(dir, "${track.key}.tmp")
            body.byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
            tmp.renameTo(File(dir, track.key))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
        }
    }
}