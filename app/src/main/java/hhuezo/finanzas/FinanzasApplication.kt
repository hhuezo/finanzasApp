package hhuezo.finanzas

import android.app.Application
import hhuezo.finanzas.data.local.AppDatabase

class FinanzasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.init(this)
    }
}
