package com.cumtb.mineplus.ui.grade

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cumtb.mineplus.data.model.GradeData
import com.cumtb.mineplus.data.model.GradeItem
import com.cumtb.mineplus.data.model.GradeSemester
import com.cumtb.mineplus.data.model.GradeSummary
import com.cumtb.mineplus.data.model.calculateGradeSummary
import com.cumtb.mineplus.data.repository.GradeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class GradeViewModel @Inject constructor(
    private val repository: GradeRepository
) : ViewModel() {

    private var latestData: GradeData? = null

    private val _uiState = MutableStateFlow(GradeUiState())
    val uiState: StateFlow<GradeUiState> = _uiState.asStateFlow()

    init {
        observeCachedGrades()
        observeGradeFetchedAt()
        refresh(showErrorOnFailure = false)
    }

    private fun observeCachedGrades() {
        viewModelScope.launch {
            repository.observeCachedGrades().collect { data ->
                latestData = data
                if (data.semesters.isNotEmpty() || _uiState.value.hasLoaded) {
                    applyData(
                        data = data,
                        preferredSemesterId = _uiState.value.selectedSemesterId,
                        isLoading = _uiState.value.isLoading,
                        errorMessage = _uiState.value.errorMessage,
                        gradeFetchedAt = _uiState.value.gradeFetchedAt
                    )
                }
            }
        }
    }

    private fun observeGradeFetchedAt() {
        viewModelScope.launch {
            repository.observeGradeFetchedAt().collect { fetchedAt ->
                _uiState.update { it.copy(gradeFetchedAt = fetchedAt) }
            }
        }
    }

    fun refresh() {
        refresh(showErrorOnFailure = true)
    }

    private fun refresh(showErrorOnFailure: Boolean) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }
            try {
                repository.refreshGrades()
                _uiState.update { it.copy(isLoading = false, hasLoaded = true, errorMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MinePlus", "成绩加载失败", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasLoaded = latestData != null || it.hasLoaded,
                        errorMessage = if (showErrorOnFailure) e.toUserMessage() else null
                    )
                }
            }
        }
    }

    fun onSemesterSelected(semesterId: Int) {
        val data = latestData ?: return
        applyData(
            data = data,
            preferredSemesterId = semesterId,
            isLoading = false,
            errorMessage = null,
            gradeFetchedAt = _uiState.value.gradeFetchedAt
        )
    }

    fun onOverviewSelected() {
        val data = latestData ?: return
        applyData(
            data = data,
            preferredSemesterId = null,
            isLoading = false,
            errorMessage = null,
            gradeFetchedAt = _uiState.value.gradeFetchedAt
        )
    }

    private fun applyData(
        data: GradeData,
        preferredSemesterId: Int?,
        isLoading: Boolean,
        errorMessage: String?,
        gradeFetchedAt: Long?
    ) {
        val selectedSemesterId = preferredSemesterId
            ?.takeIf { id -> data.semesters.any { it.id == id } }

        val allGrades = data.gradesBySemester.values.flatten()
        val grades = selectedSemesterId
            ?.let { data.gradesBySemester[it] }
            ?: allGrades

        val semesterSummaries = data.semesters.mapNotNull { semester ->
            val semesterGrades = data.gradesBySemester[semester.id].orEmpty()
            if (semesterGrades.isEmpty()) {
                null
            } else {
                GradeSemesterSummary(
                    semester = semester,
                    summary = semesterGrades.calculateGradeSummary()
                )
            }
        }
        val cumulativeSemesterSummaries = buildCumulativeSemesterSummaries(data)

        _uiState.value = GradeUiState(
            isLoading = isLoading,
            hasLoaded = true,
            semesters = data.semesters,
            selectedSemesterId = selectedSemesterId,
            grades = grades,
            summary = grades.calculateGradeSummary(),
            semesterSummaries = semesterSummaries,
            cumulativeSemesterSummaries = cumulativeSemesterSummaries,
            gradeFetchedAt = gradeFetchedAt,
            errorMessage = errorMessage
        )
    }

    private fun buildCumulativeSemesterSummaries(data: GradeData): List<GradeSemesterSummary> {
        val cumulativeGrades = mutableListOf<GradeItem>()

        return data.semesters
            .sortedBy { it.id }
            .mapNotNull { semester ->
                val semesterGrades = data.gradesBySemester[semester.id].orEmpty()
                if (semesterGrades.isEmpty()) {
                    return@mapNotNull null
                }

                cumulativeGrades += semesterGrades
                GradeSemesterSummary(
                    semester = semester,
                    summary = cumulativeGrades.calculateGradeSummary()
                )
            }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is UnknownHostException -> "网络不可用，请检查网络后重试"
            is SocketTimeoutException -> "成绩加载超时，请稍后重试"
            is HttpException -> {
                if (code() == 500) {
                    "成绩接口返回 500，成绩数据ID可能不匹配，请重新登录后再试"
                } else {
                    "成绩接口请求失败：HTTP ${code()}"
                }
            }
            is IOException -> "网络异常，请稍后重试"
            is IllegalStateException -> message ?: "成绩加载失败，请重新登录后重试"
            else -> "成绩加载失败，请重新登录后重试"
        }
    }
}

data class GradeUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val semesters: List<GradeSemester> = emptyList(),
    val selectedSemesterId: Int? = null,
    val grades: List<GradeItem> = emptyList(),
    val summary: GradeSummary = GradeSummary(
        courseCount = 0,
        totalCredits = 0.0,
        gpa = null,
        weightedAverage = null,
        passedCount = 0,
        failedCount = 0,
        gpaCredits = 0.0,
        weightedCredits = 0.0
    ),
    val semesterSummaries: List<GradeSemesterSummary> = emptyList(),
    val cumulativeSemesterSummaries: List<GradeSemesterSummary> = emptyList(),
    val gradeFetchedAt: Long? = null,
    val errorMessage: String? = null
) {
    val isOverview: Boolean
        get() = selectedSemesterId == null
}

data class GradeSemesterSummary(
    val semester: GradeSemester,
    val summary: GradeSummary
)
