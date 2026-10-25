package com.omnitune.app.data

import com.omnitune.app.data.net.OmniApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@Singleton
class Repository @Inject constructor(
    private val api: OmniApi,
    private val prefs: Prefs,
    private val downloads: DownloadStore,
    private val cache: LibraryCache,
) {
    val downloaded: StateFlow<Map<String, String>> = downloads.uris

    fun localUri(key: String): Uri? = downloads.localUri(key)
    private val _favorites = MutableStateFlow<List<Track>>(emptyList())
    val favorites: StateFlow<List<Track>> = _favorites.asStateFlow()

    private val _favoriteKeys = MutableStateFlow<Set<String>>(emptySet())
    val favoriteKeys: StateFlow<Set<String>> = _favoriteKeys.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistSummary>>(emptyList())
    val playlists: StateFlow<List<PlaylistSummary>> = _playlists.asStateFlow()

    init {
        cache.read("playlists.json", ListSerializer(PlaylistSummary.serializer()))
            ?.let { _playlists.value = it }
        cache.read("favorites.json", ListSerializer(Track.serializer()))?.let { list ->
            _favorites.value = list
            _favoriteKeys.value = list.map { it.key }.toSet()
        }
    }

    private fun base() = prefs.serverUrl.trimEnd('/')
    fun streamUrl(key: String) = "${base()}/music/$key"
    fun thumbUrl(key: String): String =
        downloads.thumbFile(key)?.let { Uri.fromFile(it).toString() }
            ?: "${base()}/thumbnail/$key"

    fun absolute(path: String?): String? = path?.let { if (it.startsWith("http")) it else base() + it }

    suspend fun refreshFavorites() {
        val list = try { api.favorites().favorites } catch (e: IOException) { return }
        _favorites.value = list
        _favoriteKeys.value = list.map { it.key }.toSet()
        cache.write("favorites.json", ListSerializer(Track.serializer()), list)
    }

    suspend fun toggleFavorite(track: Track) {
        val key = track.key
        val was = key in _favoriteKeys.value
        _favoriteKeys.update { if (was) it - key else it + key }
        try {
            if (was) api.removeFavorite(key) else api.addFavorite(key)
            refreshFavorites()
        } catch (e: Exception) {
            _favoriteKeys.update { if (was) it + key else it - key }
            throw e
        }
    }

    suspend fun downloadTrack(context: Context, track: Track): String {
        val saved = TrackDownloader.saveToDownloads(context, api, track).getOrThrow()
        downloads.put(track.key, saved.uri)
        return saved.name
    }

    suspend fun refreshPlaylists() {
        val list = try { api.playlists().playlists } catch (e: IOException) { return }
        _playlists.value = list
        cache.write("playlists.json", ListSerializer(PlaylistSummary.serializer()), list)
    }

    fun cachedPlaylist(id: String): PlaylistDetail? =
        cache.read("playlist_$id.json", PlaylistDetail.serializer())

    suspend fun playlist(id: String): PlaylistDetail =
        try {
            api.playlist(id).also { cache.write("playlist_$id.json", PlaylistDetail.serializer(), it) }
        } catch (e: IOException) {
            withContext(Dispatchers.IO) { cachedPlaylist(id) } ?: throw e
        }

    suspend fun createPlaylist(name: String): String {
        val id = api.createPlaylist(CreatePlaylistRequest(name.trim())).id
        refreshPlaylists()
        return id
    }

    suspend fun addToPlaylist(playlistId: String, track: Track) {
        api.addToPlaylist(playlistId, track.key)
        refreshPlaylists()
    }

    suspend fun removeFromPlaylist(playlistId: String, track: Track) {
        api.removeFromPlaylist(playlistId, track.key)
        refreshPlaylists()
    }

    suspend fun deletePlaylist(playlistId: String) {
        api.deletePlaylist(playlistId)
        cache.delete("playlist_$playlistId.json")
        refreshPlaylists()
    }

    private val _coverRev = MutableStateFlow(0)
    val coverRev: StateFlow<Int> = _coverRev.asStateFlow()

    fun playlistCover(id: String): String? =
        cache.coverFile(id).takeIf { it.exists() }?.let { Uri.fromFile(it).toString() }

    suspend fun syncCover(id: String) = withContext(Dispatchers.IO) {
        val f = cache.coverFile(id)
        try {
            val r = api.playlistCover(id)
            val body = r.body()
            if (r.isSuccessful && body != null) {
                val tmp = File(f.parentFile, "${f.name}.tmp")
                body.byteStream().use { input -> tmp.outputStream().use { input.copyTo(it) } }
                tmp.renameTo(f)
            } else if (r.code() == 404) {
                f.delete()
            }
        } catch (e: IOException) {
        }
        _coverRev.update { it + 1 }
    }

    suspend fun syncCovers() {
        _playlists.value.filter { it.coverUrl != null }.forEach { syncCover(it.id) }
    }

    suspend fun uploadCover(context: Context, id: String, uri: Uri) {
        val bytes = withContext(Dispatchers.IO) { encodeCover(context, uri) }
        val part = MultipartBody.Part.createFormData(
            "file", "cover.jpg", bytes.toRequestBody("image/jpeg".toMediaType())
        )
        api.uploadPlaylistCover(id, part)
        syncCover(id)
        refreshPlaylists()
    }

    suspend fun deleteCover(id: String) {
        api.deletePlaylistCover(id)
        cache.coverFile(id).delete()
        _coverRev.update { it + 1 }
        refreshPlaylists()
    }

    private fun encodeCover(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val bmp = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("No se pudo leer la imagen")
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }
}
