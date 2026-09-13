package com.topnotchlock.workorder.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkOrderEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workOrderDao(): WorkOrderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tnl_work_orders.db"
                )
                    // Bumped for the companyName/companyPhone columns added alongside
                    // multi-company support. History is small and easily regenerated,
                    // so a destructive migration (clears old History rows once on
                    // upgrade, this update only) is simpler than a manual migration.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
