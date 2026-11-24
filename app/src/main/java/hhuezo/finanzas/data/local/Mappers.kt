package hhuezo.finanzas.data.local

import hhuezo.finanzas.data.model.FinanzasCategoria
import hhuezo.finanzas.data.model.FinanzasCuenta
import hhuezo.finanzas.data.model.FinanzasPresupuesto
import hhuezo.finanzas.data.model.FinanzasTransaccion
import hhuezo.finanzas.data.model.PresupuestoCategoria
import hhuezo.finanzas.data.model.TransaccionCategoria
import hhuezo.finanzas.data.model.TransaccionCuenta

internal fun CuentaEntity.toModel() = FinanzasCuenta(
    id = id,
    nombre = nombre,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CategoriaEntity.toModel() = FinanzasCategoria(
    id = id,
    tipo = tipo,
    icono = icono,
    nombre = nombre,
    color = color,
    activo = activo,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun CategoriaEntity.toTransaccionCategoria() = TransaccionCategoria(
    id = id,
    nombre = nombre,
    icono = icono,
    color = color,
    tipo = tipo,
)

internal fun CategoriaEntity.toPresupuestoCategoria() = PresupuestoCategoria(
    id = id,
    nombre = nombre,
    icono = icono,
    color = color,
    tipo = tipo,
    activo = activo,
)

internal fun CuentaEntity.toTransaccionCuenta() = TransaccionCuenta(
    id = id,
    nombre = nombre,
)

internal fun TransaccionEntity.toModel(
    categoria: CategoriaEntity?,
    cuenta: CuentaEntity?,
) = FinanzasTransaccion(
    id = id,
    tipo = tipo,
    categoriaId = categoriaId,
    cuentaId = cuentaId,
    cantidad = cantidad,
    fecha = fecha,
    nota = nota,
    categoria = categoria?.toTransaccionCategoria(),
    cuenta = cuenta?.toTransaccionCuenta(),
)

internal fun PresupuestoEntity.toModel(
    categoria: CategoriaEntity?,
    gastado: Double,
): FinanzasPresupuesto {
    val restante = monto - gastado
    val porcentaje = if (monto > 0.0) (gastado / monto) * 100.0 else 0.0
    return FinanzasPresupuesto(
        id = id,
        categoriaId = categoriaId,
        mes = mes,
        anio = anio,
        monto = monto,
        gastado = gastado,
        restante = restante,
        porcentaje = porcentaje,
        excedido = gastado > monto,
        categoria = categoria?.toPresupuestoCategoria(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
