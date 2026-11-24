package hhuezo.finanzas.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hhuezo.finanzas.R
import hhuezo.finanzas.ui.theme.FinanzasPersonalesTheme
import hhuezo.finanzas.ui.theme.GlassSurfaceStrong
import hhuezo.finanzas.ui.theme.IosBlue
import hhuezo.finanzas.ui.theme.OnGlassSecondary

data class GlassTabItem(
    val id: String,
    val label: String,
    @param:DrawableRes val icon: Int,
)

@Composable
fun GlassBottomTabBar(
    items: List<GlassTabItem>,
    selectedId: String,
    onTabSelected: (GlassTabItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xE6FFFFFF),
                        Color(0xF2F7FAFF)
                    )
                )
            )
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0x4D3C3C43)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                GlassTabButton(
                    item = item,
                    selected = item.id == selectedId,
                    onClick = { onTabSelected(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GlassTabButton(
    item: GlassTabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) IosBlue else OnGlassSecondary,
        label = "tabIconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) IosBlue else OnGlassSecondary,
        label = "tabLabelColor"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = (-0.08).sp
            ),
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, device = Devices.PHONE, name = "Bottom tab glass")
@Composable
private fun GlassBottomTabBarPreview() {
    FinanzasPersonalesTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurfaceStrong)
                .padding(top = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            GlassBottomTabBar(
                items = listOf(
                    GlassTabItem("dashboard", "Dashboard", R.drawable.ic_dashboard),
                    GlassTabItem("transacciones", "Transacciones", R.drawable.ic_transactions),
                    GlassTabItem("opciones", "Opciones", R.drawable.ic_options),
                ),
                selectedId = "transacciones",
                onTabSelected = {}
            )
        }
    }
}
