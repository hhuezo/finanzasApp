package hhuezo.finanzas.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {

    private const val Authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun isAvailable(context: Context): Boolean {
        return BiometricManager.from(context)
            .canAuthenticate(Authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /** Hay hardware de huella, aunque aún no haya ninguna registrada. */
    fun canOfferSetup(context: Context): Boolean {
        return when (BiometricManager.from(context).canAuthenticate(Authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            -> true
            else -> false
        }
    }

    fun availabilityMessage(context: Context): String {
        return when (BiometricManager.from(context).canAuthenticate(Authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                "Pedir huella al abrir la app"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Configura una huella en el teléfono para activarla aquí"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "Este teléfono no tiene sensor de huella"
            else ->
                "La huella no está disponible ahora"
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCanceled: () -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onCanceled()
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    // Reintento automático del sistema; no cerramos el flujo.
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(Authenticators)
            .build()

        prompt.authenticate(info)
    }
}
