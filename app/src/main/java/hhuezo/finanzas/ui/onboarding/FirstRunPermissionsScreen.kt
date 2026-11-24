package hhuezo.finanzas.ui.onboarding

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import hhuezo.finanzas.R
import hhuezo.finanzas.data.AppPermissions
import hhuezo.finanzas.data.PermissionItem
import hhuezo.finanzas.data.SessionPreferences
import hhuezo.finanzas.security.BiometricAuth
import hhuezo.finanzas.ui.theme.AppBackground
import hhuezo.finanzas.ui.theme.CardShadow
import hhuezo.finanzas.ui.theme.ElegantBlue
import hhuezo.finanzas.ui.theme.IosTeal
import hhuezo.finanzas.ui.theme.OnGlass
import hhuezo.finanzas.ui.theme.OnGlassSecondary
import hhuezo.finanzas.ui.theme.Slate200
import hhuezo.finanzas.ui.theme.Slate500

@Composable
fun FirstRunPermissionsScreen(
    onFinished: (biometricEnabled: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val sessionPrefs = remember { SessionPreferences(context) }
    val items = remember { AppPermissions.items(context) }

    var biometricEnabled by remember { mutableStateOf(sessionPrefs.biometricLockEnabled) }

    fun enableBiometric() {
        if (!BiometricAuth.isAvailable(activity)) {
            Toast.makeText(
                context,
                BiometricAuth.availabilityMessage(context),
                Toast.LENGTH_LONG,
            ).show()
            if (BiometricAuth.canOfferSetup(activity)) {
                val enroll = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                } else {
                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                }
                runCatching { activity.startActivity(enroll) }
                    .onFailure {
                        activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    }
            }
            return
        }
        BiometricAuth.authenticate(
            activity = activity,
            title = "Activar protección",
            subtitle = "Confirma tu huella para proteger la app",
            onSuccess = {
                biometricEnabled = true
                sessionPrefs.biometricLockEnabled = true
                Toast.makeText(context, "Huella activada", Toast.LENGTH_SHORT).show()
            },
            onError = { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            },
        )
    }

    fun finish() {
        sessionPrefs.permissionOnboardingDone = true
        onFinished(biometricEnabled)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(ElegantBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = null,
                        tint = ElegantBlue,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Antes de empezar",
                    color = OnGlass,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Revisa cómo funciona la app. Todo es opcional y puedes cambiarlo después en Opciones.",
                    color = OnGlassSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))

                items.forEach { item ->
                    PermissionCard(
                        item = item,
                        biometricEnabled = biometricEnabled,
                        onEnableBiometric = { enableBiometric() },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { finish() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElegantBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text("Continuar", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    biometricEnabled: Boolean,
    onEnableBiometric: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElegantBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    tint = ElegantBlue,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = OnGlass,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = when {
                        item.id == AppPermissions.ID_BIOMETRIC && biometricEnabled -> "Activada"
                        item.id == AppPermissions.ID_BIOMETRIC -> "No activada"
                        else -> "Listo"
                    },
                    color = when {
                        item.id == AppPermissions.ID_BIOMETRIC && biometricEnabled -> IosTeal
                        item.id == AppPermissions.ID_BIOMETRIC -> SoftAmber
                        else -> IosTeal
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.subtitle,
            color = Slate500,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )

        if (item.id == AppPermissions.ID_BIOMETRIC && !biometricEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onEnableBiometric,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Activar huella", color = ElegantBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private val SoftAmber = Color(0xFFC27803)
