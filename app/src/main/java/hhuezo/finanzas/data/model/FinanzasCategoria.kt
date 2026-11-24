package hhuezo.finanzas.data.model

data class FinanzasCategoria(
    val id: Long,
    val tipo: String,
    val icono: String,
    val nombre: String,
    val color: Long,
    val activo: Boolean,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val isGasto: Boolean get() = tipo.equals("GASTO", ignoreCase = true)
    val isIngreso: Boolean get() = tipo.equals("INGRESO", ignoreCase = true)
}
