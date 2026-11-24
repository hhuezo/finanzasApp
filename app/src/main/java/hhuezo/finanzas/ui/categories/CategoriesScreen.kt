package hhuezo.finanzas.ui.categories

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hhuezo.finanzas.R
import hhuezo.finanzas.data.model.FinanzasCategoria
import hhuezo.finanzas.data.repository.CategoriaRepository
import hhuezo.finanzas.ui.components.AppBottomSheet
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
import hhuezo.finanzas.ui.theme.WarmPill
import hhuezo.finanzas.ui.theme.WarmPillAccent
import kotlinx.coroutines.launch

private enum class CategoryTab(val apiTipo: String, val label: String) {
    GASTOS("GASTO", "Gastos"),
    INGRESOS("INGRESO", "Ingresos"),
}

@Composable
fun CategoriesScreen(
    repository: CategoriaRepository = remember { CategoriaRepository() }
) {
    var selectedTab by rememberSaveable { mutableStateOf(CategoryTab.GASTOS) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var categorias by remember { mutableStateOf<List<FinanzasCategoria>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategoria by remember { mutableStateOf<FinanzasCategoria?>(null) }
    var deletingCategoria by remember { mutableStateOf<FinanzasCategoria?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun cargar() {
        scope.launch {
            isLoading = true
            errorMessage = null
            repository.listar(tipo = selectedTab.apiTipo)
                .onSuccess { categorias = it }
                .onFailure { errorMessage = it.message ?: "Error al cargar categorías" }
            isLoading = false
        }
    }

    fun afterTipoChange(tipo: String) {
        if (tipo == selectedTab.apiTipo) {
            cargar()
        } else {
            selectedTab = if (tipo == "INGRESO") {
                CategoryTab.INGRESOS
            } else {
                CategoryTab.GASTOS
            }
        }
    }

    LaunchedEffect(selectedTab) {
        cargar()
    }

    if (showAddDialog) {
        AddCategoryDialog(
            initialTipo = selectedTab.apiTipo,
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = {
                if (!isSaving) {
                    showAddDialog = false
                    saveError = null
                }
            },
            onSave = { nombre, tipo, icono, color ->
                scope.launch {
                    isSaving = true
                    saveError = null
                    repository.crear(nombre = nombre, tipo = tipo, icono = icono, color = color)
                        .onSuccess {
                            showAddDialog = false
                            afterTipoChange(tipo)
                        }
                        .onFailure {
                            saveError = it.message ?: "No se pudo crear la categoría"
                        }
                    isSaving = false
                }
            }
        )
    }

    editingCategoria?.let { categoria ->
        EditCategoryDialog(
            initialNombre = categoria.nombre,
            initialTipo = categoria.tipo,
            initialIcono = categoria.icono,
            initialColor = categoria.color,
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = {
                if (!isSaving) {
                    editingCategoria = null
                    saveError = null
                }
            },
            onSave = { nombre, tipo, icono, color ->
                scope.launch {
                    isSaving = true
                    saveError = null
                    repository.actualizar(
                        id = categoria.id,
                        nombre = nombre,
                        tipo = tipo,
                        icono = icono,
                        color = color,
                        activo = categoria.activo,
                    )
                        .onSuccess {
                            editingCategoria = null
                            afterTipoChange(tipo)
                        }
                        .onFailure {
                            saveError = it.message ?: "No se pudo actualizar la categoría"
                        }
                    isSaving = false
                }
            }
        )
    }

    deletingCategoria?.let { categoria ->
        DeleteCategoryDialog(
            nombre = categoria.nombre,
            isDeleting = isDeleting,
            errorMessage = deleteError,
            onDismiss = {
                if (!isDeleting) {
                    deletingCategoria = null
                    deleteError = null
                }
            },
            onConfirm = {
                scope.launch {
                    isDeleting = true
                    deleteError = null
                    repository.eliminar(categoria.id)
                        .onSuccess {
                            deletingCategoria = null
                            cargar()
                        }
                        .onFailure {
                            deleteError = it.message ?: "No se pudo eliminar la categoría"
                        }
                    isDeleting = false
                }
            }
        )
    }

    val filtered = remember(categorias, searchQuery) {
        categorias.filter {
            searchQuery.isBlank() ||
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                it.icono.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Fijo: búsqueda + tabs
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onAddClick = {
                    saveError = null
                    showAddDialog = true
                }
            )

            CategoryTypeTabs(
                selected = selectedTab,
                gastosCount = if (selectedTab == CategoryTab.GASTOS) categorias.size else null,
                ingresosCount = if (selectedTab == CategoryTab.INGRESOS) categorias.size else null,
                onSelect = { selectedTab = it }
            )
        }

        // Solo el listado hace scroll
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 48.dp),
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
                    ErrorCard(
                        message = errorMessage.orEmpty(),
                        onRetry = { cargar() }
                    )
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
                        text = if (searchQuery.isBlank()) {
                            "No hay categorías de ${selectedTab.label.lowercase()}"
                        } else {
                            "Sin resultados para \"$searchQuery\""
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
                    items(filtered, key = { it.id }) { categoria ->
                        CategoriaApiCard(
                            categoria = categoria,
                            onEdit = {
                                saveError = null
                                editingCategoria = categoria
                            },
                            onDelete = {
                                deleteError = null
                                deletingCategoria = categoria
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAddClick: () -> Unit
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
                        text = "Buscar por nombre o icono...",
                        color = Slate500,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
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
                contentDescription = "Nueva categoría",
                tint = ElegantBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CategoryTypeTabs(
    selected: CategoryTab,
    gastosCount: Int?,
    ingresosCount: Int?,
    onSelect: (CategoryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarmPill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabPill(
            selected = selected == CategoryTab.GASTOS,
            label = gastosCount?.let { "Gastos ($it)" } ?: "Gastos",
            icon = R.drawable.ic_arrow_down,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(CategoryTab.GASTOS) }
        )
        TabPill(
            selected = selected == CategoryTab.INGRESOS,
            label = ingresosCount?.let { "Ingresos ($it)" } ?: "Ingresos",
            icon = R.drawable.ic_arrow_up,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(CategoryTab.INGRESOS) }
        )
    }
}

@Composable
private fun TabPill(
    selected: Boolean,
    label: String,
    icon: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ElegantBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (selected) Color.White else Slate600,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) Color.White else Slate600,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun CategoriaApiCard(
    categoria: FinanzasCategoria,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val accent = Color(categoria.color.toInt())
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconResForName(categoria.icono)),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = categoria.nombre,
                color = OnGlass,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            )
        }

        Box {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { menuExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "Opciones",
                    tint = Slate600,
                    modifier = Modifier.size(22.dp)
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
                    text = {
                        Text("Eliminar", color = IosRed)
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun DeleteCategoryDialog(
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
                text = "Eliminar categoría",
                color = OnGlass,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "¿Seguro que deseas eliminar \"$nombre\"?",
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
        Text(
            text = message,
            color = IosRed,
            style = MaterialTheme.typography.bodyMedium
        )
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

@Preview(
    name = "Categorías teléfono vertical",
    showBackground = true,
    backgroundColor = 0xFFE7E3DC,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun CategoriesPreview() {
    FinanzasPersonalesTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SearchBar(query = "", onQueryChange = {}, onAddClick = {})
                CategoryTypeTabs(
                    selected = CategoryTab.GASTOS,
                    gastosCount = 1,
                    ingresosCount = null,
                    onSelect = {}
                )
                CategoriaApiCard(
                    categoria = FinanzasCategoria(
                        id = 1,
                        tipo = "GASTO",
                        icono = "Restaurant",
                        nombre = "Comida",
                        color = 4294198070L,
                        activo = true
                    )
                )
            }
        }
    }
}
