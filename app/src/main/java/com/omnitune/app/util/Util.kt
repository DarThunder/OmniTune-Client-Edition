package com.omnitune.app.util

import retrofit2.HttpException
import java.io.IOException

fun Throwable.userMessage(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Sesión expirada o credenciales inválidas"
        403 -> "No tienes permiso para hacer eso"
        404 -> "No encontrado"
        405 -> "El servidor aún no soporta esta acción"
        429 -> "Demasiados intentos, espera un momento"
        in 500..599 -> "Error del servidor (${code()})"
        else -> "Error HTTP ${code()}"
    }
    is IOException -> "Sin conexión con el servidor. ¿Tailscale está activo?"
    else -> message ?: "Error inesperado"
}

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** "mi-pc.tailnet.ts.net" → "https://mi-pc.tailnet.ts.net"; quita "/" final. */
fun normalizeUrl(raw: String): String {
    val t = raw.trim().trimEnd('/')
    if (t.isEmpty()) return ""
    return if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
}
