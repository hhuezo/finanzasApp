package hhuezo.finanzas.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hhuezo.finanzas.R
import hhuezo.finanzas.data.model.FinanzasCategoria
import hhuezo.finanzas.data.model.FinanzasCuenta
import hhuezo.finanzas.data.model.FinanzasTransaccion
import hhuezo.finanzas.data.repository.CategoriaRepository
import hhuezo.finanzas.data.repository.CuentaRepository
import hhuezo.finanzas.data.repository.TransaccionRepository
import hhuezo.finanzas.ui.categories.iconResForName
import hhuezo.finanzas.ui.components.AppBottomSheet
import hhuezo.finanzas.ui.theme.AppBackground
import hhuezo.finanzas.ui.theme.CardShadow
import hhuezo.finanzas.ui.theme.ElegantBlue
import hhuezo.finanzas.ui.theme.IosRed
import hhuezo.finanzas.ui.theme.IosTeal
import hhuezo.finanzas.ui.theme.OnGlass
import hhuezo.finanzas.ui.theme.OnGlassSecondary
import hhuezo.finanzas.ui.theme.Slate100
import hhuezo.finanzas.ui.theme.Slate200
import hhuezo.finanzas.ui.theme.Slate500
import hhuezo.finanzas.ui.theme.Slate600
import hhuezo.finanzas.ui.theme.WarmPill
import hhuezo.finanzas.ui.theme.WarmPillAccent
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class TxFilter(val apiTipo: String?, val label: String) {
    TODOS(null, "Todos"),
    GASTOS("GASTO", "Gastos"),
    INGRESOS("INGRESO", "Ingresos"),
}

private val MonthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val moneyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}

@Composable
fun TransactionsScreen(
    transaccionRepository: TransaccionRepository = remember { TransaccionRepository() },
    categoriaRepository: CategoriaRepository = remember { CategoriaRepository() },
    cuentaRepository: CuentaRepository = remember { CuentaRepository() },
) {
    val now = remember { Calendar.getInstance() }
    var mes by rememberSaveable { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var anio by rememberSaveable { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var filter by rememberSaveable { mutableStateOf(TxFilter.TODOS) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var transacciones by remember { mutableStateOf<List<FinanzasTransaccion>>(emptyList()) }
    var categorias by remember { mutableStateOf<List<FinanzasCategoria>>(emptyList()) }
    var cuentas by remember { mutableStateOf<List<FinanzasCuenta>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<FinanzasTransaccion?>(null) }
    var deletingTx by remember { mutableStateOf<FinanzasTransaccion?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun cargar() {
        scope.launch {
            isLoading = true
            errorMessage = null
            transaccionRepository.listar(mes = mes, anio = anio)
                .onSuccess { transacciones = it }
                .onFailure { errorMessage = it.message ?: "Error al cargar transacciones" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            categoriaRepository.listar(soloActivas = true)
                .onSuccess { categorias = it }
            cuentaRepository.listar()
                .onSuccess { cuentas = it }
        }
    }

    LaunchedEffect(mes, anio) {
        cargar()
    }

    if (showAddDialog) {
        TransactionFormDialog(
            title = "Nueva transacción",
            categorias = categorias,
            cuentas = cuentas,
            initialTipo = "GASTO",
            initialCategoriaId = null,
            initialCuentaId = cuentas.firstOrNull()?.id,
            initialCantidad = "",
            initialFecha = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Calendar.getInstance().time),
            initialNota = "",
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = {
                if (!isSaving) {
                    showAddDialog = false
                    saveError = null
                }
            },
            onSave = { tipo, categoriaId, cuentaId, cantidad, fecha, nota ->
                scope.launch {
                    isSaving = true
                    saveError = null
                    transaccionRepository.crear(
                        tipo = tipo,
                        categoriaId = categoriaId,
                        cuentaId = cuentaId,
                        cantidad = cantidad,
                        fecha = fecha,
                        nota = nota,
                    )
                        .onSuccess {
                            showAddDialog = false
                            cargar()
                        }
                        .onFailure {
                            saveError = it.message ?: "No se pudo crear la transacción"
                        }
                    isSaving = false
                }
            }
        )
    }

    editingTx?.let { tx ->
        TransactionFormDialog(
            title = "Modificar transacción",
            categorias = categorias,
            cuentas = cuentas,
            initialTipo = tx.tipo.uppercase(),
            initialCategoriaId = tx.categoriaId,
            initialCuentaId = tx.cuentaId,
            initialCantidad = tx.cantidad.toString(),
            initialFecha = tx.fecha.take(10),
            initialNota = tx.nota.orEmpty(),
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = {
                if (!isSaving) {
                    editingTx = null
                    saveError = null
                }
            },
            onSave = { tipo, categoriaId, cuentaId, cantidad, fecha, nota ->
                scope.launch {
                    isSaving = true
                    saveError = null
                    transaccionRepository.actualizar(
                        id = tx.id,
                        tipo = tipo,
                        categoriaId = categoriaId,
                        cuentaId = cuentaId,
                        cantidad = cantidad,
                        fecha = fecha,
                        nota = nota,
                    )
                        .onSuccess {
                            editingTx = null
                            // Si cambió de mes, recargar el mes actual; la lista se actualiza igual
                            cargar()
                        }
                        .onFailure {
                            saveError = it.message ?: "No se pudo actualizar la transacción"
                        }
                    isSaving = false
                }
            }
        )
    }

    deletingTx?.let { tx ->
        DeleteTransactionDialog(
            titulo = tx.nota?.takeIf { it.isNotBlank() }
                ?: tx.categoria?.nombre
                ?: "esta transacción",
            isDeleting = isDeleting,
            errorMessage = deleteError,
            onDismiss = {
                if (!isDeleting) {
                    deletingTx = null
                    deleteError = null
                }
            },
            onConfirm = {
                scope.launch {
                    isDeleting = true
                    deleteError = null
                    transaccionRepository.eliminar(tx.id)
                        .onSuccess {
                            deletingTx = null
                            cargar()
                        }
                        .onFailure {
                            deleteError = it.message ?: "No se pudo eliminar la transacción"
                        }
                    isDeleting = false
                }
            }
        )
    }

    val filtered = remember(transacciones, filter, searchQuery) {
        transacciones.filter { tx ->
            val tipoOk = filter.apiTipo == null || tx.tipo.equals(filter.apiTipo, ignoreCase = true)
            val q = searchQuery.trim()
            val searchOk = q.isBlank() ||
                tx.nota.orEmpty().contains(q, ignoreCase = true) ||
                tx.categoria?.nombre.orEmpty().contains(q, ignoreCase = true) ||
                tx.cuenta?.nombre.orEmpty().contains(q, ignoreCase = true)
            tipoOk && searchOk
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onAddClick = {
                    saveError = null
                    showAddDialog = true
                }
            )

            FilterTabs(
                selected = filter,
                onSelect = { filter = it }
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElegantBlue)
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ErrorCard(message = errorMessage.orEmpty(), onRetry = { cargar() })
                }
            }

            filtered.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    EmptyCard(
                        text = when {
                            searchQuery.isNotBlank() -> "Sin resultados para \"$searchQuery\""
                            filter != TxFilter.TODOS -> "No hay ${filter.label.lowercase()} este mes"
                            else -> "No hay transacciones este mes"
                        }
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { tx ->
                        TransactionCard(
                            transaccion = tx,
                            onEdit = {
                                saveError = null
                                editingTx = tx
                            },
                            onDelete = {
                                deleteError = null
                                deletingTx = tx
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .size(48.dp)
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

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = Slate600,
            modifier = Modifier
                .padding(start = 14.dp)
                .size(22.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            cursorBrush = SolidColor(ElegantBlue),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnGlass, fontSize = 14.sp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar nota, categoría o cuenta...",
                        color = Slate500,
                        fontSize = 14.sp
                    )
                }
                inner()
            }
        )
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .background(WarmPillAccent)
                .clickable(onClick = onAddClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = "Nueva transacción",
                tint = ElegantBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FilterTabs(
    selected: TxFilter,
    onSelect: (TxFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarmPill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TxFilter.entries.forEach { tab ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) ElegantBlue else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) Color.White else Slate600,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TransactionCard(
    transaccion: FinanzasTransaccion,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = Color((transaccion.categoria?.color ?: 0xFF14213D).toInt())
    val title = transaccion.nota?.takeIf { it.isNotBlank() }
        ?: transaccion.categoria?.nombre
        ?: "Movimiento"
    val subtitle = buildString {
        append(formatFecha(transaccion.fecha))
        transaccion.categoria?.nombre?.let {
            if (transaccion.nota?.isNotBlank() == true) {
                append(" · ")
                append(it)
            }
        }
    }
    val amountText = if (transaccion.isIngreso) {
        "+${moneyFormat.format(transaccion.cantidad)}"
    } else {
        "-${moneyFormat.format(transaccion.cantidad)}"
    }
    var menuExpanded by remember { mutableStateOf(false) }

    val stripeColor = if (transaccion.isIngreso) IosTeal else IosRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(stripeColor)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconResForName(transaccion.categoria?.icono.orEmpty())),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = OnGlass,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = OnGlassSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amountText,
                color = if (transaccion.isIngreso) IosTeal else OnGlass,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { menuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "Opciones",
                        tint = Slate600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modificar") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = IosRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionFormDialog(
    title: String,
    categorias: List<FinanzasCategoria>,
    cuentas: List<FinanzasCuenta>,
    initialTipo: String,
    initialCategoriaId: Long?,
    initialCuentaId: Long?,
    initialCantidad: String,
    initialFecha: String,
    initialNota: String,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (
        tipo: String,
        categoriaId: Long,
        cuentaId: Long,
        cantidad: Double,
        fecha: String,
        nota: String?,
    ) -> Unit,
) {
    var tipo by remember { mutableStateOf(initialTipo.uppercase()) }
    var categoriaId by remember { mutableStateOf(initialCategoriaId) }
    var cuentaId by remember { mutableStateOf(initialCuentaId ?: cuentas.firstOrNull()?.id) }
    var cantidadText by remember { mutableStateOf(initialCantidad) }
    var nota by remember { mutableStateOf(initialNota) }
    var fecha by remember { mutableStateOf(initialFecha.take(10)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val categoriasFiltradas = remember(categorias, tipo) {
        categorias.filter { it.tipo.equals(tipo, ignoreCase = true) }
    }

    LaunchedEffect(tipo, categoriasFiltradas) {
        if (categoriasFiltradas.none { it.id == categoriaId }) {
            categoriaId = categoriasFiltradas.firstOrNull()?.id
        }
    }

    AppBottomSheet(
        onDismiss = { if (!isSaving) onDismiss() },
        contentHeightFraction = 0.84f,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate100)
                        .clickable(enabled = !isSaving, onClick = onDismiss)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancelar",
                        color = Slate600,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val cantidad = cantidadText.toDoubleOrNull()
                val canSave = categoriaId != null &&
                    cuentaId != null &&
                    cantidad != null &&
                    cantidad > 0 &&
                    fecha.isNotBlank() &&
                    !isSaving
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canSave) ElegantBlue else ElegantBlue.copy(alpha = 0.45f))
                        .clickable(enabled = canSave) {
                            onSave(
                                tipo,
                                categoriaId!!,
                                cuentaId!!,
                                cantidad!!,
                                fecha.trim(),
                                nota.trim().ifBlank { null }
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "Guardar",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                color = OnGlass,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            AmountHeroField(
                value = cantidadText,
                enabled = !isSaving,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        cantidadText = it
                    }
                }
            )

            FieldLabel("Tipo")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarmPill)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("GASTO" to "Gasto", "INGRESO" to "Ingreso").forEach { (value, label) ->
                    val selected = tipo == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) ElegantBlue else Color.Transparent)
                            .clickable(enabled = !isSaving) { tipo = value },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else Slate600,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            FieldLabel("Categoría")
            if (categoriasFiltradas.isEmpty()) {
                Text(
                    text = "No hay categorías para este tipo",
                    color = Slate500,
                    fontSize = 13.sp
                )
            } else {
                CategoryHorizontalPicker(
                    categorias = categoriasFiltradas,
                    selectedId = categoriaId,
                    enabled = !isSaving,
                    onSelect = { categoriaId = it }
                )
            }

            FieldLabel("Cuenta")
            if (cuentas.isEmpty()) {
                Text(
                    text = "No hay cuentas disponibles",
                    color = Slate500,
                    fontSize = 13.sp
                )
            } else {
                AccountHorizontalPicker(
                    cuentas = cuentas,
                    selectedId = cuentaId,
                    enabled = !isSaving,
                    onSelect = { cuentaId = it }
                )
            }

            FieldLabel("Fecha")
            DatePickerField(
                fecha = fecha,
                enabled = !isSaving,
                onClick = { showDatePicker = true }
            )

            if (showDatePicker) {
                TransactionDatePickerDialog(
                    initialFecha = fecha,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { selected ->
                        fecha = selected
                        showDatePicker = false
                    }
                )
            }

            FieldLabel("Nota (opcional)")
            FormTextField(
                value = nota,
                onValueChange = { nota = it },
                placeholder = "Ej. Almuerzo",
                enabled = !isSaving
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(text = errorMessage, color = IosRed, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CategoryHorizontalPicker(
    categorias: List<FinanzasCategoria>,
    selectedId: Long?,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categorias.forEach { cat ->
            val selected = cat.id == selectedId
            val accent = Color(cat.color.toInt())
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) accent.copy(alpha = 0.14f) else Slate100)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) accent else Slate200,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = enabled) { onSelect(cat.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent.copy(alpha = if (selected) 0.22f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconResForName(cat.icono)),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = cat.nombre,
                    color = if (selected) OnGlass else Slate600,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AccountHorizontalPicker(
    cuentas: List<FinanzasCuenta>,
    selectedId: Long?,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cuentas.forEach { cuenta ->
            val selected = cuenta.id == selectedId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) ElegantBlue.copy(alpha = 0.12f) else Slate100)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) ElegantBlue else Slate200,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = enabled) { onSelect(cuenta.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = cuenta.nombre,
                    color = if (selected) ElegantBlue else Slate600,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DatePickerField(
    fecha: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val display = remember(fecha) { formatFechaDisplay(fecha) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
            .background(Slate100.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = display,
            color = OnGlass,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = "Elegir fecha",
            tint = ElegantBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerDialog(
    initialFecha: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialMillis = remember(initialFecha) { parseFechaToUtcMillis(initialFecha) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onConfirm(formatUtcMillisToApiDate(millis))
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Aceptar", color = ElegantBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Slate600)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun AmountHeroField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val showPlaceholder = value.isEmpty() && !focused

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "IMPORTE",
            color = Slate500,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$",
                color = ElegantBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(ElegantBlue),
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    color = OnGlass,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    textAlign = TextAlign.Start
                ),
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 260.dp)
                    .onFocusChanged { focusState ->
                        focused = focusState.isFocused
                        // Solo limpia placeholder "0.00" / "0"; no toca una cantidad real
                        if (focusState.isFocused) {
                            val placeholderLike = value.isEmpty() ||
                                value == "0" ||
                                value == "0.0" ||
                                value == "0.00"
                            if (placeholderLike) {
                                onValueChange("")
                            }
                        }
                    },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (showPlaceholder) {
                            Text(
                                text = "0.00",
                                color = Slate500.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(ElegantBlue),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnGlass, fontSize = 15.sp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
            .background(Slate100.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, color = Slate500, fontSize = 15.sp)
            }
            inner()
        }
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = Slate600,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
}

@Composable
private fun DeleteTransactionDialog(
    titulo: String,
    isDeleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AppBottomSheet(
        onDismiss = { if (!isDeleting) onDismiss() },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate100)
                        .clickable(enabled = !isDeleting, onClick = onDismiss)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancelar",
                        color = Slate600,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDeleting) IosRed.copy(alpha = 0.5f) else IosRed)
                        .clickable(enabled = !isDeleting, onClick = onConfirm)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "Eliminar",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Eliminar transacción",
                color = OnGlass,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "¿Seguro que deseas eliminar \"$titulo\"?",
                color = Slate600,
                fontSize = 14.sp
            )
            if (!errorMessage.isNullOrBlank()) {
                Text(text = errorMessage, color = IosRed, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EmptyCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Slate500)
    }
}

private fun formatFecha(raw: String): String {
    return try {
        val date = parseFecha(raw) ?: return raw.take(10)
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date)
    } catch (_: Exception) {
        raw.take(10)
    }
}

private fun formatFechaDisplay(raw: String): String {
    return try {
        val date = parseFecha(raw) ?: return raw.take(10)
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date)
    } catch (_: Exception) {
        raw.take(10)
    }
}

private fun parseFecha(raw: String): Date? {
    val parsers = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
    )
    parsers.forEach { it.timeZone = TimeZone.getTimeZone("UTC") }
    return parsers.firstNotNullOfOrNull { runCatching { it.parse(raw) }.getOrNull() }
}

private fun parseFechaToUtcMillis(raw: String): Long {
    val date = parseFecha(raw) ?: Date()
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun formatUtcMillisToApiDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(millis))
}
