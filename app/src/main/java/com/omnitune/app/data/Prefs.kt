package com.omnitune.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Cifra los tokens con una clave AES-GCM que vive en el Android Keystore (no sale del dispositivo). */
class TokenCipher {
    private val alias = "omnitune_tokens"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        val out = c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? = runCatching {
        val raw = Base64.decode(encoded, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw.copyOfRange(0, 12)))
        String(c.doFinal(raw.copyOfRange(12, raw.size)), Charsets.UTF_8)
    }.getOrNull()
}

/**
 * Configuración y sesión. Lectura síncrona a propósito: la usan interceptores de OkHttp,
 * que corren en hilos de red y necesitan el valor al instante.
 */
@Singleton
class Prefs @Inject constructor(@ApplicationContext ctx: Context) {
    private val sp = ctx.getSharedPreferences("omnitune", Context.MODE_PRIVATE)
    private val cipher = TokenCipher()

    var serverUrl: String
        get() = sp.getString("server", "") ?: ""
        set(v) = sp.edit().putString("server", v).apply()

    /** URL de OSIRIS. Vacío = mismo host que el servidor de OmniTune. */
    var authUrl: String
        get() = sp.getString("auth", "") ?: ""
        set(v) = sp.edit().putString("auth", v).apply()

    var username: String
        get() = sp.getString("user", "") ?: ""
        set(v) = sp.edit().putString("user", v).apply()

    var accessToken: String?
        get() = sp.getString("at", null)?.let(cipher::decrypt)
        private set(v) = putSecret("at", v)

    var refreshToken: String?
        get() = sp.getString("rt", null)?.let(cipher::decrypt)
        private set(v) = putSecret("rt", v)

    /** Epoch en segundos en que expira el access token. */
    var accessExpiresAt: Long
        get() = sp.getLong("at_exp", 0L)
        private set(v) = sp.edit().putLong("at_exp", v).apply()

    /** commit() síncrono: el refresh token rota y no podemos perder el nuevo. */
    fun saveTokens(access: String, refresh: String, expiresAtSec: Long) {
        sp.edit()
            .putString("at", cipher.encrypt(access))
            .putString("rt", cipher.encrypt(refresh))
            .putLong("at_exp", expiresAtSec)
            .commit()
    }

    fun clearTokens() {
        sp.edit().remove("at").remove("rt").remove("at_exp").commit()
    }

    private fun putSecret(key: String, value: String?) {
        sp.edit().apply { if (value == null) remove(key) else putString(key, cipher.encrypt(value)) }.commit()
    }
}
