package com.yawmiyati.app.data.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) : TokenStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "yawmiyati_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Plain prefs for non-secret flags (survives TokenAuthenticator.clear() race conditions)
    private val flags = context.getSharedPreferences("wathb_flags", Context.MODE_PRIVATE)

    override fun saveTokens(access: String, refresh: String) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .apply()
        flags.edit().putBoolean(FLAG_BIOMETRIC_ELIGIBLE, true).commit()
        Log.d("WATHB_AUTH", "saveTokens: FLAG_BIOMETRIC_ELIGIBLE=true")
    }

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)
    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun saveRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun isLoggedIn(): Boolean = getAccessToken() != null

    /** True after any successful login, until a hard clear(). Survives soft-logout. */
    fun isBiometricEligible(): Boolean {
        val v = flags.getBoolean(FLAG_BIOMETRIC_ELIGIBLE, false)
        Log.d("WATHB_AUTH", "isBiometricEligible=$v")
        return v
    }

    /** Full logout — clears everything. */
    override fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_ROLE)
            .apply()
        flags.edit().putBoolean(FLAG_BIOMETRIC_ELIGIBLE, false).apply()
    }

    /** Soft logout — clears only the access token; keeps refresh token for biometric re-login. */
    fun softLogout() {
        prefs.edit().remove(KEY_ACCESS).apply()
        Log.d("WATHB_AUTH", "softLogout: access removed, flag=${flags.getBoolean(FLAG_BIOMETRIC_ELIGIBLE, false)}")
    }

    companion object {
        private const val KEY_ACCESS  = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_ROLE    = "user_role"
        private const val FLAG_BIOMETRIC_ELIGIBLE = "biometric_eligible"
    }
}
