package hhuezo.finanzas.ui.main

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import hhuezo.finanzas.R
import hhuezo.finanzas.data.AppPermissions
import hhuezo.finanzas.data.SessionPreferences
import hhuezo.finanzas.security.BiometricAuth
import hhuezo.finanzas.ui.accounts.AccountsScreen
import hhuezo.finanzas.ui.budgets.BudgetsScreen
import hhuezo.finanzas.ui.categories.CategoriesScreen
import hhuezo.finanzas.ui.components.GlassAppBar
import hhuezo.finanzas.ui.components.GlassBackground
import hhuezo.finanzas.ui.components.GlassBottomTabBar
import hhuezo.finanzas.ui.components.GlassTabItem
import hhuezo.finanzas.ui.dashboard.DashboardScreen
import hhuezo.finanzas.ui.onboarding.FirstRunPermissionsScreen
import hhuezo.finanzas.ui.options.OptionsScreen
import hhuezo.finanzas.ui.security.BiometricLockScreen
import hhuezo.finanzas.ui.transactions.TransactionsScreen
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

private val AppTabs = listOf(
    GlassTabItem(id = AppDestinations.DASHBOARD.name, label = "Dashboard", icon = R.drawable.ic_dashboard),
    GlassTabItem(id = AppDestinations.TRANSACCIONES.name, label = "Transacciones", icon = R.drawable.ic_transactions),
    GlassTabItem(id = AppDestinations.OPCIONES.name, label = "Opciones", icon = R.drawable.ic_options),
)

enum class AppDestinations {
    DASHBOARD,
    TRANSACCIONES,
    CATEGORIAS,
    OPCIONES,
    CUENTAS,
    PRESUPUESTOS,
}

@Composable
fun FinanzasPersonalesApp() {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val sessionPrefs = remember { SessionPreferences(context) }
    val biometricHardwareAvailable = remember { BiometricAuth.isAvailable(context) }

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.TRANSACCIONES) }
    var biometricEnabled by rememberSaveable { mutableStateOf(sessionPrefs.biometricLockEnabled) }
    var permissionOnboardingDone by rememberSaveable {
        mutableStateOf(sessionPrefs.permissionOnboardingDone)
    }
    var unlocked by remember {
        mutableStateOf(!(biometricEnabled && biometricHardwareAvailable))
    }
    var lockError by remember { mutableStateOf<String?>(null) }
    var lockGeneration by remember { mutableIntStateOf(0) }
    val isAuthenticating = remember { AtomicBoolean(false) }

    if (AppPermissions.needsFirstRunScreen(context, sessionPrefs) && !permissionOnboardingDone) {
        FirstRunPermissionsScreen(
            onFinished = { enabled ->
                biometricEnabled = enabled
                sessionPrefs.biometricLockEnabled = enabled
                permissionOnboardingDone = true
                unlocked = !(enabled && BiometricAuth.isAvailable(context))
            }
        )
        return
    }

    fun requestUnlock(
        title: String = "Desbloquear Finanzas",
        subtitle: String = "Confirma tu huella para entrar",
        onSuccess: () -> Unit = { unlocked = true },
    ) {
        if (!BiometricAuth.isAvailable(activity)) {
            lockError = BiometricAuth.availabilityMessage(activity)
            if (biometricEnabled) {
                biometricEnabled = false
                sessionPrefs.biometricLockEnabled = false
                unlocked = true
            }
            return
        }
        if (!isAuthenticating.compareAndSet(false, true)) return
        lockError = null
        BiometricAuth.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = {
                isAuthenticating.set(false)
                lockError = null
                onSuccess()
            },
            onError = { message ->
                isAuthenticating.set(false)
                lockError = message
            },
            onCanceled = {
                isAuthenticating.set(false)
            },
        )
    }

    DisposableEffect(biometricEnabled) {
        if (!biometricEnabled) {
            unlocked = true
            return@DisposableEffect onDispose { }
        }
        val observer = LifecycleEventObserver { _, event ->
            // No bloquear mientras el diálogo de huella está abierto:
            // en varios teléfonos eso dispara ON_STOP y cancelaba el prompt.
            if (event == Lifecycle.Event.ON_STOP && !isAuthenticating.get()) {
                unlocked = false
                lockError = null
                lockGeneration += 1
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }

    // Al iniciar o volver bloqueado, pide la huella sola (sin tocar el botón).
    LaunchedEffect(unlocked, biometricEnabled, lockGeneration) {
        if (biometricEnabled && biometricHardwareAvailable && !unlocked) {
            delay(200)
            if (!unlocked && !isAuthenticating.get()) {
                requestUnlock()
            }
        }
    }

    if (biometricEnabled && biometricHardwareAvailable && !unlocked) {
        BiometricLockScreen(
            errorMessage = lockError,
            onUnlockClick = { requestUnlock() },
        )
        return
    }

    val selectedTabId = when (currentDestination) {
        AppDestinations.CUENTAS,
        AppDestinations.PRESUPUESTOS,
        AppDestinations.CATEGORIAS -> AppDestinations.OPCIONES.name
        else -> currentDestination.name
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (currentDestination != AppDestinations.DASHBOARD) {
                    val title = when (currentDestination) {
                        AppDestinations.CATEGORIAS -> "Categorías"
                        AppDestinations.TRANSACCIONES -> "Transacciones"
                        AppDestinations.OPCIONES -> "Opciones"
                        AppDestinations.CUENTAS -> "Cuentas"
                        AppDestinations.PRESUPUESTOS -> "Presupuestos"
                        AppDestinations.DASHBOARD -> ""
                    }
                    val showBack = currentDestination == AppDestinations.CATEGORIAS ||
                        currentDestination == AppDestinations.CUENTAS ||
                        currentDestination == AppDestinations.PRESUPUESTOS
                    GlassAppBar(
                        title = title,
                        onBack = if (showBack) {
                            { currentDestination = AppDestinations.OPCIONES }
                        } else {
                            null
                        }
                    )
                }
            },
            bottomBar = {
                GlassBottomTabBar(
                    items = AppTabs,
                    selectedId = selectedTabId,
                    onTabSelected = { tab ->
                        currentDestination = AppDestinations.valueOf(tab.id)
                    }
                )
            }
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val contentPadding = if (currentDestination == AppDestinations.DASHBOARD) {
                PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding(),
                    top = 0.dp
                )
            } else {
                innerPadding
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                when (currentDestination) {
                    AppDestinations.DASHBOARD -> DashboardScreen()
                    AppDestinations.TRANSACCIONES -> TransactionsScreen()
                    AppDestinations.CATEGORIAS -> CategoriesScreen()
                    AppDestinations.OPCIONES -> OptionsScreen(
                        biometricEnabled = biometricEnabled,
                        biometricAvailable = biometricHardwareAvailable,
                        biometricSubtitle = BiometricAuth.availabilityMessage(context),
                        onBiometricChange = { enable ->
                            if (enable) {
                                if (!BiometricAuth.isAvailable(activity)) {
                                    Toast.makeText(
                                        context,
                                        BiometricAuth.availabilityMessage(context),
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    return@OptionsScreen
                                }
                                requestUnlock(
                                    title = "Activar protección",
                                    subtitle = "Confirma tu huella para activarla",
                                ) {
                                    biometricEnabled = true
                                    sessionPrefs.biometricLockEnabled = true
                                    unlocked = true
                                    Toast.makeText(
                                        context,
                                        "Protección con huella activada",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } else {
                                requestUnlock(
                                    title = "Desactivar protección",
                                    subtitle = "Confirma tu huella para desactivarla",
                                ) {
                                    biometricEnabled = false
                                    sessionPrefs.biometricLockEnabled = false
                                    unlocked = true
                                    Toast.makeText(
                                        context,
                                        "Protección con huella desactivada",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        onCategoriasClick = { currentDestination = AppDestinations.CATEGORIAS },
                        onCuentasClick = { currentDestination = AppDestinations.CUENTAS },
                        onPresupuestosClick = { currentDestination = AppDestinations.PRESUPUESTOS },
                        onBackupRestored = {
                            currentDestination = AppDestinations.TRANSACCIONES
                        },
                    )
                    AppDestinations.CUENTAS -> AccountsScreen()
                    AppDestinations.PRESUPUESTOS -> BudgetsScreen()
                }
            }
        }
    }
}
