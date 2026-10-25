package com.omnitune.app.data.net

import com.omnitune.app.data.CreatePlaylistRequest
import com.omnitune.app.data.CreatedPlaylist
import com.omnitune.app.data.DiscoverResponse
import com.omnitune.app.data.DownloadStarted
import com.omnitune.app.data.FavoritesResponse
import com.omnitune.app.data.FeedResponse
import com.omnitune.app.data.LoginRequest
import com.omnitune.app.data.PlaylistDetail
import com.omnitune.app.data.PlaylistsResponse
import com.omnitune.app.data.Profile
import com.omnitune.app.data.RefreshRequest
import com.omnitune.app.data.Track
import com.omnitune.app.data.TokenResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

object Hosts {
    const val SERVER = "omnitune.invalid"
    const val AUTH = "osiris.invalid"
}

const val AUDIENCE = "omnitune"

interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse
    @POST("api/v1/auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<TokenResponse>
    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequest)
}

interface OmniApi {
    @GET("feed")
    suspend fun feed(@Query("skip") skip: Int, @Query("limit") limit: Int = 50): FeedResponse

    @GET("search")
    suspend fun search(@Query("q") q: String): List<Track>

    @GET("discover")
    suspend fun discover(
        @Query("q") q: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20,
    ): DiscoverResponse

    @Streaming
    @GET("music/{hash}/download")
    suspend fun downloadFile(@Path("hash") hash: String): Response<ResponseBody>

    @Streaming
    @GET("thumbnail/{hash}")
    suspend fun thumbnail(@Path("hash") hash: String): Response<ResponseBody>

    @POST("music")
    suspend fun startDownload(@Query("url") url: String): DownloadStarted

    @GET("music/progress/{id}")
    suspend fun downloadProgress(@Path("id") id: String): Map<String, String>

    @GET("users/me/profile")
    suspend fun profile(): Profile

    @GET("users/me/favorites")
    suspend fun favorites(): FavoritesResponse

    @POST("users/me/favorites/{hash}")
    suspend fun addFavorite(@Path("hash") hash: String)
    @DELETE("users/me/favorites/{hash}")
    suspend fun removeFavorite(@Path("hash") hash: String)

    @GET("users/me/playlists")
    suspend fun playlists(): PlaylistsResponse

    @POST("users/me/playlists")
    suspend fun createPlaylist(@Body body: CreatePlaylistRequest): CreatedPlaylist

    @GET("users/me/playlists/{id}")
    suspend fun playlist(@Path("id") id: String): PlaylistDetail

    @DELETE("users/me/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String)

    @POST("users/me/playlists/{id}/items/{hash}")
    suspend fun addToPlaylist(@Path("id") id: String, @Path("hash") hash: String)

    @DELETE("users/me/playlists/{id}/items/{hash}")
    suspend fun removeFromPlaylist(@Path("id") id: String, @Path("hash") hash: String)

    @Multipart
    @POST("users/me/playlists/{id}/cover")
    suspend fun uploadPlaylistCover(@Path("id") id: String, @Part file: MultipartBody.Part)

    @DELETE("users/me/playlists/{id}/cover")
    suspend fun deletePlaylistCover(@Path("id") id: String)

    @Streaming
    @GET("users/me/playlists/{id}/cover")
    suspend fun playlistCover(@Path("id") id: String): Response<ResponseBody>
}
