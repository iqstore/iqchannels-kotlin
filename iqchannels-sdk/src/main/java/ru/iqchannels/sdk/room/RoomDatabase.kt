package ru.iqchannels.sdk.room

import androidx.room.*
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.iqchannels.sdk.IQLog

@Database(entities = [DatabaseMessage::class], version = 3, exportSchema = false)
@TypeConverters(SingleChoiceListConverter::class, ActionListConverter::class, FileConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN fileIds TEXT")
        db.execSQL("ALTER TABLE messages ADD COLUMN files TEXT")
    }
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
            IQLog.i("database", "init database")
            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "iq_database"
            )
                .addMigrations(MIGRATION_2_3)
                .build()
        }
        return instance!!
    }
}