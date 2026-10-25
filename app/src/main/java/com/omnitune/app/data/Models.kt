package com.omnitune.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

// ---------- OSIRIS (auth) ----------

@Serializable
data class LoginRequest(val username: String, val password: String, val audience: String)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long = 0,
    val audience: String? = null,
)
@Serializable
data class Track(
    val hash: String? = null,
    val id: String? = null,
    val title: String? = null,
    val uploader: String? = null,
    val duration: JsonElement? = null,
) {
    val key: String get() = hash ?: id ?: ""
    val displayTitle: String get() = title?.takeIf { it.isNotBlank() } ?: "Sin título"
    val displayArtist: String get() = uploader?.takeIf { it.isNotBlank() } ?: "Artista desconocido"
    val durationSeconds: Long? get() = durationOf(duration)
}

@Serializable
data class FeedResponse(
    val feed: List<Track> = emptyList(),
    @SerialName("next_skip") val nextSkip: Int? = null,
)

@Serializable
data class DiscoverItem(
    val title: String? = null,
    val uploader: String? = null,
    val url: String? = null,
    val duration: JsonElement? = null,
    val thumbnail: String? = null,
) {
    val durationSeconds: Long? get() = durationOf(duration)
}

@Serializable
data class DiscoverResponse(
    val page: Int = 1,
    val limit: Int = 20,
    @SerialName("has_more") val hasMore: Boolean = false,
    val results: List<DiscoverItem> = emptyList(),
)

@Serializable
data class DownloadStarted(@SerialName("task_id") val taskId: String)

@Serializable
data class FavoritesResponse(val favorites: List<Track> = emptyList())

@Serializable
data class PlaylistSummary(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("item_count") val itemCount: Int = 0,
)

@Serializable
data class PlaylistsResponse(val playlists: List<PlaylistSummary> = emptyList())

@Serializable
data class PlaylistDetail(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("item_count") val itemCount: Int = 0,
    val items: List<Track> = emptyList(),
)

@Serializable
data class CreatePlaylistRequest(val name: String, val description: String = "")

@Serializable
data class CreatedPlaylist(val id: String, val name: String = "")

@Serializable
data class Profile(val username: String = "", val bio: String? = null)

private fun durationOf(e: JsonElement?): Long? =
    (e as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong()?.takeIf { it > 0 }
