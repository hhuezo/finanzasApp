package hhuezo.finanzas.ui.options

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hhuezo.finanzas.R
import hhuezo.finanzas.data.backup.BackupRepository
import hhuezo.finanzas.ui.theme.AppBackground
import hhuezo.finanzas.ui.theme.CardShadow
import hhuezo.finanzas.ui.theme.ElegantBlue
import hhuezo.finanzas.ui.theme.FinanzasPersonalesTheme
import hhuezo.finanzas.ui.theme.IosRed
import hhuezo.finanzas.ui.theme.OnGlass
import hhuezo.finanzas.ui.theme.Slate100
import hhuezo.finanzas.ui.theme.Slate200
import hhuezo.finanzas.ui.theme.Slate500
import hhuezo.finanzas.ui.theme.Slate600
import kotlinx.coroutines.launch

@Composable
fun OptionsScreen(
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    biometricSubtitle: String,
    onBiometricChange: (Boolean) -> Unit,
    onCategoriasClick: () -> Unit,
    onCuentasClick: () -> Unit,
    onPresupuestosClick: () -> Unit,
    onBackupRestored: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupRepository = remember { BackupRepository() }
    var busy by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupRepository.MIME_TYPE)
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            backupRepository.exportar(context, uri)
                .onSuccess { resumen ->
                    Toast.makeText(
                        context,
                        "Respaldo listo: ${resumen.transacciones} transacciones",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                .onFailure {
                    Toast.makeText(
                        context,
                        it.message ?: "No se pudo exportar",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            busy = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        showImportConfirm = true
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!busy) {
                    showImportConfirm = false
                    pendingImportUri = null
                }
            },
            title = { Text("Restaurar respaldo") },
            text = {
                Text(
                    "Esto reemplazará todas las cuentas, categorías, transacciones y presupuestos actuales por los del archivo."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val uri = pendingImportUri ?: return@TextButton
                        scope.launch {
                            busy = true
                            backupRepository.importar(context, uri)
                                .onSuccess { resumen ->
                                    showImportConfirm = false
                                    pendingImportUri = null
                                    Toast.makeText(
                                        context,
                                        "Restaurado: ${resumen.transacciones} transacciones",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    onBackupRestored()
                                }
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        it.message ?: "No se pudo importar",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            busy = false
                        }
                    },
                ) {
                    Text("Restaurar", color = IosRed)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        showImportConfirm = false
                        pendingImportUri = null
                    },
                ) {
                    Text("Cancelar")
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("General")
            OptionsCard {
                OptionNavRow(
                    icon = R.drawable.ic_categories,
                    title = "Categorías",
                    subtitle = "Gastos e ingresos",
                    onClick = onCategoriasClick,
                    enabled = !busy,
                )
                OptionDivider()
                OptionNavRow(
                    icon = R.drawable.ic_accounts,
                    title = "Cuentas",
                    subtitle = "Bancos y billeteras",
                    onClick = onCuentasClick,
                    enabled = !busy,
                )
                OptionDivider()
                OptionNavRow(
                    icon = R.drawable.ic_wallet,
                    title = "Presupuestos",
                    subtitle = "Límites y metas mensuales",
                    onClick = onPresupuestosClick,
                    enabled = !busy,
                )
            }

            SectionLabel("Respaldo")
            OptionsCard {
                OptionNavRow(
                    icon = R.drawable.ic_export,
                    title = "Exportar datos",
                    subtitle = "Guardar un archivo JSON de respaldo",
                    onClick = {
                        exportLauncher.launch(BackupRepository.nombreArchivoSugerido())
                    },
                    enabled = !busy,
                )
                OptionDivider()
                OptionNavRow(
                    icon = R.drawable.ic_import,
                    title = "Importar datos",
                    subtitle = "Restaurar desde un archivo de respaldo",
                    onClick = {
                        importLauncher.launch(arrayOf(BackupRepository.MIME_TYPE, "text/*", "*/*"))
                    },
                    enabled = !busy,
                )
            }

            SectionLabel("Seguridad")
            OptionsCard {
                OptionSwitchRow(
                    icon = R.drawable.ic_fingerprint,
                    title = "Proteger con huella",
                    subtitle = biometricSubtitle,
                    checked = biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable && !busy,
                    onCheckedChange = onBiometricChange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (busy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ElegantBlue)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Slate500,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun OptionsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

@Composable
private fun OptionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Slate100)
    )
}

@Composable
private fun OptionNavRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OptionIcon(icon = icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) OnGlass else Slate500,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = Slate500,
                fontSize = 12.sp
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Slate600,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun OptionSwitchRow(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OptionIcon(icon = icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) OnGlass else Slate500,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = Slate500,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ElegantBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Slate200,
                disabledCheckedTrackColor = ElegantBlue.copy(alpha = 0.35f),
                disabledUncheckedTrackColor = Slate200.copy(alpha = 0.7f),
            )
        )
    }
}

@Composable
private fun OptionIcon(@DrawableRes icon: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ElegantBlue.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = ElegantBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 700)
@Composable
private fun OptionsPreview() {
    FinanzasPersonalesTheme {
        OptionsScreen(
            biometricEnabled = true,
            biometricAvailable = true,
            biometricSubtitle = "Pedir huella al abrir la app",
            onBiometricChange = {},
            onCategoriasClick = {},
            onCuentasClick = {},
            onPresupuestosClick = {},
        )
    }
}
