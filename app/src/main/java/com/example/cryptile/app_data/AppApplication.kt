package com.example.cryptile.app_data

import android.app.Application
import com.example.cryptile.app_data.room_files.SafeDatabase

class AppApplication : Application() {
    val database: SafeDatabase by lazy { SafeDatabase.getDatabase(this) }
}