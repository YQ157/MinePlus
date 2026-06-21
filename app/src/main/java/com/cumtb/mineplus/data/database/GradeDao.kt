package com.cumtb.mineplus.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades ORDER BY semesterId DESC, courseName ASC")
    fun observeAllGrades(): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Query("DELETE FROM grades")
    suspend fun deleteAllGrades()

    @Transaction
    suspend fun replaceAll(grades: List<GradeEntity>) {
        deleteAllGrades()
        insertGrades(grades)
    }
}
