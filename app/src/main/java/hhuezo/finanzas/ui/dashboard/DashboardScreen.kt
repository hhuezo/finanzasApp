package hhuezo.finanzas.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hhuezo.finanzas.R
import hhuezo.finanzas.data.model.FinanzasCuenta
import hhuezo.finanzas.data.model.FinanzasDashboard
import hhuezo.finanzas.data.model.FinanzasTransaccion
import hhuezo.finanzas.data.repository.CuentaRepository
import hhuezo.finanzas.data.repository.DashboardRepository
import hhuezo.finanzas.data.repository.TransaccionRepository
import hhuezo.finanzas.ui.categories.iconResForName
import hhuezo.finanzas.ui.components.GlassCard
import hhuezo.finanzas.ui.theme.AppBackground
import hhuezo.finanzas.ui.theme.CardShadow
import hhuezo.finanzas.ui.theme.ElegantBlue
import hhuezo.finanzas.ui.theme.ElegantBlueDeep
import hhuezo.finanzas.ui.theme.ElegantBlueSoft
import hhuezo.finanzas.ui.theme.FinanzasPersonalesTheme
import hhuezo.finanzas.ui.theme.IosOrange
import hhuezo.finanzas.ui.theme.IosRed
import hhuezo.finanzas.ui.theme.IosTeal
import hhuezo.finanzas.ui.theme.OnBlue
import hhuezo.finanzas.ui.theme.OnBlueMuted
import hhuezo.finanzas.ui.theme.OnGlass
import hhuezo.finanzas.ui.theme.OnGlassSecondary
import hhuezo.finanzas.ui.theme.Slate100
import hhuezo.finanzas.ui.theme.Slate200
import hhuezo.finanzas.ui.theme.Slate500
import hhuezo.finanzas.ui.theme.Slate600
import hhuezo.finanzas.ui.theme.WarmPill
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val MonthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val moneyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}

private val DayLabels = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

private data class DailyExpense(
    val dateKey: String,
    val dayLabel: String,
    val dayNumber: Int,
    val amount: Double,
)

@Composable
fun DashboardScreen(
    repository: DashboardRepository = remember { DashboardRepository() },
    transaccionRepository: TransaccionRepository = remember { TransaccionRepository() },
    cuentaRepository: CuentaRepository = remember { CuentaRepository() },
) {
    val now = remember { Calendar.getInstance() }
    var mes by rememberSaveable { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var anio by rememberSaveable { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var selectedCuentaId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cuentas by remember { mutableStateOf<List<FinanzasCuenta>>(emptyList()) }
    var dashboard by remember { mutableStateOf<FinanzasDashboard?>(null) }
    var weeklyExpenses by remember { mutableStateOf<List<DailyExpense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun cargar() {
        scope.launch {
            isLoading = true
            errorMessage = null
            if (cuentas.isEmpty()) {
                cuentaRepository.listar().onSuccess { cuentas = it }
            }
            repository.obtener(mes = mes, anio = anio)
                .onSuccess { dashboard = it }
                .onFailure { errorMessage = it.message ?: "Error al cargar el dashboard" }
            weeklyExpenses = loadLastWeekDailyExpenses(
                repository = transaccionRepository,
                cuentaId = selectedCuentaId,
            )
            isLoading = false
        }
    }

    LaunchedEffect(mes, anio, selectedCuentaId) {
        cargar()
    }

    val filteredTx = remember(dashboard, selectedCuentaId) {
        val txs = dashboard?.transacciones.orEmpty()
        if (selectedCuentaId == null) txs
        else txs.filter { it.cuentaId == selectedCuentaId || it.cuenta?.id == selectedCuentaId }
    }
    val filteredIngresos = remember(dashboard, selectedCuentaId, filteredTx) {
        if (selectedCuentaId == null) dashboard?.ingresos ?: 0.0
        else filteredTx.filter { it.isIngreso }.sumOf { it.cantidad }
    }
    val filteredGastos = remember(dashboard, selectedCuentaId, filteredTx) {
        if (selectedCuentaId == null) dashboard?.gastos ?: 0.0
        else filteredTx.filter { !it.isIngreso }.sumOf { it.cantidad }
    }
    val filteredBalance = remember(dashboard, selectedCuentaId, filteredIngresos, filteredGastos) {
        if (selectedCuentaId == null) dashboard?.balanceTotal ?: 0.0
        else filteredIngresos - filteredGastos
    }
    val selectedCuentaNombre = remember(cuentas, selectedCuentaId) {
        cuentas.firstOrNull { it.id == selectedCuentaId }?.nombre
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppBackground)
    ) {
        DashboardHeader(
            balanceTotal = filteredBalance,
            isLoading = isLoading && dashboard == null,
            accountLabel = selectedCuentaNombre,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-36).dp)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MonthSelector(
                mes = mes,
                anio = anio,
                onPrevious = {
                    if (mes == 1) {
                        mes = 12
                        anio -= 1
                    } else {
                        mes -= 1
                    }
                },
                onNext = {
                    if (mes == 12) {
                        mes = 1
                        anio += 1
                    } else {
                        mes += 1
                    }
                }
            )

            AccountFilterRow(
                cuentas = cuentas,
                selectedCuentaId = selectedCuentaId,
                onSelect = { selectedCuentaId = it }
            )

            when {
                isLoading && dashboard == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElegantBlue)
                    }
                }

                errorMessage != null && dashboard == null -> {
                    ErrorCard(
                        message = errorMessage.orEmpty(),
                        onRetry = { cargar() }
                    )
                }

                else -> {
                    if (dashboard != null) {
                        SummaryCards(
                            ingresos = filteredIngresos,
                            gastos = filteredGastos
                        )
                        WeeklyExpenseChartSection(
                            days = weeklyExpenses,
                            isLoading = isLoading
                        )
                        ExpenseByCategorySection(
                            transacciones = filteredTx,
                            totalGastos = filteredGastos,
                            isLoading = isLoading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DashboardHeader(
    balanceTotal: Double,
    isLoading: Boolean,
    accountLabel: String? = null,
) {
    val isPositive = balanceTotal > 0
    val isNegative = balanceTotal < 0
    val statusLabel = when {
        isPositive -> "Posición favorable"
        isNegative -> "Posición en déficit"
        else -> "Sin movimiento neto"
    }
    val statusColor = when {
        isPositive -> Color(0xFF34D399)
        isNegative -> Color(0xFFFCA5A5)
        else -> Color(0xFFE2E8F0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1B3A),
                        ElegantBlueDeep,
                        ElegantBlue
                    )
                )
            )
            .padding(bottom = 48.dp)
    ) {
        // Acento sutil tipo banca privada
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .size(180.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-50).dp, y = 40.dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(ElegantBlueSoft.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FINANZAS PERSONALES",
                    color = OnBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = accountLabel ?: "Todas",
                        color = OnBlueMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.07f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BALANCE TOTAL",
                        color = OnBlueMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            color = OnBlueMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading) {
                    CircularProgressIndicator(
                        color = OnBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = moneyFormat.format(balanceTotal),
                        color = OnBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        letterSpacing = (-0.8).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (accountLabel.isNullOrBlank()) {
                        "Ingresos − gastos de todas las cuentas"
                    } else {
                        "Ingresos − gastos de $accountLabel"
                    },
                    color = OnBlueMuted.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MonthSelector(
    mes: Int,
    anio: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        contentPadding = 12.dp,
        elevation = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ElegantBlue.copy(alpha = 0.08f))
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Mes anterior",
                    tint = ElegantBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "${MonthNames.getOrElse(mes - 1) { "" }} $anio",
                color = OnGlass,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ElegantBlue.copy(alpha = 0.08f))
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward),
                    contentDescription = "Mes siguiente",
                    tint = ElegantBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryCards(ingresos: Double, gastos: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            title = "Ingresos",
            amount = ingresos,
            accent = IosTeal,
            icon = R.drawable.ic_arrow_up,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            title = "Gastos",
            amount = gastos,
            accent = IosOrange,
            icon = R.drawable.ic_arrow_down,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    accent: Color,
    icon: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = CardShadow, spotColor = Color(0x10000000))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = OnGlassSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = moneyFormat.format(amount),
            color = OnGlass,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private suspend fun loadLastWeekDailyExpenses(
    repository: TransaccionRepository,
    cuentaId: Long? = null,
): List<DailyExpense> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }

    val months = linkedSetOf(
        start.get(Calendar.MONTH) + 1 to start.get(Calendar.YEAR),
        today.get(Calendar.MONTH) + 1 to today.get(Calendar.YEAR),
    )

    val transacciones = coroutineScope {
        months.map { (m, y) ->
            async {
                repository.listar(
                    mes = m,
                    anio = y,
                    tipo = "GASTO",
                    cuentaId = cuentaId,
                ).getOrDefault(emptyList())
            }
        }.flatMap { it.await() }
    }

    return buildLastWeekDailyExpenses(transacciones, today)
}

@Composable
private fun AccountFilterRow(
    cuentas: List<FinanzasCuenta>,
    selectedCuentaId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccountFilterChip(
            label = "Todas",
            selected = selectedCuentaId == null,
            onClick = { onSelect(null) }
        )
        cuentas.forEach { cuenta ->
            AccountFilterChip(
                label = cuenta.nombre,
                selected = selectedCuentaId == cuenta.id,
                onClick = { onSelect(cuenta.id) }
            )
        }
    }
}

@Composable
private fun AccountFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) ElegantBlue else WarmPill)
            .border(
                width = 1.dp,
                color = if (selected) ElegantBlue else Color(0xFFE0DAD2),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Slate600,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

private fun buildLastWeekDailyExpenses(
    transacciones: List<FinanzasTransaccion>,
    today: Calendar = Calendar.getInstance(),
): List<DailyExpense> {
    val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val totals = mutableMapOf<String, Double>()

    transacciones
        .filter { !it.isIngreso }
        .forEach { tx ->
            val date = parseDashboardFecha(tx.fecha) ?: return@forEach
            val key = keyFormat.format(date)
            totals[key] = (totals[key] ?: 0.0) + tx.cantidad
        }

    val base = (today.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return (6 downTo 0).map { offset ->
        val day = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
        val key = keyFormat.format(day.time)
        DailyExpense(
            dateKey = key,
            dayLabel = DayLabels[day.get(Calendar.DAY_OF_WEEK) - 1],
            dayNumber = day.get(Calendar.DAY_OF_MONTH),
            amount = totals[key] ?: 0.0,
        )
    }
}

private fun parseDashboardFecha(raw: String): Date? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
    )
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            if (pattern.contains("'Z'")) {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
            }
            return sdf.parse(raw.trim())
        } catch (_: Exception) {
            // try next
        }
    }
    return null
}

@Composable
private fun WeeklyExpenseChartSection(
    days: List<DailyExpense>,
    isLoading: Boolean,
) {
    val maxAmount = days.maxOfOrNull { it.amount } ?: 0.0
    val weekTotal = days.sumOf { it.amount }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gasto diario",
                    color = OnGlass,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Última semana",
                    color = OnGlassSecondary,
                    fontSize = 12.sp
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    color = ElegantBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = moneyFormat.format(weekTotal),
                    color = IosOrange,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = CardShadow,
                    spotColor = Color(0x10000000)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Slate200.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(vertical = 16.dp),
        ) {
            val chartScroll = rememberScrollState()
            val columnWidth = 72.dp
            val barWidth = 36.dp

            // Orientar al día actual (derecha); scroll a la izquierda para días previos
            LaunchedEffect(days, chartScroll.maxValue) {
                if (chartScroll.maxValue > 0) {
                    chartScroll.scrollTo(chartScroll.maxValue)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chartScroll)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { day ->
                    val fraction = if (maxAmount > 0) (day.amount / maxAmount).toFloat() else 0f
                    val barHeight = if (day.amount > 0) {
                        (fraction * 118f).coerceAtLeast(8f).dp
                    } else {
                        4.dp
                    }
                    Column(
                        modifier = Modifier
                            .width(columnWidth)
                            .height(178.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = if (day.amount > 0) {
                                String.format(Locale.US, "%.2f", day.amount)
                            } else {
                                " "
                            },
                            color = OnGlassSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (day.amount > 0) IosOrange else Slate100
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = day.dayLabel,
                            color = OnGlass,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = day.dayNumber.toString(),
                            color = OnGlassSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

private data class CategoryExpense(
    val id: Long,
    val nombre: String,
    val icono: String,
    val color: Long,
    val total: Double,
    val percent: Float,
)

private fun buildCategoryExpenses(
    transacciones: List<FinanzasTransaccion>,
    totalGastos: Double,
): List<CategoryExpense> {
    val gastos = transacciones.filter { !it.isIngreso }
    val grouped = gastos.groupBy { it.categoria?.id ?: -1L }
    val base = if (totalGastos > 0) totalGastos else gastos.sumOf { it.cantidad }

    return grouped.map { (id, items) ->
        val first = items.first()
        val total = items.sumOf { it.cantidad }
        CategoryExpense(
            id = id,
            nombre = first.categoria?.nombre ?: "Sin categoría",
            icono = first.categoria?.icono.orEmpty(),
            color = first.categoria?.color ?: 0xFF14213DL,
            total = total,
            percent = if (base > 0) (total / base).toFloat().coerceIn(0f, 1f) else 0f
        )
    }.sortedByDescending { it.total }
}

@Composable
private fun ExpenseByCategorySection(
    transacciones: List<FinanzasTransaccion>,
    totalGastos: Double,
    isLoading: Boolean,
) {
    val categories = remember(transacciones, totalGastos) {
        buildCategoryExpenses(transacciones, totalGastos)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gasto por categoría",
                color = OnGlass,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            if (isLoading) {
                CircularProgressIndicator(
                    color = ElegantBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Slate200, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin gastos en este mes",
                    color = Slate500
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = CardShadow,
                        spotColor = Color(0x10000000)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Slate200.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                categories.forEach { item ->
                    CategoryExpenseRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseRow(item: CategoryExpense) {
    val accent = Color(item.color.toInt())
    val percentLabel = "${(item.percent * 100).toInt()}%"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconResForName(item.icono)),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nombre,
                    color = OnGlass,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = percentLabel,
                    color = OnGlassSecondary,
                    fontSize = 12.sp
                )
            }
            Text(
                text = moneyFormat.format(item.total),
                color = OnGlass,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Slate100)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(item.percent.coerceAtLeast(0.02f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent)
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = message, color = IosRed)
        Text(
            text = "Reintentar",
            color = ElegantBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRetry)
                .background(Slate100)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DashboardPreview() {
    FinanzasPersonalesTheme {
        Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
            // Solo preview visual del header + selector vacío
            Column {
                DashboardHeader(balanceTotal = -299.30, isLoading = false)
            }
        }
    }
}
