package hhuezo.finanzas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CuentaDao {
    @Query("SELECT * FROM cuentas ORDER BY nombre COLLATE NOCASE")
    suspend fun listar(): List<CuentaEntity>

    @Query("SELECT * FROM cuentas WHERE id = :id")
    suspend fun buscar(id: Long): CuentaEntity?

    @Insert
    suspend fun insertar(cuenta: CuentaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(cuentas: List<CuentaEntity>)

    @Update
    suspend fun actualizar(cuenta: CuentaEntity)

    @Query("DELETE FROM cuentas WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("DELETE FROM cuentas")
    suspend fun eliminarTodos()

    @Query("SELECT COUNT(*) FROM transacciones WHERE cuentaId = :cuentaId")
    suspend fun transaccionesDe(cuentaId: Long): Int
}

@Dao
interface CategoriaDao {
    @Query(
        """
        SELECT * FROM categorias
        WHERE (:tipo IS NULL OR UPPER(tipo) = UPPER(:tipo))
          AND (:soloActivas = 0 OR activo = 1)
        ORDER BY nombre COLLATE NOCASE
        """
    )
    suspend fun listar(tipo: String?, soloActivas: Int): List<CategoriaEntity>

    @Query("SELECT * FROM categorias")
    suspend fun todas(): List<CategoriaEntity>

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun buscar(id: Long): CategoriaEntity?

    @Insert
    suspend fun insertar(categoria: CategoriaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(categorias: List<CategoriaEntity>)

    @Update
    suspend fun actualizar(categoria: CategoriaEntity)

    @Query("DELETE FROM categorias WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("DELETE FROM categorias")
    suspend fun eliminarTodas()

    @Query("SELECT COUNT(*) FROM transacciones WHERE categoriaId = :categoriaId")
    suspend fun transaccionesDe(categoriaId: Long): Int

    @Query("SELECT COUNT(*) FROM presupuestos WHERE categoriaId = :categoriaId")
    suspend fun presupuestosDe(categoriaId: Long): Int
}

@Dao
interface TransaccionDao {
    @Query(
        """
        SELECT * FROM transacciones
        WHERE fecha >= :desde AND fecha < :hasta
          AND (:tipo IS NULL OR UPPER(tipo) = UPPER(:tipo))
          AND (:cuentaId IS NULL OR cuentaId = :cuentaId)
          AND (:categoriaId IS NULL OR categoriaId = :categoriaId)
        ORDER BY fecha DESC, id DESC
        """
    )
    suspend fun listar(
        desde: String,
        hasta: String,
        tipo: String?,
        cuentaId: Long?,
        categoriaId: Long?,
    ): List<TransaccionEntity>

    @Query("SELECT * FROM transacciones WHERE id = :id")
    suspend fun buscar(id: Long): TransaccionEntity?

    @Query("SELECT * FROM transacciones ORDER BY fecha DESC, id DESC")
    suspend fun todas(): List<TransaccionEntity>

    @Insert
    suspend fun insertar(transaccion: TransaccionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(transacciones: List<TransaccionEntity>)

    @Update
    suspend fun actualizar(transaccion: TransaccionEntity)

    @Query("DELETE FROM transacciones WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("DELETE FROM transacciones")
    suspend fun eliminarTodas()

    @Query(
        """
        SELECT COALESCE(SUM(cantidad), 0.0) FROM transacciones
        WHERE UPPER(tipo) = UPPER(:tipo)
          AND fecha >= :desde AND fecha < :hasta
        """
    )
    suspend fun sumar(tipo: String, desde: String, hasta: String): Double

    @Query(
        """
        SELECT categoriaId, COALESCE(SUM(cantidad), 0.0) AS total
        FROM transacciones
        WHERE UPPER(tipo) = 'GASTO'
          AND fecha >= :desde AND fecha < :hasta
        GROUP BY categoriaId
        """
    )
    suspend fun gastosPorCategoria(desde: String, hasta: String): List<CategoriaGasto>
}

@Dao
interface PresupuestoDao {
    @Query(
        """
        SELECT * FROM presupuestos
        WHERE mes = :mes AND anio = :anio
        ORDER BY id DESC
        """
    )
    suspend fun listar(mes: Int, anio: Int): List<PresupuestoEntity>

    @Query("SELECT * FROM presupuestos WHERE id = :id")
    suspend fun buscar(id: Long): PresupuestoEntity?

    @Query("SELECT * FROM presupuestos ORDER BY anio DESC, mes DESC, id DESC")
    suspend fun todos(): List<PresupuestoEntity>

    @Query(
        """
        SELECT * FROM presupuestos
        WHERE categoriaId = :categoriaId AND mes = :mes AND anio = :anio
        LIMIT 1
        """
    )
    suspend fun buscarPorPeriodo(categoriaId: Long, mes: Int, anio: Int): PresupuestoEntity?

    @Insert
    suspend fun insertar(presupuesto: PresupuestoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(presupuestos: List<PresupuestoEntity>)

    @Update
    suspend fun actualizar(presupuesto: PresupuestoEntity)

    @Query("DELETE FROM presupuestos WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("DELETE FROM presupuestos")
    suspend fun eliminarTodos()
}
