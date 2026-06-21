package com.cumtb.mineplus.data.model

data class GradeResponse(
    val semesterId2studentGrades: Map<String, List<StudentGradeJson>?>?,
    val semesters: List<GradeSemesterJson>?,
    val id2semesters: Map<String, GradeSemesterJson>?,
    val studentGradeList: GradeResponse?,
    val studentGrades: List<StudentGradeJson>?,
    val grades: List<StudentGradeJson>?,
    val result: GradeResponse?
)

data class StudentGradeJson(
    val id: Long?,
    val semester: GradeSemesterJson?,
    val course: GradeCourseJson?,
    val lessonCode: String?,
    val minorCourse: GradeCourseJson?,
    val gaGrade: String?,
    val makeupGaGrade: String?,
    val passed: Boolean?,
    val gp: Double?,
    val published: Boolean?,
    val courseType: GradeNameJson?,
    val courseProperty: GradeNameJson?,
    val compulsory: Boolean?,
    val displayShowStudentGrade: Boolean?
)

data class GradeSemesterJson(
    val id: Int?,
    val nameZh: String?,
    val nameEn: String?,
    val schoolYear: String?,
    val code: String?
)

data class GradeCourseJson(
    val nameZh: String?,
    val code: String?,
    val credits: Double?,
    val calculateGp: Boolean?,
    val practice: Boolean?
)

data class GradeNameJson(
    val nameZh: String?,
    val name: String?
)

data class GradeData(
    val semesters: List<GradeSemester>,
    val gradesBySemester: Map<Int, List<GradeItem>>,
    val defaultSemesterId: Int?
)

data class GradeSemester(
    val id: Int,
    val name: String
)

data class GradeItem(
    val id: Long,
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

data class GradeSummary(
    val courseCount: Int,
    val totalCredits: Double,
    val gpa: Double?,
    val weightedAverage: Double?,
    val passedCount: Int,
    val failedCount: Int,
    val gpaCredits: Double,
    val weightedCredits: Double
)

fun List<GradeItem>.calculateGradeSummary(): GradeSummary {
    val totalCredits = sumOf { it.credits ?: 0.0 }
    val passedCount = count { it.passed == true }
    val failedCount = count { it.passed == false }

    val gpaItems = filter {
        it.calculateGp &&
            it.gp != null &&
            it.shouldIncludeInGradePointAverage()
    }
    val gpaCredits = gpaItems.sumOf { it.credits ?: 0.0 }
    val gpa = if (gpaCredits > 0.0) {
        gpaItems.sumOf { (it.gp ?: 0.0) * (it.credits ?: 0.0) } / gpaCredits
    } else {
        null
    }

    val weightedGradeItems = mapNotNull { item ->
        val score = item.convertedWeightedScore()
        val credits = item.credits
        if (item.shouldIncludeInWeightedAverage() && score != null && credits != null) {
            score to credits
        } else {
            null
        }
    }
    val weightedCredits = weightedGradeItems.sumOf { it.second }
    val weightedAverage = if (weightedCredits > 0.0) {
        weightedGradeItems.sumOf { (score, credits) -> score * credits } / weightedCredits
    } else {
        null
    }

    return GradeSummary(
        courseCount = size,
        totalCredits = totalCredits,
        gpa = gpa,
        weightedAverage = weightedAverage,
        passedCount = passedCount,
        failedCount = failedCount,
        gpaCredits = gpaCredits,
        weightedCredits = weightedCredits
    )
}

private fun GradeItem.shouldIncludeInGradePointAverage(): Boolean {
    val credits = credits ?: return false
    if (credits <= 0.0) return false
    if (practice == true) return false

    return when (courseType?.trim()) {
        "专业教育", "通识教育" -> true
        else -> false
    }
}

private fun GradeItem.shouldIncludeInWeightedAverage(): Boolean {
    val credits = credits ?: return false
    if (credits <= 0.0) return false
    if (practice == true) return false

    return when (courseType?.trim()) {
        "专业教育", "通识教育" -> true
        else -> false
    }
}

private fun GradeItem.convertedWeightedScore(): Double? {
    val rawGrade = grade.trim()
    rawGrade.toDoubleOrNull()?.let { return it }

    return when (rawGrade) {
        "优秀" -> 95.0
        "良好" -> 85.0
        "中等" -> 75.0
        "及格" -> 65.0
        "不及格" -> 0.0
        "合格" -> if (courseName.contains("形势与政策")) 85.0 else 65.0
        "不合格" -> 0.0
        else -> null
    }
}
