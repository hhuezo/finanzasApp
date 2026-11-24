package hhuezo.finanzas.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hhuezo.finanzas.R
import hhuezo.finanzas.ui.components.AppBottomSheet
import hhuezo.finanzas.ui.theme.ElegantBlue
import hhuezo.finanzas.ui.theme.IosRed
import hhuezo.finanzas.ui.theme.OnGlass
import hhuezo.finanzas.ui.theme.Slate100
import hhuezo.finanzas.ui.theme.Slate200
import hhuezo.finanzas.ui.theme.Slate500
import hhuezo.finanzas.ui.theme.Slate600
import hhuezo.finanzas.ui.theme.WarmPill

@Composable
fun AddCategoryDialog(
    initialTipo: String,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (nombre: String, tipo: String, icono: String, color: Long) -> Unit,
) {
    CategoryFormDialog(
        title = "Nueva categoría",
        initialNombre = "",
        initialTipo = initialTipo,
        initialIcono = CategoryIconOptions.first().apiName,
        initialColor = CategoryColorOptions.first(),
        isSaving = isSaving,
        errorMessage = errorMessage,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

@Composable
fun EditCategoryDialog(
    initialNombre: String,
    initialTipo: String,
    initialIcono: String,
    initialColor: Long,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (nombre: String, tipo: String, icono: String, color: Long) -> Unit,
) {
    CategoryFormDialog(
        title = "Modificar categoría",
        initialNombre = initialNombre,
        initialTipo = initialTipo,
        initialIcono = initialIcono,
        initialColor = colorFromApiLong(initialColor),
        isSaving = isSaving,
        errorMessage = errorMessage,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

@Composable
private fun CategoryFormDialog(
    title: String,
    initialNombre: String,
    initialTipo: String,
    initialIcono: String,
    initialColor: Color,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (nombre: String, tipo: String, icono: String, color: Long) -> Unit,
) {
    var nombre by remember { mutableStateOf(initialNombre) }
    var tipo by remember { mutableStateOf(initialTipo.uppercase()) }
    var selectedIcon by remember {
        mutableStateOf(
            CategoryIconOptions
                .firstOrNull { it.apiName.equals(initialIcono, ignoreCase = true) }
                ?.apiName
                ?: CategoryIconOptions.first().apiName
        )
    }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AppBottomSheet(
        onDismiss = { if (!isSaving) onDismiss() },
        contentHeightFraction = 0.78f,
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
                            onSave(
                                nombre.trim(),
                                tipo,
                                selectedIcon,
                                colorToApiLong(selectedColor)
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
                fontSize = 18.sp
            )

            FieldLabel("Nombre")
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
                        Text("Ej. Supermercado", color = Slate500, fontSize = 15.sp)
                    }
                    inner()
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
                TipoChip(
                    label = "Gasto",
                    selected = tipo == "GASTO",
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onClick = { tipo = "GASTO" }
                )
                TipoChip(
                    label = "Ingreso",
                    selected = tipo == "INGRESO",
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    onClick = { tipo = "INGRESO" }
                )
            }

            FieldLabel("Icono")
            IconGrid(
                selectedIcon = selectedIcon,
                selectedColor = selectedColor,
                enabled = !isSaving,
                onSelect = { selectedIcon = it }
            )

            FieldLabel("Color")
            ColorGrid(
                selectedColor = selectedColor,
                enabled = !isSaving,
                onSelect = { selectedColor = it }
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = IosRed,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun IconGrid(
    selectedIcon: String,
    selectedColor: Color,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    // ~3.55 columnas: se pelea una franja del siguiente icono a la derecha
    val visibleColumns = 3.55f
    val rows = 4
    val cellHeight = 48.dp
    val spacing = 8.dp
    val gridHeight = cellHeight * rows + spacing * (rows - 1)
    val gridState = rememberLazyGridState()
    val canScrollMore by remember {
        derivedStateOf { gridState.canScrollForward }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth - spacing * 3) / visibleColumns

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
        ) {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(rows),
                state = gridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                contentPadding = PaddingValues(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                items(CategoryIconOptions, key = { it.apiName }) { option ->
                    val selected = selectedIcon == option.apiName
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(cellHeight)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) selectedColor.copy(alpha = 0.18f) else Slate100
                            )
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) selectedColor else Slate200,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = enabled) { onSelect(option.apiName) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(option.drawableRes),
                            contentDescription = option.apiName,
                            tint = if (selected) selectedColor else Slate600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (canScrollMore) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(36.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0f),
                                    Color.White.copy(alpha = 0.92f),
                                    Color.White
                                )
                            )
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = "Más iconos",
                        tint = Slate500,
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorGrid(
    selectedColor: Color,
    enabled: Boolean,
    onSelect: (Color) -> Unit,
) {
    val rows = CategoryColorOptions.chunked(6)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { color ->
                    val selected = colorToApiLong(selectedColor) == colorToApiLong(color)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable(enabled = enabled) { onSelect(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(2.5.dp, color, CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(if (selected) 28.dp else 30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) Color.White else Slate200.copy(alpha = 0.55f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
private fun TipoChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) ElegantBlue else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Slate600,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}
