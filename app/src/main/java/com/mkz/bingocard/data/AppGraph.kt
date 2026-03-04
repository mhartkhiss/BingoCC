package com.mkz.bingocard.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mkz.bingocard.data.db.AppDatabase

object AppGraph {
    @Volatile
    private var db: AppDatabase? = null

    fun database(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            val MIGRATION_2_3 = object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE patterns ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                }
            }
            
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bingocard.db"
            )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .build()
                .also { db = it }
        }
    }
}
