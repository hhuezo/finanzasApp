package hhuezo.finanzas.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cuentas")
data class CuentaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val icono: String,
    val nombre: String,
    val color: Long,
    val activo: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(
    tableName = "transacciones",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CuentaEntity::class,
            parentColumns = ["id"],
            childColumns = ["cuentaId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("categoriaId"),
        Index("cuentaId"),
        Index("fecha"),
    ],
)
data class TransaccionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: String,
    val categoriaId: Long,
    val cuentaId: Long,
    val cantidad: Double,
    val fecha: String,
    val nota: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(
    tableName = "presupuestos",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["categoriaId", "mes", "anio"], unique = true),
    ],
)
data class PresupuestoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoriaId: Long,
    val mes: Int,
    val anio: Int,
    val monto: Double,
    val createdAt: String,
    val updatedAt: String,
)

data class CategoriaGasto(
    val categoriaId: Long,
    val total: Double,
)
