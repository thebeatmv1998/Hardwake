package com.wecker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Alarm::class], version = 6, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile private var instance: AlarmDatabase? = null

        /** v4: Neue Spalte dateEpochDay für Wecker an konkreten Kalenderdaten. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN dateEpochDay INTEGER")
            }
        }

        /** v5: Neue Spalte skipEpochDay für einmalig ausgesetzte Vorkommen. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN skipEpochDay INTEGER")
            }
        }

        /** v6: Neue Spalte wakeCheckUntil für die geplante Aufwachkontrolle. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN wakeCheckUntil INTEGER")
            }
        }

        fun get(context: Context): AlarmDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    "wecker.db"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
