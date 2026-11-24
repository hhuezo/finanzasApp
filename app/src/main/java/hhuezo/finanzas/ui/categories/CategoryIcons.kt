package hhuezo.finanzas.ui.categories

import androidx.compose.ui.graphics.Color
import hhuezo.finanzas.R

data class CategoryIconOption(
    val apiName: String,
    val drawableRes: Int,
)

val CategoryIconOptions = listOf(
    CategoryIconOption("Home", R.drawable.ic_icon_home),
    CategoryIconOption("Restaurant", R.drawable.ic_icon_restaurant),
    CategoryIconOption("Fastfood", R.drawable.ic_icon_fastfood),
    CategoryIconOption("LocalCafe", R.drawable.ic_icon_cafe),
    CategoryIconOption("LocalGroceryStore", R.drawable.ic_icon_grocery),
    CategoryIconOption("ShoppingBag", R.drawable.ic_icon_shopping),
    CategoryIconOption("DirectionsCar", R.drawable.ic_icon_car),
    CategoryIconOption("DirectionsBus", R.drawable.ic_icon_bus),
    CategoryIconOption("LocalGasStation", R.drawable.ic_icon_gas),
    CategoryIconOption("Flight", R.drawable.ic_icon_flight),
    CategoryIconOption("MedicalServices", R.drawable.ic_icon_medical),
    CategoryIconOption("FitnessCenter", R.drawable.ic_icon_fitness),
    CategoryIconOption("Pets", R.drawable.ic_icon_pets),
    CategoryIconOption("ChildFriendly", R.drawable.ic_icon_childcare),
    CategoryIconOption("School", R.drawable.ic_icon_school),
    CategoryIconOption("MenuBook", R.drawable.ic_icon_book),
    CategoryIconOption("Work", R.drawable.ic_icon_work),
    CategoryIconOption("Payments", R.drawable.ic_icon_payments),
    CategoryIconOption("AccountBalanceWallet", R.drawable.ic_icon_wallet),
    CategoryIconOption("AccountBalance", R.drawable.ic_icon_bank),
    CategoryIconOption("CreditCard", R.drawable.ic_icon_creditcard),
    CategoryIconOption("Tv", R.drawable.ic_icon_tv),
    CategoryIconOption("PhoneAndroid", R.drawable.ic_icon_phone),
    CategoryIconOption("Wifi", R.drawable.ic_icon_wifi),
    CategoryIconOption("Hotel", R.drawable.ic_icon_bolt),
    CategoryIconOption("Woman", R.drawable.ic_icon_woman),
    CategoryIconOption("Theaters", R.drawable.ic_icon_movie),
    CategoryIconOption("FamilyRestroom", R.drawable.ic_icon_family),
    CategoryIconOption("PhotoCamera", R.drawable.ic_icon_camera),
    CategoryIconOption("DryCleaning", R.drawable.ic_icon_clothes),
    CategoryIconOption("Shoes", R.drawable.ic_icon_shoes),
    CategoryIconOption("Savings", R.drawable.ic_icon_savings),
)

/** Nombres antiguos → recurso actual (categorías ya guardadas en API). */
private val LegacyIconAliases = mapOf(
    "ChildCare" to R.drawable.ic_icon_childcare,
    "CardGiftcard" to R.drawable.ic_icon_tv,
    "Movie" to R.drawable.ic_icon_movie,
    "SportsEsports" to R.drawable.ic_icon_family,
    "MusicNote" to R.drawable.ic_icon_camera,
    "ContentCut" to R.drawable.ic_icon_clothes,
    "Checkroom" to R.drawable.ic_icon_clothes,
    "IceSkating" to R.drawable.ic_icon_shoes,
    "Bolt" to R.drawable.ic_icon_bolt,
    "WaterDrop" to R.drawable.ic_icon_woman,
    "Spa" to R.drawable.ic_icon_woman,
)

/** Paleta exacta 6 columnas × 4 filas = 24 colores */
val CategoryColorOptions = listOf(
    // fila 1
    Color(0xFFE53935),
    Color(0xFFD81B60),
    Color(0xFF8E24AA),
    Color(0xFF5E35B1),
    Color(0xFF3949AB),
    Color(0xFF14213D),
    // fila 2
    Color(0xFF1E88E5),
    Color(0xFF039BE5),
    Color(0xFF00ACC1),
    Color(0xFF00897B),
    Color(0xFF43A047),
    Color(0xFF7CB342),
    // fila 3
    Color(0xFFFDD835),
    Color(0xFFFB8C00),
    Color(0xFFF4511E),
    Color(0xFF6D4C41),
    Color(0xFF546E7A),
    Color(0xFFEC407A),
    // fila 4
    Color(0xFF26A69A),
    Color(0xFFAB47BC),
    Color(0xFFFF7043),
    Color(0xFF66BB6A),
    Color(0xFF42A5F5),
    Color(0xFFFFA726),
)

fun iconResForName(name: String): Int {
    CategoryIconOptions
        .firstOrNull { it.apiName.equals(name, ignoreCase = true) }
        ?.drawableRes
        ?.let { return it }
    LegacyIconAliases.entries
        .firstOrNull { it.key.equals(name, ignoreCase = true) }
        ?.value
        ?.let { return it }
    return R.drawable.ic_categories
}

fun colorToApiLong(color: Color): Long {
    val argb = android.graphics.Color.argb(
        (color.alpha * 255).toInt().coerceIn(0, 255),
        (color.red * 255).toInt().coerceIn(0, 255),
        (color.green * 255).toInt().coerceIn(0, 255),
        (color.blue * 255).toInt().coerceIn(0, 255),
    )
    return argb.toLong() and 0xFFFFFFFFL
}

fun colorFromApiLong(value: Long): Color {
    val normalized = value and 0xFFFFFFFFL
    return CategoryColorOptions.firstOrNull { colorToApiLong(it) == normalized }
        ?: Color(value.toInt())
}
