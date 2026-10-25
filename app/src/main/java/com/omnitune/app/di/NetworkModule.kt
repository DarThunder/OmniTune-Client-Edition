package com.omnitune.app.di

import com.omnitune.app.data.Prefs
import com.omnitune.app.data.SessionManager
import com.omnitune.app.data.net.AuthApi
import com.omnitune.app.data.net.AuthInterceptor
import com.omnitune.app.data.net.Hosts
import com.omnitune.app.data.net.HostInterceptor
import com.omnitune.app.data.net.OmniApi
import com.omnitune.app.data.net.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /** Sin auth: lo usan login/refresh/logout (el refresh no puede depender de sí mismo). */
    @Provides
    @Singleton
    @Named("bare")
    fun bareClient(prefs: Prefs): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HostInterceptor(prefs))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("main")
    fun mainClient(
        @Named("bare") bare: OkHttpClient,
        prefs: Prefs,
        session: SessionManager,
    ): OkHttpClient = bare.newBuilder()
        .addInterceptor(AuthInterceptor(prefs, session))
        .authenticator(TokenAuthenticator(prefs, session))
        .build()

    @Provides
    @Singleton
    fun authApi(@Named("bare") client: OkHttpClient, json: Json): AuthApi =
        retrofit("http://${Hosts.AUTH}/", client, json).create(AuthApi::class.java)

    @Provides
    @Singleton
    fun omniApi(@Named("main") client: OkHttpClient, json: Json): OmniApi =
        retrofit("http://${Hosts.SERVER}/", client, json).create(OmniApi::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient, json: Json) = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
