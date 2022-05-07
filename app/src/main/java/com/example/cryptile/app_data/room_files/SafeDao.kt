package com.example.cryptile.app_data.room_files

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(safeData: SafeData)

    @Update
    suspend fun update(safeData: SafeData)

    @Delete
    suspend fun delete(safeData: SafeData)

    @Query("SELECT id FROM safe_database")
    fun getListOfIds(): Flow<List<Int>>

    @Query("SELECT * FROM safe_database where id = :id")
    fun getById(id: Int): Flow<SafeData>
}