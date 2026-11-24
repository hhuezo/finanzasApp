package hhuezo.finanzas.data.model

data class FinanzasDashboard(
    val mes: Int,
    val anio: Int,
    val balanceTotal: Double,
    val ingresos: Double,
    val gastos: Double,
    val transacciones: List<FinanzasTransaccion> = emptyList(),
)

data class FinanzasTransaccion(
    val id: Long,
    val tipo: String,
    val categoriaId: Long,
    val cuentaId: Long,
    val cantidad: Double,
    val fecha: String,
    val nota: String? = null,
    val categoria: TransaccionCategoria? = null,
    val cuenta: TransaccionCuenta? = null,
) {
    val isIngreso: Boolean get() = tipo.equals("INGRESO", ignoreCase = true)
}

data class TransaccionCategoria(
    val id: Long,
    val nombre: String,
    val icono: String,
    val color: Long,
    val tipo: String,
)

data class TransaccionCuenta(
    val id: Long,
    val nombre: String,
)
