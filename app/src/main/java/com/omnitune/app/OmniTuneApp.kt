package com.omnitune.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

@HiltAndroidApp
class OmniTuneApp : Application(), ImageLoaderFactory {

    @Inject
    @Named("main")
    lateinit var client: OkHttpClient

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .build()
}
