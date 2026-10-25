package com.omnitune.app.data

import com.omnitune.app.data.net.AUDIENCE
import com.omnitune.app.data.net.AuthApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * Dueño de la sesión. OSIRIS rota el refresh token y revoca TODA la familia si ve un reuso,
 * así que solo puede haber un /refresh en vuelo: todo pasa por [lock].
 */
@Singleton
class SessionManager @Inject constructor(
    private val prefs: Prefs,
    private val authApi: AuthApi,
) {
    private val lock = ReentrantLock()
    private val _loggedIn = MutableStateFlow(prefs.refreshToken != null)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private fun nowSec() = System.currentTimeMillis() / 1000

    suspend fun login(server: String, auth: String, username: String, password: String) {
        prefs.serverUrl = server
        prefs.authUrl = auth
        prefs.username = username
        val t = authApi.login(LoginRequest(username.trim().lowercase(), password, AUDIENCE))
        save(t)
    }

    suspend fun logout() {
        val rt = prefs.refreshToken
        if (rt != null) runCatching { authApi.logout(RefreshRequest(rt)) }
        clear()
    }

    /** Token válido; si le quedan <30 s (o ya venció) lo refresca antes de usarlo. */
    fun validAccessToken(): String? {
        val at = prefs.accessToken
        if (at != null && prefs.accessExpiresAt - nowSec() > 30) return at
        if (prefs.refreshToken == null) return at
        return refresh(at) ?: at
    }

    /**
     * [stale] es el access token con el que falló la petición. Si mientras esperábamos el lock
     * otro hilo ya refrescó, devolvemos ese token en vez de volver a llamar a /refresh.
     */
    fun refresh(stale: String?): String? = lock.withLock {
        val current = prefs.accessToken
        if (current != null && current != stale && prefs.accessExpiresAt - nowSec() > 5) return current

        val rt = prefs.refreshToken ?: return null
        try {
            val resp = authApi.refresh(RefreshRequest(rt)).execute()
            if (resp.isSuccessful) {
                val body = resp.body() ?: return null
                save(body)
                body.accessToken
            } else {
                // Token muerto o revocado → hay que volver a iniciar sesión.
                // 429/5xx NO cierran la sesión: puede ser pasajero.
                if (resp.code() in 400..403) clear()
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun save(t: TokenResponse) {
        prefs.saveTokens(t.accessToken, t.refreshToken, nowSec() + t.expiresIn)
        _loggedIn.value = true
    }

    private fun clear() {
        prefs.clearTokens()
        _loggedIn.value = false
    }
}
