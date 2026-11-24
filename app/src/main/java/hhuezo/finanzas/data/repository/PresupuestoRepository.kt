package hhuezo.finanzas.data.repository

import hhuezo.finanzas.data.local.AppDatabase
import hhuezo.finanzas.data.local.PresupuestoEntity
import hhuezo.finanzas.data.local.ahora
import hhuezo.finanzas.data.local.limitesMes
import hhuezo.finanzas.data.local.toModel
import hhuezo.finanzas.data.model.FinanzasPresupuesto
import hhuezo.finanzas.data.model.FinanzasPresupuestoResponse

class PresupuestoRepository(
    private val db: AppDatabase = AppDatabase.get(),
) {
    private val dao get() = db.presupuestoDao()

    suspend fun listar(mes: Int, anio: Int): Result<FinanzasPresupuestoResponse> {
        return try {
            val items = aModelos(dao.listar(mes, anio), mes, anio)
            Result.success(
                FinanzasPresupuestoResponse(
                    mes = mes,
                    anio = anio,
                    totalPresupuesto = items.sumOf { it.monto },
                    totalGastado = items.sumOf { it.gastado },
                    items = items,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crear(
        categoriaId: Long,
        mes: Int,
        anio: Int,
        monto: Double,
    ): Result<FinanzasPresupuesto> {
        if (db.categoriaDao().buscar(categoriaId) == null) {
            return Result.failure(Exception("La categoría no existe"))
        }
        if (dao.buscarPorPeriodo(categoriaId, mes, anio) != null) {
            return Result.failure(Exception("Ya existe un presupuesto de esa categoría en este mes"))
        }
        return try {
            val ahora = ahora()
            val id = dao.insertar(
                PresupuestoEntity(
                    categoriaId = categoriaId,
                    mes = mes,
                    anio = anio,
                    monto = monto,
                    createdAt = ahora,
                    updatedAt = ahora,
                )
            )
            val creado = dao.buscar(id) ?: error("No se pudo leer el presupuesto creado")
            Result.success(aModelos(listOf(creado), mes, anio).first())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizar(
        id: Long,
        categoriaId: Long,
        mes: Int,
        anio: Int,
        monto: Double,
    ): Result<FinanzasPresupuesto> {
        val actual = dao.buscar(id)
            ?: return Result.failure(Exception("No se encontró el presupuesto"))
        if (db.categoriaDao().buscar(categoriaId) == null) {
            return Result.failure(Exception("La categoría no existe"))
        }
        val duplicado = dao.buscarPorPeriodo(categoriaId, mes, anio)
        if (duplicado != null && duplicado.id != id) {
            return Result.failure(Exception("Ya existe un presupuesto de esa categoría en este mes"))
        }
        return try {
            dao.actualizar(
                actual.copy(
                    categoriaId = categoriaId,
                    mes = mes,
                    anio = anio,
                    monto = monto,
                    updatedAt = ahora(),
                )
            )
            val guardado = dao.buscar(id) ?: error("No se pudo leer el presupuesto actualizado")
            Result.success(aModelos(listOf(guardado), mes, anio).first())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(id: Long): Result<Unit> {
        return try {
            if (dao.buscar(id) == null) {
                return Result.failure(Exception("No se encontró el presupuesto"))
            }
            dao.eliminar(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun aModelos(
        filas: List<PresupuestoEntity>,
        mes: Int,
        anio: Int,
    ): List<FinanzasPresupuesto> {
        val (desde, hasta) = limitesMes(mes, anio)
        val gastado = db.transaccionDao().gastosPorCategoria(desde, hasta)
            .associate { it.categoriaId to it.total }
        val categorias = db.categoriaDao().todas().associateBy { it.id }
        return filas.map { fila ->
            fila.toModel(
                categoria = categorias[fila.categoriaId],
                gastado = gastado[fila.categoriaId] ?: 0.0,
            )
        }
    }
}
