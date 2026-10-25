package com.omnitune.app.data.net

import com.omnitune.app.data.Prefs
import com.omnitune.app.data.SessionManager
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

class HostInterceptor(private val prefs: Prefs) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val target = when (req.url.host) {
            Hosts.SERVER -> prefs.serverUrl
            Hosts.AUTH -> prefs.authUrl.ifBlank { prefs.serverUrl }
            else -> return chain.proceed(req)
        }.toHttpUrlOrNull() ?: throw IOException("URL del servidor no configurada")

        val prefix = target.encodedPath.trimEnd('/')
        val url = req.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .encodedPath(prefix + req.url.encodedPath)
            .build()
        return chain.proceed(req.newBuilder().url(url).build())
    }
}

class AuthInterceptor(
    private val prefs: Prefs,
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        if (req.header("Authorization") != null || !isServerHost(prefs, req)) return chain.proceed(req)
        val token = session.validAccessToken() ?: return chain.proceed(req)
        return chain.proceed(req.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

class TokenAuthenticator(
    private val prefs: Prefs,
    private val session: SessionManager,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (!isServerHost(prefs, response.request)) return null
        if (generateSequence(response) { it.priorResponse }.count() >= 2) return null

        val stale = response.request.header("Authorization")?.removePrefix("Bearer ")
        val fresh = session.refresh(stale) ?: return null
        if (fresh == stale) return null
        return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
    }
}

private fun isServerHost(prefs: Prefs, req: Request): Boolean {
    val serverHost = prefs.serverUrl.toHttpUrlOrNull()?.host ?: return false
    return req.url.host == serverHost
}
