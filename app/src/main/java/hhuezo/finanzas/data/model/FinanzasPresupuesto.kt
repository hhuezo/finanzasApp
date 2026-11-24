package hhuezo.finanzas.data.model

data class FinanzasPresupuestoResponse(
    val mes: Int,
    val anio: Int,
    val totalPresupuesto: Double,
    val totalGastado: Double,
    val items: List<FinanzasPresupuesto> = emptyList(),
)

data class FinanzasPresupuesto(
    val id: Long,
    val categoriaId: Long,
    val mes: Int,
    val anio: Int,
    val monto: Double,
    val gastado: Double = 0.0,
    val restante: Double = 0.0,
    val porcentaje: Double = 0.0,
    val excedido: Boolean = false,
    val categoria: PresupuestoCategoria? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class PresupuestoCategoria(
    val id: Long,
    val nombre: String,
    val icono: String,
    val color: Long,
    val tipo: String,
    val activo: Boolean? = null,
)
