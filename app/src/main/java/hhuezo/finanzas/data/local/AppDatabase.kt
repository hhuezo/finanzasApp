package hhuezo.finanzas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        CuentaEntity::class,
        CategoriaEntity::class,
        TransaccionEntity::class,
        PresupuestoEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cuentaDao(): CuentaDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun transaccionDao(): TransaccionDao
    abstract fun presupuestoDao(): PresupuestoDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun init(context: Context) {
            if (instance != null) return
            synchronized(this) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "finanzas.db",
                    )
                        .addCallback(SeedCallback)
                        .build()
                }
            }
        }

        fun get(): AppDatabase = instance ?: error("La base de datos no está inicializada")
    }
}

private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DemoSeed.insertar(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        // Solo si la base está totalmente vacía (p. ej. instalación a medias).
        // No rellena demo tras un import con 0 transacciones.
        val sinCuentas = db.query("SELECT COUNT(*) FROM cuentas").use {
            it.moveToFirst() && it.getLong(0) == 0L
        }
        if (sinCuentas) {
            DemoSeed.insertar(db)
        }
    }
}

private object DemoSeed {
    fun insertar(db: SupportSQLiteDatabase) {
        val ahora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val cal = Calendar.getInstance()
        val mes = cal.get(Calendar.MONTH) + 1
        val anio = cal.get(Calendar.YEAR)
        val diaHoy = cal.get(Calendar.DAY_OF_MONTH)

        /** Fechas de demo siempre dentro del mes de instalación (día 1 .. hoy). */
        fun fechaEnMesActual(diasAtras: Int): String {
            val dia = (diaHoy - diasAtras).coerceAtLeast(1)
            return "%04d-%02d-%02d".format(Locale.US, anio, mes, dia)
        }

        listOf("Efectivo", "Banco", "Tarjeta").forEach { nombre ->
            val existe = db.query(
                "SELECT id FROM cuentas WHERE nombre = ? LIMIT 1",
                arrayOf(nombre),
            ).use { it.moveToFirst() }
            if (!existe) {
                db.execSQL(
                    "INSERT INTO cuentas (nombre, createdAt, updatedAt) VALUES (?, ?, ?)",
                    arrayOf(nombre, ahora, ahora),
                )
            }
        }

        data class CategoriaDemo(
            val tipo: String,
            val icono: String,
            val nombre: String,
            val color: Long,
        )

        val categoriasDemo = listOf(
            CategoriaDemo("GASTO", "Restaurant", "Comida", 0xFFE53935L),
            CategoriaDemo("GASTO", "DirectionsCar", "Transporte", 0xFF1E88E5L),
            CategoriaDemo("GASTO", "Home", "Hogar", 0xFF00897BL),
            CategoriaDemo("GASTO", "MedicalServices", "Salud", 0xFF8E24AAL),
            CategoriaDemo("GASTO", "ShoppingBag", "Compras", 0xFFFB8C00L),
            CategoriaDemo("GASTO", "Theaters", "Entretenimiento", 0xFF5E35B1L),
            CategoriaDemo("INGRESO", "Payments", "Salario", 0xFF43A047L),
            CategoriaDemo("INGRESO", "Savings", "Extra", 0xFF00897BL),
        )
        categoriasDemo.forEach { cat ->
            val existe = db.query(
                "SELECT id FROM categorias WHERE nombre = ? LIMIT 1",
                arrayOf(cat.nombre),
            ).use { it.moveToFirst() }
            if (!existe) {
                db.execSQL(
                    """
                    INSERT INTO categorias (tipo, icono, nombre, color, activo, createdAt, updatedAt)
                    VALUES (?, ?, ?, ?, 1, ?, ?)
                    """.trimIndent(),
                    arrayOf(cat.tipo, cat.icono, cat.nombre, cat.color, ahora, ahora),
                )
            }
        }

        fun idCuenta(nombre: String): Long {
            return db.query("SELECT id FROM cuentas WHERE nombre = ? LIMIT 1", arrayOf(nombre)).use {
                check(it.moveToFirst()) { "No se encontró la cuenta $nombre" }
                it.getLong(0)
            }
        }

        fun idCategoria(nombre: String): Long {
            return db.query("SELECT id FROM categorias WHERE nombre = ? LIMIT 1", arrayOf(nombre)).use {
                check(it.moveToFirst()) { "No se encontró la categoría $nombre" }
                it.getLong(0)
            }
        }

        val efectivo = idCuenta("Efectivo")
        val banco = idCuenta("Banco")
        val tarjeta = idCuenta("Tarjeta")

        val comida = idCategoria("Comida")
        val transporte = idCategoria("Transporte")
        val hogar = idCategoria("Hogar")
        val salud = idCategoria("Salud")
        val compras = idCategoria("Compras")
        val entretenimiento = idCategoria("Entretenimiento")
        val salario = idCategoria("Salario")
        val extra = idCategoria("Extra")

        data class TxDemo(
            val tipo: String,
            val categoriaId: Long,
            val cuentaId: Long,
            val cantidad: Double,
            val fecha: String,
            val nota: String,
        )

        val transacciones = listOf(
            TxDemo("INGRESO", salario, banco, 1850.00, fechaEnMesActual(minOf(12, diaHoy - 1)), "Pago quincenal"),
            TxDemo("INGRESO", extra, efectivo, 75.50, fechaEnMesActual(minOf(8, diaHoy - 1)), "Venta usada"),
            TxDemo("GASTO", comida, efectivo, 18.75, fechaEnMesActual(minOf(1, diaHoy - 1)), "Almuerzo"),
            TxDemo("GASTO", comida, tarjeta, 42.30, fechaEnMesActual(minOf(3, diaHoy - 1)), "Supermercado"),
            TxDemo("GASTO", transporte, efectivo, 6.00, fechaEnMesActual(minOf(2, diaHoy - 1)), "Bus"),
            TxDemo("GASTO", transporte, tarjeta, 28.00, fechaEnMesActual(minOf(5, diaHoy - 1)), "Gasolina"),
            TxDemo("GASTO", hogar, banco, 95.00, fechaEnMesActual(minOf(9, diaHoy - 1)), "Internet"),
            TxDemo("GASTO", salud, tarjeta, 35.00, fechaEnMesActual(minOf(6, diaHoy - 1)), "Farmacia"),
            TxDemo("GASTO", compras, tarjeta, 59.99, fechaEnMesActual(minOf(4, diaHoy - 1)), "Ropa"),
            TxDemo("GASTO", entretenimiento, efectivo, 12.50, fechaEnMesActual(0), "Cine"),
        )

        transacciones.forEach { tx ->
            db.execSQL(
                """
                INSERT INTO transacciones
                (tipo, categoriaId, cuentaId, cantidad, fecha, nota, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    tx.tipo,
                    tx.categoriaId,
                    tx.cuentaId,
                    tx.cantidad,
                    tx.fecha,
                    tx.nota,
                    ahora,
                    ahora,
                ),
            )
        }

        val presupuestosExistentes = db.query(
            "SELECT COUNT(*) FROM presupuestos WHERE mes = ? AND anio = ?",
            arrayOf(mes.toString(), anio.toString()),
        ).use {
            it.moveToFirst()
            it.getLong(0)
        }
        if (presupuestosExistentes == 0L) {
            listOf(
                Triple(comida, 250.0, "Comida"),
                Triple(transporte, 120.0, "Transporte"),
                Triple(entretenimiento, 80.0, "Entretenimiento"),
            ).forEach { (categoriaId, monto, _) ->
                db.execSQL(
                    """
                    INSERT INTO presupuestos
                    (categoriaId, mes, anio, monto, createdAt, updatedAt)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(categoriaId, mes, anio, monto, ahora, ahora),
                )
            }
        }
    }
}

internal fun ahora(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

internal fun limitesMes(mes: Int, anio: Int): Pair<String, String> {
    val inicio = "%04d-%02d-01".format(Locale.US, anio, mes)
    val mesSiguiente = if (mes == 12) 1 else mes + 1
    val anioSiguiente = if (mes == 12) anio + 1 else anio
    val fin = "%04d-%02d-01".format(Locale.US, anioSiguiente, mesSiguiente)
    return inicio to fin
}

internal fun normalizarFecha(fecha: String): String = fecha.trim().take(10)
