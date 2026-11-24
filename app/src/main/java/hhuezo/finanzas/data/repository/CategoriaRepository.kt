package hhuezo.finanzas.data.repository

import hhuezo.finanzas.data.local.AppDatabase
import hhuezo.finanzas.data.local.CategoriaEntity
import hhuezo.finanzas.data.local.ahora
import hhuezo.finanzas.data.local.toModel
import hhuezo.finanzas.data.model.FinanzasCategoria

class CategoriaRepository(
    private val db: AppDatabase = AppDatabase.get(),
) {
    private val dao get() = db.categoriaDao()

    suspend fun listar(
        tipo: String? = null,
        soloActivas: Boolean = true,
    ): Result<List<FinanzasCategoria>> {
        return try {
            Result.success(
                dao.listar(
                    tipo = tipo?.trim()?.ifBlank { null },
                    soloActivas = if (soloActivas) 1 else 0,
                ).map { it.toModel() }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crear(
        nombre: String,
        tipo: String,
        icono: String,
        color: Long,
    ): Result<FinanzasCategoria> {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) {
            return Result.failure(Exception("El nombre es obligatorio"))
        }
        return try {
            val ahora = ahora()
            val id = dao.insertar(
                CategoriaEntity(
                    tipo = tipo.uppercase(),
                    icono = icono,
                    nombre = limpio,
                    color = color,
                    activo = true,
                    createdAt = ahora,
                    updatedAt = ahora,
                )
            )
            val creada = dao.buscar(id) ?: error("No se pudo leer la categoría creada")
            Result.success(creada.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizar(
        id: Long,
        nombre: String,
        tipo: String,
        icono: String,
        color: Long,
        activo: Boolean = true,
    ): Result<FinanzasCategoria> {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) {
            return Result.failure(Exception("El nombre es obligatorio"))
        }
        return try {
            val actual = dao.buscar(id)
                ?: return Result.failure(Exception("No se encontró la categoría"))
            dao.actualizar(
                actual.copy(
                    nombre = limpio,
                    tipo = tipo.uppercase(),
                    icono = icono,
                    color = color,
                    activo = activo,
                    updatedAt = ahora(),
                )
            )
            val guardada = dao.buscar(id) ?: error("No se pudo leer la categoría actualizada")
            Result.success(guardada.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(id: Long): Result<Unit> {
        return try {
            if (dao.buscar(id) == null) {
                return Result.failure(Exception("No se encontró la categoría"))
            }
            if (dao.transaccionesDe(id) > 0 || dao.presupuestosDe(id) > 0) {
                return Result.failure(
                    Exception("La categoría está en uso y no se puede eliminar")
                )
            }
            dao.eliminar(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
