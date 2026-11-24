package hhuezo.finanzas.data.repository

import hhuezo.finanzas.data.local.AppDatabase
import hhuezo.finanzas.data.local.CuentaEntity
import hhuezo.finanzas.data.local.ahora
import hhuezo.finanzas.data.local.toModel
import hhuezo.finanzas.data.model.FinanzasCuenta

class CuentaRepository(
    private val db: AppDatabase = AppDatabase.get(),
) {
    private val dao get() = db.cuentaDao()

    suspend fun listar(): Result<List<FinanzasCuenta>> {
        return try {
            Result.success(dao.listar().map { it.toModel() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crear(nombre: String): Result<FinanzasCuenta> {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) {
            return Result.failure(Exception("El nombre es obligatorio"))
        }
        return try {
            val ahora = ahora()
            val id = dao.insertar(
                CuentaEntity(
                    nombre = limpio,
                    createdAt = ahora,
                    updatedAt = ahora,
                )
            )
            val creada = dao.buscar(id) ?: error("No se pudo leer la cuenta creada")
            Result.success(creada.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizar(id: Long, nombre: String): Result<FinanzasCuenta> {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) {
            return Result.failure(Exception("El nombre es obligatorio"))
        }
        return try {
            val actual = dao.buscar(id)
                ?: return Result.failure(Exception("No se encontró la cuenta"))
            dao.actualizar(actual.copy(nombre = limpio, updatedAt = ahora()))
            val guardada = dao.buscar(id) ?: error("No se pudo leer la cuenta actualizada")
            Result.success(guardada.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(id: Long): Result<Unit> {
        return try {
            if (dao.buscar(id) == null) {
                return Result.failure(Exception("No se encontró la cuenta"))
            }
            if (dao.transaccionesDe(id) > 0) {
                return Result.failure(Exception("La cuenta tiene transacciones y no se puede eliminar"))
            }
            dao.eliminar(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
