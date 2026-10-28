package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeSourceDao {
    @Query("SELECT * FROM income_sources ORDER BY id ASC")
    fun getAllIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Query("SELECT * FROM income_sources WHERE id = :sourceId")
    suspend fun getIncomeSourceById(sourceId: Long): IncomeSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(source: IncomeSourceEntity): Long

    @Update
    suspend fun updateIncomeSource(source: IncomeSourceEntity)

    @Delete
    suspend fun deleteIncomeSource(source: IncomeSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSources(sources: List<IncomeSourceEntity>)

    @Query("DELETE FROM income_sources")
    suspend fun deleteAllIncomeSources()
}
