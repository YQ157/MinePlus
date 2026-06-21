package com.cumtb.mineplus.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey val id: Long,
    val semesterId: Int,
    val semesterName: String,
    val courseName: String,
    val courseCode: String,
    val lessonCode: String,
    val minorCourseName: String?,
    val credits: Double?,
    val grade: String,
    val makeupGrade: String?,
    val gp: Double?,
    val passed: Boolean?,
    val calculateGp: Boolean,
    val practice: Boolean?,
    val courseType: String?,
    val courseProperty: String?,
    val compulsory: Boolean?
)
