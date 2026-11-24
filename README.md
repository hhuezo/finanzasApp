# Finanzas

App Android de finanzas personales **100 % local**. Los datos se guardan en el teléfono con SQLite y no necesita internet ni API.

**Nombre en el dispositivo:** Finanzas  
**Package / applicationId:** `hhuezo.finanzas`

## Características

- **Dashboard** con balance, ingresos, gastos y resumen del mes
- **Transacciones** (ingresos y gastos) por cuenta y categoría
- **Cuentas** (efectivo, banco, tarjeta, etc.)
- **Categorías** personalizables con icono y color
- **Presupuestos** mensuales por categoría
- **Huella opcional** para proteger la app al abrirla o volver del segundo plano
- **Exportar / importar** respaldo en JSON para pasar los datos a otro dispositivo
- Pantalla de primer inicio con explicación de respaldos y activación de huella

## Cómo funciona el almacenamiento

| Qué | Dónde |
|-----|--------|
| Cuentas, categorías, transacciones, presupuestos | SQLite (`finanzas.db`) con Room |
| Preferencia de huella y onboarding | SharedPreferences |

Al instalar, se crean datos de ejemplo del **mes actual** (según el día de instalación) para poder probar la app de inmediato.

### Respaldo

1. **Opciones → Respaldo → Exportar datos**  
   Guarda un archivo `.json` (Drive, archivos, WhatsApp, etc.).
2. **Opciones → Respaldo → Importar datos**  
   Restaura ese archivo en el mismo u otro teléfono.

Al importar se **reemplazan** todos los datos actuales por los del archivo. La app pide confirmación antes.

## Requisitos

- Android 7.0 (API 24) o superior
- Android Studio con el proyecto Gradle incluido
- Kotlin + Jetpack Compose

## Abrir y ejecutar

1. Abre la carpeta del proyecto en Android Studio.
2. Espera a que sincronice Gradle.
3. Ejecuta en un emulador o dispositivo físico.

```bash
./gradlew :app:assembleDebug
```

El APK de depuración queda en `app/build/outputs/apk/debug/`.

## Tecnologías

- Kotlin
- Jetpack Compose + Material 3
- Navigation (Fragments)
- Room (SQLite)
- BiometricPrompt (huella)
- Coroutines

## Estructura principal

```text
app/src/main/java/hhuezo/finanzas/
├── data/
│   ├── local/          # Room: entidades, DAOs, seed
│   ├── backup/         # Exportar / importar JSON
│   ├── repository/     # Acceso a datos para la UI
│   └── model/          # Modelos de dominio
├── security/           # Autenticación biométrica
└── ui/
    ├── dashboard/
    ├── transactions/
    ├── accounts/
    ├── categories/
    ├── budgets/
    ├── options/
    ├── onboarding/     # Primer inicio
    └── security/       # Pantalla de bloqueo
```

## Notas

- No usa red: no hay permiso de internet.
- La huella es opcional; se activa en el primer inicio o en **Opciones → Seguridad**.
- Si desinstalas la app, se borra la base local (salvo que hayas exportado un respaldo antes).
