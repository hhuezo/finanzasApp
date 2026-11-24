package hhuezo.finanzas.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import hhuezo.finanzas.data.local.AppDatabase
import hhuezo.finanzas.data.local.CategoriaEntity
import hhuezo.finanzas.data.local.CuentaEntity
import hhuezo.finanzas.data.local.PresupuestoEntity
import hhuezo.finanzas.data.local.TransaccionEntity
import hhuezo.finanzas.data.local.ahora
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(
    private val db: AppDatabase = AppDatabase.get(),
) {
    data class Resumen(
        val cuentas: Int,
        val categorias: Int,
        val transacciones: Int,
        val presupuestos: Int,
    )

    suspend fun exportar(context: Context, uri: Uri): Result<Resumen> {
        return try {
            val cuentas = db.cuentaDao().listar()
            val categorias = db.categoriaDao().todas()
            val transacciones = db.transaccionDao().todas()
            val presupuestos = db.presupuestoDao().todos()

            val root = JSONObject()
                .put("app", "hhuezo.finanzas")
                .put("version", BACKUP_VERSION)
                .put("exportedAt", ahora())
                .put("cuentas", JSONArray().also { arr ->
                    cuentas.forEach { c ->
                        arr.put(
                            JSONObject()
                                .put("id", c.id)
                                .put("nombre", c.nombre)
                                .put("createdAt", c.createdAt)
                                .put("updatedAt", c.updatedAt)
                        )
                    }
                })
                .put("categorias", JSONArray().also { arr ->
                    categorias.forEach { c ->
                        arr.put(
                            JSONObject()
                                .put("id", c.id)
                                .put("tipo", c.tipo)
                                .put("icono", c.icono)
                                .put("nombre", c.nombre)
                                .put("color", c.color)
                                .put("activo", c.activo)
                                .put("createdAt", c.createdAt)
                                .put("updatedAt", c.updatedAt)
                        )
                    }
                })
                .put("transacciones", JSONArray().also { arr ->
                    transacciones.forEach { t ->
                        arr.put(
                            JSONObject()
                                .put("id", t.id)
                                .put("tipo", t.tipo)
                                .put("categoriaId", t.categoriaId)
                                .put("cuentaId", t.cuentaId)
                                .put("cantidad", t.cantidad)
                                .put("fecha", t.fecha)
                                .put("nota", t.nota)
                                .put("createdAt", t.createdAt)
                                .put("updatedAt", t.updatedAt)
                        )
                    }
                })
                .put("presupuestos", JSONArray().also { arr ->
                    presupuestos.forEach { p ->
                        arr.put(
                            JSONObject()
                                .put("id", p.id)
                                .put("categoriaId", p.categoriaId)
                                .put("mes", p.mes)
                                .put("anio", p.anio)
                                .put("monto", p.monto)
                                .put("createdAt", p.createdAt)
                                .put("updatedAt", p.updatedAt)
                        )
                    }
                })

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return Result.failure(Exception("No se pudo escribir el archivo"))

            Result.success(
                Resumen(
                    cuentas = cuentas.size,
                    categorias = categorias.size,
                    transacciones = transacciones.size,
                    presupuestos = presupuestos.size,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importar(context: Context, uri: Uri): Result<Resumen> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            } ?: return Result.failure(Exception("No se pudo leer el archivo"))

            val root = JSONObject(json)
            val version = root.optInt("version", 0)
            if (version != BACKUP_VERSION) {
                return Result.failure(Exception("Este archivo de respaldo no es compatible"))
            }

            val cuentas = root.getJSONArray("cuentas").toCuentaEntities()
            val categorias = root.getJSONArray("categorias").toCategoriaEntities()
            val transacciones = root.getJSONArray("transacciones").toTransaccionEntities()
            val presupuestos = root.getJSONArray("presupuestos").toPresupuestoEntities()

            if (cuentas.isEmpty()) {
                return Result.failure(Exception("El respaldo no tiene cuentas"))
            }

            db.withTransaction {
                db.transaccionDao().eliminarTodas()
                db.presupuestoDao().eliminarTodos()
                db.categoriaDao().eliminarTodas()
                db.cuentaDao().eliminarTodos()

                db.cuentaDao().insertarTodos(cuentas)
                db.categoriaDao().insertarTodas(categorias)
                db.transaccionDao().insertarTodas(transacciones)
                db.presupuestoDao().insertarTodos(presupuestos)
            }

            Result.success(
                Resumen(
                    cuentas = cuentas.size,
                    categorias = categorias.size,
                    transacciones = transacciones.size,
                    presupuestos = presupuestos.size,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val BACKUP_VERSION = 1
        const val MIME_TYPE = "application/json"

        fun nombreArchivoSugerido(): String {
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            return "finanzas-respaldo-$stamp.json"
        }
    }
}

private fun JSONArray.toCuentaEntities(): List<CuentaEntity> {
    return buildList {
        for (i in 0 until length()) {
            val o = getJSONObject(i)
            add(
                CuentaEntity(
                    id = o.getLong("id"),
                    nombre = o.getString("nombre"),
                    createdAt = o.optString("createdAt", ahora()),
                    updatedAt = o.optString("updatedAt", ahora()),
                )
            )
        }
    }
}

private fun JSONArray.toCategoriaEntities(): List<CategoriaEntity> {
    return buildList {
        for (i in 0 until length()) {
            val o = getJSONObject(i)
            add(
                CategoriaEntity(
                    id = o.getLong("id"),
                    tipo = o.getString("tipo"),
                    icono = o.getString("icono"),
                    nombre = o.getString("nombre"),
                    color = o.getLong("color"),
                    activo = o.optBoolean("activo", true),
                    createdAt = o.optString("createdAt", ahora()),
                    updatedAt = o.optString("updatedAt", ahora()),
                )
            )
        }
    }
}

private fun JSONArray.toTransaccionEntities(): List<TransaccionEntity> {
    return buildList {
        for (i in 0 until length()) {
            val o = getJSONObject(i)
            add(
                TransaccionEntity(
                    id = o.getLong("id"),
                    tipo = o.getString("tipo"),
                    categoriaId = o.getLong("categoriaId"),
                    cuentaId = o.getLong("cuentaId"),
                    cantidad = o.getDouble("cantidad"),
                    fecha = o.getString("fecha"),
                    nota = if (o.isNull("nota")) null else o.optString("nota"),
                    createdAt = o.optString("createdAt", ahora()),
                    updatedAt = o.optString("updatedAt", ahora()),
                )
            )
        }
    }
}

private fun JSONArray.toPresupuestoEntities(): List<PresupuestoEntity> {
    return buildList {
        for (i in 0 until length()) {
            val o = getJSONObject(i)
            add(
                PresupuestoEntity(
                    id = o.getLong("id"),
                    categoriaId = o.getLong("categoriaId"),
                    mes = o.getInt("mes"),
                    anio = o.getInt("anio"),
                    monto = o.getDouble("monto"),
                    createdAt = o.optString("createdAt", ahora()),
                    updatedAt = o.optString("updatedAt", ahora()),
                )
            )
        }
    }
}
