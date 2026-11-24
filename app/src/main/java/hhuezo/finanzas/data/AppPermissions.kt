package hhuezo.finanzas.data

import android.content.Context
import androidx.annotation.DrawableRes
import hhuezo.finanzas.R
import hhuezo.finanzas.security.BiometricAuth

data class PermissionItem(
    val id: String,
    @param:DrawableRes val icon: Int,
    val title: String,
    val subtitle: String,
)

object AppPermissions {

    const val ID_BIOMETRIC = "biometric"
    const val ID_BACKUP = "backup"

    fun items(context: Context): List<PermissionItem> {
        val list = mutableListOf(
            PermissionItem(
                id = ID_BACKUP,
                icon = R.drawable.ic_export,
                title = "Respaldos",
                subtitle = "Al exportar o importar, el sistema te pedirá elegir el archivo. No hace falta un permiso permanente.",
            )
        )

        if (BiometricAuth.isAvailable(context) || BiometricAuth.canOfferSetup(context)) {
            list += PermissionItem(
                id = ID_BIOMETRIC,
                icon = R.drawable.ic_fingerprint,
                title = "Huella digital",
                subtitle = "Opcional. Si la activas, la app pedirá tu huella al abrirla.",
            )
        }

        return list
    }

    fun needsFirstRunScreen(context: Context, prefs: SessionPreferences): Boolean {
        if (prefs.permissionOnboardingDone) return false
        return items(context).isNotEmpty()
    }
}
