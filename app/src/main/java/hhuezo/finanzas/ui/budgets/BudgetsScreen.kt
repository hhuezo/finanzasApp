package hhuezo.finanzas.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hhuezo.finanzas.R
import hhuezo.finanzas.data.model.FinanzasCategoria
import hhuezo.finanzas.data.model.FinanzasPresupuesto
import hhuezo.finanzas.data.model.FinanzasPresupuestoResponse
import hhuezo.finanzas.data.repository.CategoriaRepository
import hhuezo.finanzas.data.repository.PresupuestoRepository
import hhuezo.finanzas.ui.categories.iconResForName
import hhuezo.finanzas.ui.components.AppBottomSheet
import hhuezo.finanzas.ui.theme.AppBackground
import hhuezo.finanzas.ui.theme.CardShadow
import hhuezo.finanzas.ui.theme.ElegantBlue
import hhuezo.finanzas.ui.theme.IosOrange
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
import java.util.Calendar
import java.util.Locale

private val MonthNames = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val moneyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 2
}

@Composable
fun BudgetsScreen(
    presupuestoRepository: PresupuestoRepository = remember { PresupuestoRepository() },
    categoriaRepository: CategoriaRepository = remember { CategoriaRepository() },
) {
    val now = remember { Calendar.getInstance() }
    var mes by rememberSaveable { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var anio by rememberSaveable { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var data by remember { mutableStateOf<FinanzasPresupuestoResponse?>(null) }
    var categoriasGasto by remember { mutableStateOf<List<FinanzasCategoria>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FinanzasPresupuesto?>(null) }
    var deleting by remember { mutableStateOf<FinanzasPresupuesto?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun cargar() {
        scope.launch {
            isLoading = true
            errorMessage = null
            presupuestoRepository.listar(mes = mes, anio = anio)
                .onSuccess { data = it }
                .onFailure { errorMessage = it.message ?: "Error al cargar presupuestos" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        categoriaRepository.listar(tipo = "GASTO", soloActivas = true)
            .onSuccess { categoriasGasto = it }
    }

    LaunchedEffect(mes, anio) {
        cargar()
    }

    if (showAddDialog) {
        PresupuestoFormDialog(
            title = "Nuevo presupuesto",
            categorias = categoriasGasto,
            initialCategoriaId = categoriasGasto.firstOrNull()?.id,
            initialMonto = "",
            mes = mes,
            anio = anio,
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = {
                if (!isSaving) {
                    showAddDialog = false
                    saveError = null
                }
            },
            onSave = { categoriaId, monto ->
                scope.launch {
                    isSaving = true
                    saveError = null
                    presupuestoRepository.crear(
                        categoriaId = categoriaId,
                        mes = mes,
                        anio = anio,
                        monto = monto,
                    )
                        .onSuccess {
                            showAddDialog = false
                            cargar()
                        }
                        .onFailure {
                            saveError = it.message ?: "No se pudo crear el presupuesto"
                        }
                    isSaving = false
                }
            }
        )
    }

    editing?.let { item ->
        PresupuestoFormDialog(
            title = "Modificar presupuesto",
            categorias = categoriasGasto,
            initialCategoriaId = item.categoriaId,
            initialMonto = item.monto.toString(),
            mes = mes,
            anio = anio,
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = {
                if (!isSaving) {
                    editing = null
                    saveError = null
                }
            },
            onSave = { categoriaId, monto ->
                scope.launch {
                    isSaving = true
                    saveError = null
                    presupuestoRepository.actualizar(
                        id = item.id,
                        categoriaId = categoriaId,
                        mes = mes,
                        anio = anio,
                        monto = monto,
                    )
                        .onSuccess {
                            editing = null
                            cargar()
                        }
                        .onFailure {
                            saveError = it.message ?: "No se pudo actualizar el presupuesto"
                        }
                    isSaving = false
                }
            }
        )
    }

    deleting?.let { item ->
        DeletePresupuestoDialog(
            nombre = item.categoria?.nombre ?: "presupuesto",
            isDeleting = isDeleting,
            errorMessage = deleteError,
            onDismiss = {
                if (!isDeleting) {
                    deleting = null
                    deleteError = null
                }
            },
            onConfirm = {
                scope.launch {
                    isDeleting = true
                    deleteError = null
                    presupuestoRepository.eliminar(item.id)
                        .onSuccess {
                            deleting = null
                            cargar()
                        }
                        .onFailure {
                            deleteError = it.message ?: "No se pudo eliminar el presupuesto"
                        }
                    isDeleting = false
                }
            }
        )
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
                },
                onAddClick = {
                    saveError = null
                    showAddDialog = true
                }
            )

            data?.let { summary ->
                SummaryRow(
                    totalPresupuesto = summary.totalPresupuesto,
                    totalGastado = summary.totalGastado
                )
            }
        }

        when {
            isLoading && data == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElegantBlue)
                }
            }

            errorMessage != null && data == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ErrorCard(message = errorMessage.orEmpty(), onRetry = { cargar() })
                }
            }

            data?.items.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    EmptyCard(text = "No hay presupuestos para este mes")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(data?.items.orEmpty(), key = { it.id }) { item ->
                        PresupuestoCard(
                            item = item,
                            onEdit = {
                                saveError = null
                                editing = item
                            },
                            onDelete = {
                                deleteError = null
                                deleting = item
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
                contentDescription = "Nuevo presupuesto",
                tint = ElegantBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SummaryRow(totalPresupuesto: Double, totalGastado: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryMini(
            label = "Presupuestado",
            value = moneyFormat.format(totalPresupuesto),
            accent = ElegantBlue,
            modifier = Modifier.weight(1f)
        )
        SummaryMini(
            label = "Gastado",
            value = moneyFormat.format(totalGastado),
            accent = IosOrange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryMini(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = label, color = OnGlassSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = accent,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PresupuestoCard(
    item: FinanzasPresupuesto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = Color((item.categoria?.color ?: 0xFF14213D).toInt())
    val progress = (item.porcentaje / 100.0).toFloat().coerceIn(0f, 1.2f)
    val barColor = when {
        item.excedido -> IosRed
        progress >= 0.85f -> IosOrange
        else -> IosTeal
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconResForName(item.categoria?.icono.orEmpty())),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.categoria?.nombre ?: "Categoría",
                    color = OnGlass,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.excedido) "Excedido" else "Restante ${moneyFormat.format(item.restante)}",
                    color = if (item.excedido) IosRed else OnGlassSecondary,
                    fontSize = 12.sp
                )
            }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${moneyFormat.format(item.gastado)} / ${moneyFormat.format(item.monto)}",
                color = OnGlass,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${item.porcentaje.toInt()}%",
                color = barColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
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
                    .fillMaxWidth(progress.coerceAtMost(1f).coerceAtLeast(0.02f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun PresupuestoFormDialog(
    title: String,
    categorias: List<FinanzasCategoria>,
    initialCategoriaId: Long?,
    initialMonto: String,
    mes: Int,
    anio: Int,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (categoriaId: Long, monto: Double) -> Unit,
) {
    var categoriaId by remember { mutableStateOf(initialCategoriaId) }
    var montoText by remember { mutableStateOf(initialMonto) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var categoryQuery by remember { mutableStateOf("") }
    val selected = categorias.firstOrNull { it.id == categoriaId }
    val filteredCategorias = remember(categorias, categoryQuery) {
        categorias.filter {
            categoryQuery.isBlank() ||
                it.nombre.contains(categoryQuery, ignoreCase = true) ||
                it.icono.contains(categoryQuery, ignoreCase = true)
        }
    }

    AppBottomSheet(
        onDismiss = { if (!isSaving) onDismiss() },
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
                val monto = montoText.toDoubleOrNull()
                val canSave = categoriaId != null && monto != null && monto > 0 && !isSaving
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canSave) ElegantBlue else ElegantBlue.copy(alpha = 0.45f))
                        .clickable(enabled = canSave) {
                            onSave(categoriaId!!, monto!!)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = title, color = OnGlass, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = "${MonthNames.getOrElse(mes - 1) { "" }} $anio",
                color = Slate500,
                fontSize = 13.sp
            )

            Text(text = "Categoría", color = Slate600, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            SearchableCategoryDropdown(
                selected = selected,
                query = categoryQuery,
                expanded = categoryExpanded,
                options = filteredCategorias,
                enabled = !isSaving && categorias.isNotEmpty(),
                onExpandedChange = { open ->
                    categoryExpanded = open
                    if (!open) categoryQuery = ""
                },
                onQueryChange = { categoryQuery = it },
                onSelect = { cat ->
                    categoriaId = cat.id
                    categoryExpanded = false
                    categoryQuery = ""
                }
            )

            Text(text = "Monto mensual", color = Slate600, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            BasicTextField(
                value = montoText,
                onValueChange = { value ->
                    if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        montoText = value
                    }
                },
                singleLine = true,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(ElegantBlue),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnGlass, fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                    .background(Slate100.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (montoText.isEmpty()) {
                        Text("Ej. 200.00", color = Slate500, fontSize = 15.sp)
                    }
                    inner()
                }
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(text = errorMessage, color = IosRed, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SearchableCategoryDropdown(
    selected: FinanzasCategoria?,
    query: String,
    expanded: Boolean,
    options: List<FinanzasCategoria>,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onSelect: (FinanzasCategoria) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (expanded) ElegantBlue else Slate200,
                shape = RoundedCornerShape(10.dp)
            )
            .background(Slate100.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onExpandedChange(!expanded) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(selected.color.toInt()).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconResForName(selected.icono)),
                        contentDescription = null,
                        tint = Color(selected.color.toInt()),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = selected?.nombre ?: "Selecciona una categoría",
                color = if (selected == null) Slate500 else OnGlass,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                painter = painterResource(
                    if (expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
                ),
                contentDescription = null,
                tint = Slate600,
                modifier = Modifier.size(18.dp)
            )
        }

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Slate200)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Slate200, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(ElegantBlue),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = OnGlass,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Buscar categoría...", color = Slate500, fontSize = 14.sp)
                        }
                        inner()
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (options.isEmpty()) {
                    Text(
                        text = "Sin resultados",
                        color = Slate500,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                } else {
                    options.forEach { cat ->
                        val isSelected = selected?.id == cat.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) ElegantBlue.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .clickable { onSelect(cat) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(cat.color.toInt()).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(iconResForName(cat.icono)),
                                    contentDescription = null,
                                    tint = Color(cat.color.toInt()),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.nombre,
                                color = if (isSelected) ElegantBlue else OnGlass,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletePresupuestoDialog(
    nombre: String,
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
                horizontalArrangement = Arrangement.End
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
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(text = "Eliminar", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Eliminar presupuesto", color = OnGlass, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = "¿Seguro que deseas eliminar el presupuesto de \"$nombre\"?",
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
