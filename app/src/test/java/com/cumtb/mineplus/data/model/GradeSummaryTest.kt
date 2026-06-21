package com.cumtb.mineplus.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GradeSummaryTest {

    @Test
    fun `weighted average converts grade systems and filters excluded courses`() {
        val summary = listOf(
            gradeItem(grade = "80", credits = 3.0, courseType = "专业教育"),
            gradeItem(grade = "合格", credits = 1.0, courseType = "通识教育", courseName = "形势与政策1"),
            gradeItem(grade = "良好", credits = 2.0, courseType = "专业教育"),
            gradeItem(grade = "及格", credits = 1.0, courseType = "通识教育"),
            gradeItem(grade = "100", credits = 2.0, courseType = "实践教育", practice = true),
            gradeItem(grade = "100", credits = 2.0, courseType = "通识教育选修课"),
            gradeItem(grade = "100", credits = 2.0, courseType = "创新创业教育"),
            gradeItem(grade = "100", credits = 0.0, courseType = "专业教育"),
            gradeItem(grade = "100", credits = 2.0, courseType = "专业教育", practice = true)
        ).calculateGradeSummary()

        assertEquals(7.0, summary.weightedCredits, 0.0001)
        assertEquals(80.0, summary.weightedAverage ?: 0.0, 0.0001)
    }

    @Test
    fun `qualified uses two level score only for policy courses`() {
        val summary = listOf(
            gradeItem(grade = "合格", credits = 1.0, courseType = "通识教育", courseName = "形势与政策1"),
            gradeItem(grade = "合格", credits = 1.0, courseType = "通识教育", courseName = "普通测试课")
        ).calculateGradeSummary()

        assertEquals(75.0, summary.weightedAverage ?: 0.0, 0.0001)
    }

    @Test
    fun `gpa filters excluded course types and zero credits`() {
        val summary = listOf(
            gradeItem(grade = "90", credits = 3.0, courseType = "专业教育", gp = 4.0),
            gradeItem(grade = "85", credits = 1.0, courseType = "通识教育", gp = 3.7),
            gradeItem(grade = "95", credits = 2.0, courseType = "实践教育", practice = true, gp = 4.0),
            gradeItem(grade = "95", credits = 2.0, courseType = "通识教育选修课", gp = 4.0),
            gradeItem(grade = "95", credits = 2.0, courseType = "创新创业教育", gp = 4.0),
            gradeItem(grade = "95", credits = 0.0, courseType = "专业教育", gp = 4.0),
            gradeItem(grade = "95", credits = 2.0, courseType = "专业教育", calculateGp = false, gp = 4.0)
        ).calculateGradeSummary()

        assertEquals(4.0, summary.gpaCredits, 0.0001)
        assertEquals(3.925, summary.gpa ?: 0.0, 0.0001)
    }

    private fun gradeItem(
        grade: String,
        credits: Double,
        courseType: String,
        courseName: String = "测试课程",
        practice: Boolean? = false,
        calculateGp: Boolean = true,
        gp: Double? = null
    ): GradeItem {
        return GradeItem(
            id = grade.hashCode().toLong() + credits.hashCode() + courseType.hashCode(),
            semesterId = 281,
            semesterName = "2025-2026-1",
            courseName = courseName,
            courseCode = "TEST",
            lessonCode = "TEST.01",
            minorCourseName = null,
            credits = credits,
            grade = grade,
            makeupGrade = null,
            gp = gp,
            passed = true,
            calculateGp = calculateGp,
            practice = practice,
            courseType = courseType,
            courseProperty = "必修",
            compulsory = true
        )
    }
}
