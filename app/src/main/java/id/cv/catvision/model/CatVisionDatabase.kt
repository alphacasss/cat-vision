package id.cv.catvision.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(entities = [DetectionEntry::class], version = 1, exportSchema = false)
abstract class CatVisionDatabase : RoomDatabase() {
    abstract fun detectionDao(): DetectionDao

    companion object {
        @Volatile
        private var INSTANCE: CatVisionDatabase? = null

        private val MIGRATION_1_2 = Migration(1, 2) {
            // No schema change between v1 and v2; placeholder for future migrations.
        }

        fun getInstance(context: Context): CatVisionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CatVisionDatabase::class.java,
                    "cat_vision.db"
                ).fallbackToDestructiveMigration(false) // SAFETY: do not silently wipe data on schema change.
                    .addMigrations(MIGRATION_1_2) // extend when entities/schema change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
