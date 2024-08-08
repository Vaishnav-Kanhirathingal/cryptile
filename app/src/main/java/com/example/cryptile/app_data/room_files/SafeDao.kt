package com.example.cryptile.app_data.room_files

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(safeData: SafeData)

    @Update
    suspend fun update(safeData: SafeData)

    @Delete
    suspend fun delete(safeData: SafeData)

    @Query("SELECT id FROM safe_database")
    fun getListOfIds(): Flow<List<Int>>

    @Query("SELECT * FROM safe_database where id = :id")
    fun getById(id: Int): Flow<SafeData>

    @Query("DELETE FROM safe_database")
    suspend fun deleteAll()
}