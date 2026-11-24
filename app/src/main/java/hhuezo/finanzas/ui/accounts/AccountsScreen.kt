package hhuezo.finanzas.ui.accounts

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
import hhuezo.finanzas.data.model.FinanzasCuenta
import hhuezo.finanzas.data.repository.CuentaRepository
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

@Composable
fun AccountsScreen(
    repository: CuentaRepository = remember { CuentaRepository() }
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var cuentas by remember { mutableStateOf<List<FinanzasCuenta>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCuenta by remember { mutableStateOf<FinanzasCuenta?>(null) }
    var deletingCuenta by remember { mutableStateOf<FinanzasCuenta?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun cargar() {
        scope.launch {
            isLoading = true
            errorMessage = null
            repository.listar()
                .onSuccess { cuentas = it }
                .onFailure { errorMessage = it.message ?: "Error al cargar cuentas" }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        cargar()
    }

    if (showAddDialog) {
        CuentaFormDialog(
            title = "Nueva cuenta",
            initialNombre = "",
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = {
                if (!isSaving) {
                    showAddDialog = false
                    saveError = null
                }
            },
            onSave = { nombre ->
                scope.launch {
                    isSaving = true
                    saveError = null
                    repository.crear(nombre)
                        .onSuccess {
                            showAddDialog = false
                            cargar()
                        }
                        .onFailure {
                            saveError = it.message ?: "No se pudo crear la cuenta"
                        }
                    isSaving = false
                }
            }
        )
    }

    editingCuenta?.let { cuenta ->
        CuentaFormDialog(
            title = "Modificar cuenta",
            initialNombre = cuenta.nombre,
            isSaving = isSaving,
            errorMessage = saveError,
            onDismiss = {
                if (!isSaving) {
                    editingCuenta = null
                    saveError = null
                }
            },
            onSave = { nombre ->
                scope.launch {
                    isSaving = true
                    saveError = null
                    repository.actualizar(cuenta.id, nombre)
                        .onSuccess {
                            editingCuenta = null
                            cargar()
                        }
                        .onFailure {
                            saveError = it.message ?: "No se pudo actualizar la cuenta"
                        }
                    isSaving = false
                }
            }
        )
    }

    deletingCuenta?.let { cuenta ->
        DeleteCuentaDialog(
            nombre = cuenta.nombre,
            isDeleting = isDeleting,
            errorMessage = deleteError,
            onDismiss = {
                if (!isDeleting) {
                    deletingCuenta = null
                    deleteError = null
                }
            },
            onConfirm = {
                scope.launch {
                    isDeleting = true
                    deleteError = null
                    repository.eliminar(cuenta.id)
                        .onSuccess {
                            deletingCuenta = null
                            cargar()
                        }
                        .onFailure {
                            deleteError = it.message ?: "No se pudo eliminar la cuenta"
                        }
                    isDeleting = false
                }
            }
        )
    }

    val filtered = remember(cuentas, searchQuery) {
        cuentas.filter {
            searchQuery.isBlank() || it.nombre.contains(searchQuery, ignoreCase = true)
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
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onAddClick = {
                    saveError = null
                    showAddDialog = true
                }
            )
        }

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
                            "No hay cuentas registradas"
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
                    items(filtered, key = { it.id }) { cuenta ->
                        CuentaCard(
                            cuenta = cuenta,
                            onEdit = {
                                saveError = null
                                editingCuenta = cuenta
                            },
                            onDelete = {
                                deleteError = null
                                deletingCuenta = cuenta
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
                        text = "Buscar cuenta...",
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
                contentDescription = "Nueva cuenta",
                tint = ElegantBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CuentaCard(
    cuenta: FinanzasCuenta,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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
                    .background(ElegantBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_accounts),
                    contentDescription = null,
                    tint = ElegantBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = cuenta.nombre,
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

@Composable
private fun CuentaFormDialog(
    title: String,
    initialNombre: String,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var nombre by remember { mutableStateOf(initialNombre) }

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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (nombre.isBlank() || isSaving) ElegantBlue.copy(alpha = 0.45f)
                            else ElegantBlue
                        )
                        .clickable(enabled = nombre.isNotBlank() && !isSaving) {
                            onSave(nombre.trim())
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = title,
                color = OnGlass,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                text = "Nombre",
                color = Slate600,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            BasicTextField(
                value = nombre,
                onValueChange = { nombre = it },
                singleLine = true,
                enabled = !isSaving,
                cursorBrush = SolidColor(ElegantBlue),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnGlass, fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                    .background(Slate100.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    if (nombre.isEmpty()) {
                        Text("Ej. Banco Agrícola", color = Slate500, fontSize = 15.sp)
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
private fun DeleteCuentaDialog(
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
                text = "Eliminar cuenta",
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
        Text(text = message, color = IosRed, style = MaterialTheme.typography.bodyMedium)
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

@Preview(showBackground = true, widthDp = 390, heightDp = 700)
@Composable
private fun AccountsPreview() {
    FinanzasPersonalesTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SearchBar(query = "", onQueryChange = {}, onAddClick = {})
                CuentaCard(
                    cuenta = FinanzasCuenta(id = 1, nombre = "Banco Agrícola"),
                    onEdit = {},
                    onDelete = {}
                )
            }
        }
    }
}
