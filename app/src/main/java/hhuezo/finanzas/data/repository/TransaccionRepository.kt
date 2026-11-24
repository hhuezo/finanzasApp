package hhuezo.finanzas.data.repository

import hhuezo.finanzas.data.local.AppDatabase
import hhuezo.finanzas.data.local.TransaccionEntity
import hhuezo.finanzas.data.local.ahora
import hhuezo.finanzas.data.local.limitesMes
import hhuezo.finanzas.data.local.normalizarFecha
import hhuezo.finanzas.data.local.toModel
import hhuezo.finanzas.data.model.FinanzasTransaccion

class TransaccionRepository(
    private val db: AppDatabase = AppDatabase.get(),
) {
    private val dao get() = db.transaccionDao()

    suspend fun listar(
        mes: Int,
        anio: Int,
        tipo: String? = null,
        cuentaId: Long? = null,
    ): Result<List<FinanzasTransaccion>> {
        return try {
            val (desde, hasta) = limitesMes(mes, anio)
            val filas = dao.listar(
                desde = desde,
                hasta = hasta,
                tipo = tipo,
                cuentaId = cuentaId,
                categoriaId = null,
            )
            Result.success(conRelaciones(filas))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crear(
        tipo: String,
        categoriaId: Long,
        cuentaId: Long,
        cantidad: Double,
        fecha: String,
        nota: String?,
    ): Result<FinanzasTransaccion> {
        val errorRelacion = validarRelaciones(categoriaId, cuentaId)
        if (errorRelacion != null) return Result.failure(Exception(errorRelacion))
        return try {
            val ahora = ahora()
            val id = dao.insertar(
                TransaccionEntity(
                    tipo = tipo.uppercase(),
                    categoriaId = categoriaId,
                    cuentaId = cuentaId,
                    cantidad = cantidad,
                    fecha = normalizarFecha(fecha),
                    nota = nota?.trim()?.ifBlank { null },
                    createdAt = ahora,
                    updatedAt = ahora,
                )
            )
            val creada = dao.buscar(id) ?: error("No se pudo leer la transacción creada")
            Result.success(conRelaciones(listOf(creada)).first())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizar(
        id: Long,
        tipo: String,
        categoriaId: Long,
        cuentaId: Long,
        cantidad: Double,
        fecha: String,
        nota: String?,
    ): Result<FinanzasTransaccion> {
        val errorRelacion = validarRelaciones(categoriaId, cuentaId)
        if (errorRelacion != null) return Result.failure(Exception(errorRelacion))
        return try {
            val actual = dao.buscar(id)
                ?: return Result.failure(Exception("No se encontró la transacción"))
            dao.actualizar(
                actual.copy(
                    tipo = tipo.uppercase(),
                    categoriaId = categoriaId,
                    cuentaId = cuentaId,
                    cantidad = cantidad,
                    fecha = normalizarFecha(fecha),
                    nota = nota?.trim()?.ifBlank { null },
                    updatedAt = ahora(),
                )
            )
            val guardada = dao.buscar(id) ?: error("No se pudo leer la transacción actualizada")
            Result.success(conRelaciones(listOf(guardada)).first())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(id: Long): Result<Unit> {
        return try {
            if (dao.buscar(id) == null) {
                return Result.failure(Exception("No se encontró la transacción"))
            }
            dao.eliminar(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun validarRelaciones(categoriaId: Long, cuentaId: Long): String? {
        if (db.categoriaDao().buscar(categoriaId) == null) return "La categoría no existe"
        if (db.cuentaDao().buscar(cuentaId) == null) return "La cuenta no existe"
        return null
    }

    private suspend fun conRelaciones(filas: List<TransaccionEntity>): List<FinanzasTransaccion> {
        val categorias = db.categoriaDao().todas().associateBy { it.id }
        val cuentas = db.cuentaDao().listar().associateBy { it.id }
        return filas.map { fila ->
            fila.toModel(
                categoria = categorias[fila.categoriaId],
                cuenta = cuentas[fila.cuentaId],
            )
        }
    }
}
