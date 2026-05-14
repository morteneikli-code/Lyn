package no.lyn.app

import android.app.Application
import no.lyn.app.data.AppDatabase

class LynApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
