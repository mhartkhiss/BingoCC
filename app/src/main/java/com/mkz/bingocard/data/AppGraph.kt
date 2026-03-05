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
            val MIGRATION_3_4 = object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE cards ADD COLUMN historicalWins INTEGER NOT NULL DEFAULT 0")
                }
            }
            val MIGRATION_4_5 = object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE cards ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
                }
            }
            
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bingocard.db"
            )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(true)
                .build()
                .also { db = it }
        }
    }
}
