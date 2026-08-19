package com.flowspark.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flowspark.app.data.local.entity.WorkflowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM workflows WHERE id = :id")
    suspend fun getById(id: Long): WorkflowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workflow: WorkflowEntity): Long

    @Delete
    suspend fun delete(workflow: WorkflowEntity)
}
