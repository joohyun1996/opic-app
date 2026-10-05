package com.jooh.opic

import android.app.Application
import androidx.room.Room
import com.jooh.opic.core.database.OpicDatabase

class OpicApplication : Application() {
    val database: OpicDatabase by lazy {
        Room.databaseBuilder(this, OpicDatabase::class.java, "opic.db").build()
    }
}
