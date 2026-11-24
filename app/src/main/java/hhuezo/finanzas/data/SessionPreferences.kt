package hhuezo.finanzas.data

import android.content.Context
import android.content.SharedPreferences

class SessionPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Si es true, la app pide huella al abrir o al volver del segundo plano. */
    var biometricLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /** Pantalla de permisos del primer inicio ya completada. */
    var permissionOnboardingDone: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSION_ONBOARDING, value).apply()

    companion object {
        private const val PREFS_NAME = "finanzas_session"
        private const val KEY_BIOMETRIC = "biometric_lock_enabled"
        private const val KEY_PERMISSION_ONBOARDING = "permission_onboarding_done"
    }
}
