package com.mkz.bingocard.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mkz.bingocard.data.db.dao.CalledNumberDao
import com.mkz.bingocard.data.db.dao.CardDao
import com.mkz.bingocard.data.db.dao.ActivePatternDao
import com.mkz.bingocard.data.db.dao.PatternDao
import com.mkz.bingocard.data.db.entities.ActivePatternEntity
import com.mkz.bingocard.data.db.entities.CalledNumberEntity
import com.mkz.bingocard.data.db.entities.CardEntity
import com.mkz.bingocard.data.db.entities.CellEntity
import com.mkz.bingocard.data.db.entities.PatternEntity

@Database(
    entities = [
        CardEntity::class,
        CellEntity::class,
        PatternEntity::class,
        CalledNumberEntity::class,
        ActivePatternEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun patternDao(): PatternDao
    abstract fun calledNumberDao(): CalledNumberDao
    abstract fun activePatternDao(): ActivePatternDao
}
