package com.calendario.app

import android.app.Application
import com.calendario.app.data.AppDatabase

class CalendarioApp : Application() {
    val db by lazy { AppDatabase.get(this) }
}
