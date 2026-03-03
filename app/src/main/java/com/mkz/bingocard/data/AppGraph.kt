package com.mkz.bingocard.data

import android.content.Context
import androidx.room.Room
import com.mkz.bingocard.data.db.AppDatabase

object AppGraph {
    @Volatile
    private var db: AppDatabase? = null

    fun database(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bingocard.db"
            ).build().also { db = it }
        }
    }
}
