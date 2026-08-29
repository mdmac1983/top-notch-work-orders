package com.topnotchlock.workorder.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderDao {
    @Insert
    suspend fun insert(entity: WorkOrderEntity): Long

    @Query("SELECT * FROM work_orders ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM work_orders WHERE id = :id")
    suspend fun getById(id: Long): WorkOrderEntity?

    @Delete
    suspend fun delete(entity: WorkOrderEntity)
}
