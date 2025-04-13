package ru.iqchannels.sdk.room

import androidx.room.*
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.iqchannels.sdk.Log

@Database(entities = [DatabaseMessage::class], version = 2, exportSchema = false)
@TypeConverters(SingleChoiceListConverter::class, ActionListConverter::class, FileConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}

//val MIGRATION_1_2 = object : Migration(1, 2) {
//    override fun migrate(db: SupportSQLiteDatabase) {
//        db.execSQL("ALTER TABLE messages ADD COLUMN error INTEGER NOT NULL DEFAULT 0")
//    }
//}

object DatabaseInstance {
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (instance == null) {
            Log.i("database", "init database")
            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "iq_database"
            )
//            .addMigrations(MIGRATION_1_2)
            .build()
        }
        return instance!!
    }
}