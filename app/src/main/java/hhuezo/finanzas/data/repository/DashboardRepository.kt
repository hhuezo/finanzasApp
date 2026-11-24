package hhuezo.finanzas.data.repository

import hhuezo.finanzas.data.local.AppDatabase
import hhuezo.finanzas.data.local.limitesMes
import hhuezo.finanzas.data.local.toModel
import hhuezo.finanzas.data.model.FinanzasDashboard

class DashboardRepository(
    private val db: AppDatabase = AppDatabase.get(),
) {

    suspend fun obtener(mes: Int, anio: Int): Result<FinanzasDashboard> {
        return try {
            val (desde, hasta) = limitesMes(mes, anio)
            val transacciones = db.transaccionDao().listar(
                desde = desde,
                hasta = hasta,
                tipo = null,
                cuentaId = null,
                categoriaId = null,
            )
            val categorias = db.categoriaDao().todas().associateBy { it.id }
            val cuentas = db.cuentaDao().listar().associateBy { it.id }
            val ingresos = db.transaccionDao().sumar("INGRESO", desde, hasta)
            val gastos = db.transaccionDao().sumar("GASTO", desde, hasta)
            Result.success(
                FinanzasDashboard(
                    mes = mes,
                    anio = anio,
                    balanceTotal = ingresos - gastos,
                    ingresos = ingresos,
                    gastos = gastos,
                    transacciones = transacciones.map { fila ->
                        fila.toModel(
                            categoria = categorias[fila.categoriaId],
                            cuenta = cuentas[fila.cuentaId],
                        )
                    },
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
