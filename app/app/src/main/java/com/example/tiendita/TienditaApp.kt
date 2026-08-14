package com.example.tiendita

import android.app.Application
import com.example.tiendita.util.NotificationHelper

class TienditaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.crearCanal(this)
    }
}
