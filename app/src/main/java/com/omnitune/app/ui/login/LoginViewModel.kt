package com.omnitune.app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.data.Prefs
import com.omnitune.app.data.SessionManager
import com.omnitune.app.util.normalizeUrl
import com.omnitune.app.util.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class LoginUi(
    val server: String = "",
    val auth: String = "",
    val username: String = "",
    val password: String = "",
    val showAdvanced: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionManager,
    prefs: Prefs,
) : ViewModel() {

    var ui by mutableStateOf(
        LoginUi(
            server = prefs.serverUrl,
            auth = prefs.authUrl,
            username = prefs.username,
            showAdvanced = prefs.authUrl.isNotBlank(),
        )
    )
        private set

    fun update(transform: (LoginUi) -> LoginUi) {
        ui = transform(ui).copy(error = null)
    }

    fun submit(onSuccess: () -> Unit) {
        val s = ui
        val server = normalizeUrl(s.server)
        if (server.isEmpty() || s.username.isBlank() || s.password.isEmpty()) {
            ui = s.copy(error = "Completa servidor, usuario y contraseña")
            return
        }
        ui = s.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                session.login(server, normalizeUrl(s.auth), s.username, s.password)
                ui = ui.copy(loading = false, password = "")
                onSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = when {
                    e is HttpException && e.code() == 401 -> "Usuario o contraseña incorrectos"
                    e is HttpException && e.code() == 400 -> "El servidor rechazó la audiencia \"omnitune\""
                    else -> e.userMessage()
                }
                ui = ui.copy(loading = false, error = msg)
            }
        }
    }
}
