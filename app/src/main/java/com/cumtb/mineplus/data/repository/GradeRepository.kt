package com.cumtb.mineplus.data.repository

import android.util.Log
import com.cumtb.mineplus.data.api.SchoolApi
import com.cumtb.mineplus.data.database.GradeDao
import com.cumtb.mineplus.data.database.GradeEntity
import com.cumtb.mineplus.data.model.GradeData
import com.cumtb.mineplus.data.model.GradeItem
import com.cumtb.mineplus.data.model.GradeResponse
import com.cumtb.mineplus.data.model.GradeSemester
import com.cumtb.mineplus.data.model.GradeSemesterJson
import com.cumtb.mineplus.data.preference.AppPreferences
import com.cumtb.mineplus.data.model.StudentGradeJson
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeRepository @Inject constructor(
    private val api: SchoolApi,
    private val prefs: AppPreferences,
    private val gradeDao: GradeDao
) {
    private val gson = Gson()

    fun observeCachedGrades(): Flow<GradeData> {
        return gradeDao.observeAllGrades()
            .map { entities -> entities.toGradeData() }
    }

    fun observeGradeFetchedAt(): Flow<Long?> {
        return prefs.gradeFetchedAt
    }

    suspend fun refreshGrades() = withContext(Dispatchers.IO) {
        val data = loadGradesFromNetwork()
        gradeDao.replaceAll(data.gradesBySemester.values.flatten().map { it.toEntity() })
        prefs.saveGradeFetchedAt(System.currentTimeMillis())
    }

    suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
        gradeDao.deleteAllGrades()
        prefs.clearGradeSession()
    }

    private suspend fun loadGradesFromNetwork(): GradeData {
        val gradeDataId = resolveGradeDataId()
        Log.d("MinePlus", "成绩查询使用 gradeDataId=$gradeDataId")

        val rawBody = try {
            fetchGradeDataBodyWithRetry(gradeDataId)
        } catch (e: HttpException) {
            Log.w("MinePlus", "成绩ID可能已失效，清理后重新解析当前账号ID", e)
            prefs.clearGradeSession()
            val refreshedGradeDataId = resolveGradeDataId(forceRefresh = true)
            Log.d("MinePlus", "成绩查询重试使用 gradeDataId=$refreshedGradeDataId")
            fetchGradeDataBodyWithRetry(refreshedGradeDataId)
        }
        val parsedResponse = parseGradeResponse(rawBody)
        val response = parsedResponse.result ?: parsedResponse.studentGradeList ?: parsedResponse
        val responseSemesters = buildSemesters(response)
        val semesterNameById = responseSemesters.associate { it.id to it.name }
        val gradesBySemester = buildGradesBySemester(response, semesterNameById)

        val extraSemesters = gradesBySemester.keys
            .filterNot { id -> responseSemesters.any { it.id == id } }
            .map { id -> GradeSemester(id = id, name = "学期 $id") }
            .sortedByDescending { it.id }

        val allSemesters = responseSemesters + extraSemesters
        val defaultSemesterId = allSemesters
            .firstOrNull { semester -> gradesBySemester[semester.id].orEmpty().isNotEmpty() }
            ?.id
            ?: allSemesters.firstOrNull()?.id

        Log.d(
            "MinePlus",
            "成绩解析结果: semesters=${allSemesters.size}, gradeGroups=${gradesBySemester.size}, grades=${gradesBySemester.values.sumOf { it.size }}"
        )

        return GradeData(
            semesters = allSemesters,
            gradesBySemester = gradesBySemester,
            defaultSemesterId = defaultSemesterId
        )
    }

    private suspend fun fetchGradeDataBodyWithRetry(gradeDataId: Long): String {
        return try {
            api.getGradeDataBody(gradeDataId = gradeDataId).string()
        } catch (e: SocketTimeoutException) {
            Log.w("MinePlus", "成绩首次请求超时，准备自动重试一次", e)
            delay(300)
            api.getGradeDataBody(gradeDataId = gradeDataId).string()
        }
    }

    private fun buildSemesters(response: GradeResponse): List<GradeSemester> {
        val fromList = response.semesters.orEmpty()
            .mapNotNull { it.toGradeSemester() }

        val fromMap = response.id2semesters.orEmpty()
            .values
            .mapNotNull { it.toGradeSemester() }

        val fromGrades = response.flatGradeList()
            .mapNotNull { it.semester?.toGradeSemester() }

        return (fromList + fromMap + fromGrades)
            .distinctBy { it.id }
            .sortedByDescending { it.id }
    }

    private fun buildGradesBySemester(
        response: GradeResponse,
        semesterNameById: Map<Int, String>
    ): Map<Int, List<GradeItem>> {
        val grouped = response.semesterId2studentGrades.orEmpty()
            .mapNotNull { (semesterIdText, grades) ->
                val semesterId = semesterIdText.toIntOrNull() ?: return@mapNotNull null
                semesterId to grades.orEmpty().map { grade ->
                    grade.toGradeItem(
                        fallbackSemesterId = semesterId,
                        semesterNameById = semesterNameById
                    )
                }
            }
            .toMap()

        if (grouped.isNotEmpty()) return grouped

        return response.flatGradeList()
            .mapNotNull { grade ->
                val semesterId = grade.semester?.id ?: return@mapNotNull null
                grade.toGradeItem(
                    fallbackSemesterId = semesterId,
                    semesterNameById = semesterNameById
                )
            }
            .groupBy { it.semesterId }
    }

    private fun GradeResponse.flatGradeList(): List<StudentGradeJson> {
        return studentGrades ?: grades ?: emptyList()
    }

    private fun GradeSemesterJson.toGradeSemester(): GradeSemester? {
        val id = id ?: return null
        return GradeSemester(
            id = id,
            name = nameZh ?: nameEn ?: "学期 $id"
        )
    }

    private fun StudentGradeJson.toGradeItem(
        fallbackSemesterId: Int,
        semesterNameById: Map<Int, String>
    ): GradeItem {
        val resolvedSemesterId = semester?.id ?: fallbackSemesterId
        val semesterName = semester?.nameZh
            ?: semester?.nameEn
            ?: semesterNameById[resolvedSemesterId]
            ?: "学期 $resolvedSemesterId"

        val courseInfo = this.course
        return GradeItem(
            id = id ?: "${resolvedSemesterId}-${lessonCode}-${courseInfo?.code}".hashCode().toLong(),
            semesterId = resolvedSemesterId,
            semesterName = semesterName,
            courseName = courseInfo?.nameZh ?: minorCourse?.nameZh ?: "未知课程",
            courseCode = courseInfo?.code.orEmpty(),
            lessonCode = lessonCode.orEmpty(),
            minorCourseName = minorCourse?.nameZh,
            credits = courseInfo?.credits,
            grade = if (published == false) {
                "未公布"
            } else {
                gaGrade?.takeIf { it.isNotBlank() } ?: "未公布"
            },
            makeupGrade = makeupGaGrade?.takeIf { it.isNotBlank() },
            gp = gp,
            passed = passed,
            calculateGp = courseInfo?.calculateGp == true,
            practice = courseInfo?.practice,
            courseType = courseType?.nameZh ?: courseType?.name,
            courseProperty = courseProperty?.nameZh ?: courseProperty?.name,
            compulsory = compulsory
        )
    }

    private fun List<GradeEntity>.toGradeData(): GradeData {
        val items = map { it.toGradeItem() }
        val gradesBySemester = items.groupBy { it.semesterId }
        val semesters = items
            .map { GradeSemester(id = it.semesterId, name = it.semesterName) }
            .distinctBy { it.id }
            .sortedByDescending { it.id }

        return GradeData(
            semesters = semesters,
            gradesBySemester = gradesBySemester,
            defaultSemesterId = null
        )
    }

    private fun GradeItem.toEntity(): GradeEntity {
        return GradeEntity(
            id = id,
            semesterId = semesterId,
            semesterName = semesterName,
            courseName = courseName,
            courseCode = courseCode,
            lessonCode = lessonCode,
            minorCourseName = minorCourseName,
            credits = credits,
            grade = grade,
            makeupGrade = makeupGrade,
            gp = gp,
            passed = passed,
            calculateGp = calculateGp,
            practice = practice,
            courseType = courseType,
            courseProperty = courseProperty,
            compulsory = compulsory
        )
    }

    private fun GradeEntity.toGradeItem(): GradeItem {
        return GradeItem(
            id = id,
            semesterId = semesterId,
            semesterName = semesterName,
            courseName = courseName,
            courseCode = courseCode,
            lessonCode = lessonCode,
            minorCourseName = minorCourseName,
            credits = credits,
            grade = grade,
            makeupGrade = makeupGrade,
            gp = gp,
            passed = passed,
            calculateGp = calculateGp,
            practice = practice,
            courseType = courseType,
            courseProperty = courseProperty,
            compulsory = compulsory
        )
    }

    private suspend fun resolveGradeDataId(forceRefresh: Boolean = false): Long {
        val currentStdPersonId = prefs.stdPersonId.first()?.takeIf { it > 0L }
        if (!forceRefresh) {
            prefs.gradeDataId.first()?.takeIf { it > 0L }?.let { cachedId ->
                if (currentStdPersonId == null || cachedId == currentStdPersonId) {
                    return cachedId
                }

                Log.w(
                    "MinePlus",
                    "成绩ID缓存与当前账号不一致，忽略旧ID: cached=$cachedId, stdPersonId=$currentStdPersonId"
                )
                prefs.clearGradeSession()
            }

            currentStdPersonId?.let { stdPersonId ->
                prefs.saveGradeDataId(stdPersonId)
                return stdPersonId
            }
        }

        val gradePageHtml = api.getGradePageHtml().string()
        val gradeDataId = parseGradeDataId(gradePageHtml)
        if (gradeDataId != null) {
            prefs.saveGradeDataId(gradeDataId)
            return gradeDataId
        }

        throw IllegalStateException("无法从成绩页获取成绩数据ID")
    }

    private fun parseGradeDataId(html: String): Long? {
        val patterns = listOf(
            """/get-grade-data/(\d+)""",
            """get-grade-data/(\d+)""",
            """/get-grade-list/(\d+)""",
            """get-grade-list/(\d+)""",
            """studentId\s*[:=]\s*['"]?(\d+)""",
            """stdPersonId\s*[:=]\s*['"]?(\d+)""",
            """data\[['"]stdPersonId['"]]\s*=\s*(\d+);""",
            """data\[['"]studentId['"]]\s*=\s*(\d+);"""
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            Regex(pattern).find(html)?.groupValues?.getOrNull(1)?.toLongOrNull()
        }
    }

    private fun parseGradeResponse(rawBody: String): GradeResponse {
        val trimmed = rawBody.trimStart()
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) {
            throw IllegalStateException("登录状态已失效，请重新登录")
        }
        if (!trimmed.startsWith("{")) {
            throw IllegalStateException("成绩接口返回格式异常")
        }

        return try {
            gson.fromJson(rawBody, GradeResponse::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e("MinePlus", "成绩 JSON 解析失败: ${rawBody.take(500)}", e)
            throw IllegalStateException("成绩接口数据解析失败", e)
        }
    }
}
