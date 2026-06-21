package com.cumtb.mineplus.ui.grade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.cumtb.mineplus.data.model.GradeItem
import com.cumtb.mineplus.data.model.GradeSemester
import com.cumtb.mineplus.data.model.GradeSummary
import com.cumtb.mineplus.ui.theme.Dimens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeScreen(
    viewModel: GradeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage = uiState.errorMessage
    val pullState = rememberPullToRefreshState()

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) pullState.endRefresh()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = {
                    Text(
                        text = "成绩",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(pullState.nestedScrollConnection)
        ) {
            when {
                uiState.isLoading && !uiState.hasLoaded -> {
                    GradeLoadingState()
                }

                errorMessage != null && !uiState.hasLoaded -> {
                    GradeErrorState(
                        message = errorMessage,
                        onRetry = viewModel::refresh
                    )
                }

                uiState.semesters.isEmpty() -> {
                    GradeEmptyState(
                        title = "暂无成绩数据",
                        description = "下拉或点击按钮重新获取成绩",
                        onRetry = viewModel::refresh
                    )
                }

                else -> {
                    GradeContent(
                        uiState = uiState,
                        onOverviewSelected = viewModel::onOverviewSelected,
                        onSemesterSelected = viewModel::onSemesterSelected,
                        onRetry = viewModel::refresh
                    )
                }
            }

            if (pullState.isRefreshing && !uiState.isLoading) {
                LaunchedEffect(pullState.isRefreshing) {
                    viewModel.refresh()
                }
            }

            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullState
            )
        }
    }
}

@Composable
private fun GradeContent(
    uiState: GradeUiState,
    onOverviewSelected: () -> Unit,
    onSemesterSelected: (Int) -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.screenPaddingCompact,
            top = Dimens.small2,
            end = Dimens.screenPaddingCompact,
            bottom = Dimens.medium
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.small2)
    ) {
        item(key = "semester_selector") {
            GradeSemesterSelector(
                semesters = uiState.semesters,
                selectedSemesterId = uiState.selectedSemesterId,
                onOverviewSelected = onOverviewSelected,
                onSemesterSelected = onSemesterSelected
            )
        }

        item(key = "grade_fetched_at") {
            GradeFetchedAtText(fetchedAt = uiState.gradeFetchedAt)
        }

        uiState.errorMessage?.let { message ->
            item(key = "error_banner") {
                GradeInlineError(message = message, onRetry = onRetry)
            }
        }

        item(key = "hero") {
            GradeHeroCard(summary = uiState.summary, isOverview = uiState.isOverview)
        }

        item(key = "summary_meta") {
            GradeSummaryMetaText(summary = uiState.summary)
        }

        if (uiState.isOverview) {
            item(key = "trend_chart") {
                GradeSmoothTrendChart(semesterSummaries = uiState.cumulativeSemesterSummaries)
            }

            if (uiState.semesterSummaries.isEmpty()) {
                item(key = "overview_empty") {
                    GradeEmptyPanel(text = "暂无可统计的成绩")
                }
            } else {
                item(key = "academic_year_overview") {
                    GradeAcademicYearOverview(semesterSummaries = uiState.semesterSummaries)
                }
            }
        } else if (uiState.grades.isEmpty()) {
            item(key = "empty") {
                GradeEmptyPanel()
            }
        } else {
            items(
                items = uiState.grades,
                key = { it.id }
            ) { grade ->
                GradeCourseCard(grade = grade)
            }
        }
    }
}

@Composable
private fun GradeSemesterSelector(
    semesters: List<GradeSemester>,
    selectedSemesterId: Int?,
    onOverviewSelected: () -> Unit,
    onSemesterSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectorSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val menuModifier = if (selectorSize.width > 0) {
        Modifier.width(with(density) { selectorSize.width.toDp() })
    } else {
        Modifier.fillMaxWidth()
    }
    val selectedSemester = semesters.firstOrNull { it.id == selectedSemesterId }
    val selectedTitle = selectedSemester?.name ?: "总览"

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { selectorSize = it }
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.small2, vertical = Dimens.tiny2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(x = 0, y = selectorSize.height),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Card(
                    modifier = menuModifier,
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.tiny2, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            GradeSemesterMenuItem(
                                text = "总览",
                                selected = selectedSemesterId == null,
                                onClick = {
                                    expanded = false
                                    onOverviewSelected()
                                }
                            )

                            semesters.forEach { semester ->
                                GradeSemesterMenuItem(
                                    text = semester.name,
                                    selected = semester.id == selectedSemesterId,
                                    onClick = {
                                        expanded = false
                                        onSemesterSelected(semester.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeSemesterMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val rowHighlightColor = if (pressed && !selected) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(rowHighlightColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = Dimens.small2, vertical = Dimens.tiny2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.tiny2)
    ) {
        Box(
            modifier = Modifier.width(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GradeFetchedAtText(fetchedAt: Long?) {
    Text(
        text = fetchedAt?.let { "成绩获取时间：${it.formatFetchTime()}" } ?: "成绩获取时间：尚未获取",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.tiny),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun GradeHeroCard(
    summary: GradeSummary,
    isOverview: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 0.5.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.cardPadding, vertical = Dimens.small2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradeHeroMetric(
                label = if (isOverview) "GPA" else "GPA（本学期）",
                value = summary.gpa?.formatNumber() ?: "--",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .height(54.dp)
                    .width(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f))
            )
            GradeHeroMetric(
                label = if (isOverview) "加权均分" else "加权均分（本学期）",
                value = summary.weightedAverage?.formatNumber() ?: "--",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GradeHeroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(Dimens.tiny)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GradeSummaryMetaText(summary: GradeSummary) {
    Text(
        text = "共 ${summary.courseCount} 门课程 (已通过 ${summary.passedCount} / 未通过 ${summary.failedCount}) · 加权学分 ${summary.weightedCredits.formatNumber()}",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.tiny),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun GradeSmoothTrendChart(semesterSummaries: List<GradeSemesterSummary>) {
    val points = semesterSummaries
        .sortedBy { it.semester.id }
        .mapNotNull { semesterSummary ->
            semesterSummary.summary.weightedAverage?.let { score ->
                GradeTrendPoint(
                    semesterName = semesterSummary.semester.name,
                    gpa = semesterSummary.summary.gpa,
                    weightedAverage = score
                )
            }
        }

    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val selectedPoint = selectedIndex?.let { points.getOrNull(it) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.tiny2)) {
        Text(
            text = "累计加权趋势",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.tiny2,
                        top = Dimens.small,
                        end = Dimens.tiny2,
                        bottom = Dimens.tiny2
                    ),
                verticalArrangement = Arrangement.spacedBy(Dimens.tiny)
            ) {
                if (points.isEmpty()) {
                    Text(
                        text = "暂无可绘制的加权均分",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val lineColor = MaterialTheme.colorScheme.primary
                    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
                    val selectedColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    val pointCutoutColor = MaterialTheme.colorScheme.surface
                    val selectedOffset = selectedIndex?.let { index ->
                        if (chartSize.width > 0 && chartSize.height > 0) {
                            calculateSmoothTrendCoordinates(
                                points = points,
                                width = chartSize.width.toFloat(),
                                height = chartSize.height.toFloat()
                            ).getOrNull(index)
                        } else {
                            null
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { chartSize = it }
                                .pointerInput(points) {
                                    detectTapGestures { tap ->
                                        val coordinates = calculateSmoothTrendCoordinates(
                                            points = points,
                                            width = size.width.toFloat(),
                                            height = size.height.toFloat()
                                        )
                                        val nearest = coordinates
                                            .mapIndexed { index, offset -> index to abs(offset.x - tap.x) }
                                            .minByOrNull { it.second }

                                        selectedIndex = nearest?.first
                                    }
                                }
                        ) {
                            val frame = calculateSmoothTrendFrame(
                                width = size.width,
                                height = size.height
                            )
                            val coordinates = calculateSmoothTrendCoordinates(
                                points = points,
                                width = size.width,
                                height = size.height
                            )
                            val gridEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                            repeat(3) { index ->
                                val y = frame.top + (frame.bottom - frame.top) * ((index + 1) / 4f)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(frame.left, y),
                                    end = Offset(frame.right, y),
                                    strokeWidth = 1.2f,
                                    pathEffect = gridEffect
                                )
                            }

                            selectedIndex?.let { index ->
                                coordinates.getOrNull(index)?.let { offset ->
                                    drawLine(
                                        color = lineColor.copy(alpha = 0.28f),
                                        start = Offset(offset.x, frame.top),
                                        end = Offset(offset.x, frame.bottom),
                                        strokeWidth = 1.4f,
                                        pathEffect = gridEffect
                                    )
                                }
                            }

                            if (coordinates.size >= 2) {
                                drawPath(
                                    path = buildSmoothTrendAreaPath(coordinates, frame.bottom),
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            lineColor.copy(alpha = 0.22f),
                                            lineColor.copy(alpha = 0.04f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                drawPath(
                                    path = buildSmoothTrendPath(coordinates),
                                    color = lineColor.copy(alpha = 0.82f),
                                    style = Stroke(width = 4.4f, cap = StrokeCap.Round)
                                )
                            }

                            coordinates.forEachIndexed { index, offset ->
                                val selected = index == selectedIndex
                                if (selected) {
                                    drawCircle(
                                        color = selectedColor.copy(alpha = 0.16f),
                                        radius = 18f,
                                        center = offset
                                    )
                                    drawCircle(
                                        color = pointCutoutColor,
                                        radius = 9f,
                                        center = offset
                                    )
                                }
                                drawCircle(
                                    color = if (selected) selectedColor else lineColor.copy(alpha = 0.82f),
                                    radius = if (selected) 6.4f else 4.2f,
                                    center = offset
                                )
                            }
                        }

                        if (selectedPoint != null && selectedOffset != null && chartSize.width > 0) {
                            val tooltipWidth = 126.dp
                            val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
                            val tooltipHeightPx = with(density) { 58.dp.toPx() }
                            val tooltipGapPx = with(density) { 10.dp.toPx() }
                            val x = (selectedOffset.x - tooltipWidthPx / 2f)
                                .roundToInt()
                                .coerceIn(0, (chartSize.width - tooltipWidthPx).roundToInt().coerceAtLeast(0))
                            val aboveY = selectedOffset.y - tooltipHeightPx - tooltipGapPx
                            val belowY = selectedOffset.y + tooltipGapPx
                            val y = if (aboveY >= 0f) {
                                aboveY
                            } else {
                                belowY.coerceAtMost(chartSize.height - tooltipHeightPx)
                            }.roundToInt().coerceAtLeast(0)

                            GradeTrendTooltip(
                                point = selectedPoint,
                                modifier = Modifier
                                    .width(tooltipWidth)
                                    .offset { IntOffset(x, y) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeTrendTooltip(
    point: GradeTrendPoint,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.tiny2, vertical = Dimens.tiny2),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = point.semesterName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "GPA ${point.gpa?.formatNumber() ?: "--"} · 加权分 ${point.weightedAverage.formatNumber()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GradeAcademicYearOverview(semesterSummaries: List<GradeSemesterSummary>) {
    val academicYears = semesterSummaries
        .groupBy { it.semester.name.academicYearLabel() }
        .map { (year, summaries) ->
            GradeAcademicYearSummary(
                year = year,
                summary = summaries.toCombinedGradeSummary(),
                semesterSummaries = summaries.sortedByDescending { it.semester.id }
            )
        }
        .sortedByDescending { it.year }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        Text(
            text = "学年概览",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        academicYears.forEach { academicYear ->
            GradeAcademicYearCard(academicYear = academicYear)
        }
    }
}

@Composable
private fun GradeAcademicYearCard(academicYear: GradeAcademicYearSummary) {
    var expanded by remember { mutableStateOf(false) }
    val summary = academicYear.summary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.tiny)
                ) {
                    Text(
                        text = academicYear.year,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${summary.courseCount} 门 · ${summary.totalCredits.formatNumber()} 学分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "加权 ${summary.weightedAverage?.formatNumber() ?: "--"} · 计入 ${summary.weightedCredits.formatNumber()} 学分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (expanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
                academicYear.semesterSummaries.forEachIndexed { index, semesterSummary ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.5.dp
                        )
                    }
                    GradeSemesterSummaryRow(semesterSummary = semesterSummary)
                }
            }
        }
    }
}

@Composable
private fun GradeSemesterSummaryRow(semesterSummary: GradeSemesterSummary) {
    val summary = semesterSummary.summary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.tiny),
        verticalArrangement = Arrangement.spacedBy(Dimens.tiny)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = semesterSummary.semester.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "加权 ${summary.weightedAverage?.formatNumber() ?: "--"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${summary.courseCount} 门 · ${summary.totalCredits.formatNumber()} 学分 · GPA ${summary.gpa?.formatNumber() ?: "--"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "计入加权 ${summary.weightedCredits.formatNumber()} 学分",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GradeCourseCard(grade: GradeItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.tiny2)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = grade.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (grade.courseCode.isNotBlank()) {
                        Text(
                            text = grade.courseCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = grade.grade,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (grade.passed == false) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    grade.gp?.let { gp ->
                        Text(
                            text = "绩点 ${gp.formatNumber()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            grade.makeupGrade?.let { makeupGrade ->
                Text(
                    text = "补考成绩 $makeupGrade",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            grade.minorCourseName
                ?.takeIf { it.isNotBlank() && it != grade.courseName }
                ?.let { minorCourseName ->
                    Text(
                        text = minorCourseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            Text(
                text = grade.detailText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GradeInlineError(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.small2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.small2)
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(onClick = onRetry) {
                Text(text = "重试")
            }
        }
    }
}

@Composable
private fun GradeEmptyPanel(text: String = "当前学期暂无成绩") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(Dimens.cardPadding),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GradeLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun GradeErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Dimens.small2))
        Button(onClick = onRetry) {
            Text(text = "重试")
        }
    }
}

@Composable
private fun GradeEmptyState(
    title: String,
    description: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Dimens.tiny2))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.small2))
        Button(onClick = onRetry) {
            Text(text = "重试")
        }
    }
}

private fun GradeItem.detailText(): String {
    val parts = buildList {
        credits?.let { add("${it.formatNumber()} 学分") }
        courseProperty?.takeIf { it.isNotBlank() }?.let { add(it) }
        courseType?.takeIf { it.isNotBlank() }?.let { add(it) }
        when (passed) {
            true -> add("通过")
            false -> add("未通过")
            null -> Unit
        }
        if (!calculateGp) add("不计绩点")
    }
    return parts.joinToString(" · ").ifBlank { semesterName }
}

private data class GradeTrendPoint(
    val semesterName: String,
    val gpa: Double?,
    val weightedAverage: Double
)

private data class GradeAcademicYearSummary(
    val year: String,
    val summary: GradeSummary,
    val semesterSummaries: List<GradeSemesterSummary>
)

private data class GradeTrendFrame(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

private fun List<GradeSemesterSummary>.toCombinedGradeSummary(): GradeSummary {
    val gpaCredits = sumOf { it.summary.gpaCredits }
    val gpa = takeIf { gpaCredits > 0.0 }
        ?.sumOf { (it.summary.gpa ?: 0.0) * it.summary.gpaCredits }
        ?.div(gpaCredits)

    val weightedCredits = sumOf { it.summary.weightedCredits }
    val weightedAverage = takeIf { weightedCredits > 0.0 }
        ?.sumOf { (it.summary.weightedAverage ?: 0.0) * it.summary.weightedCredits }
        ?.div(weightedCredits)

    return GradeSummary(
        courseCount = sumOf { it.summary.courseCount },
        totalCredits = sumOf { it.summary.totalCredits },
        gpa = gpa,
        weightedAverage = weightedAverage,
        passedCount = sumOf { it.summary.passedCount },
        failedCount = sumOf { it.summary.failedCount },
        gpaCredits = gpaCredits,
        weightedCredits = weightedCredits
    )
}

private fun calculateSmoothTrendCoordinates(
    points: List<GradeTrendPoint>,
    width: Float,
    height: Float
): List<Offset> {
    if (points.isEmpty()) return emptyList()

    val frame = calculateSmoothTrendFrame(width = width, height = height)
    val chartWidth = (frame.right - frame.left).coerceAtLeast(1f)
    val chartHeight = (frame.bottom - frame.top).coerceAtLeast(1f)
    val minScore = points.minOf { it.weightedAverage }
    val maxScore = points.maxOf { it.weightedAverage }
    val hasRange = maxScore - minScore > 0.01
    val range = if (hasRange) maxScore - minScore else 1.0

    return points.mapIndexed { index, point ->
        val x = if (points.size == 1) {
            frame.left + chartWidth / 2f
        } else {
            frame.left + chartWidth * (index.toFloat() / (points.lastIndex.toFloat()))
        }
        val normalized = if (hasRange) {
            ((point.weightedAverage - minScore) / range).toFloat()
        } else {
            0.5f
        }
        val y = frame.top + chartHeight * (1f - normalized)
        Offset(x, y)
    }
}

private fun calculateSmoothTrendFrame(
    width: Float,
    height: Float
): GradeTrendFrame {
    return GradeTrendFrame(
        left = 18f,
        top = 20f,
        right = (width - 18f).coerceAtLeast(36f),
        bottom = (height - 20f).coerceAtLeast(40f)
    )
}

private fun buildSmoothTrendPath(coordinates: List<Offset>): Path {
    val path = Path()
    if (coordinates.isEmpty()) return path

    path.moveTo(coordinates.first().x, coordinates.first().y)
    coordinates.zipWithNext().forEach { (start, end) ->
        val controlOffset = (end.x - start.x) * 0.42f
        path.cubicTo(
            start.x + controlOffset,
            start.y,
            end.x - controlOffset,
            end.y,
            end.x,
            end.y
        )
    }

    return path
}

private fun buildSmoothTrendAreaPath(
    coordinates: List<Offset>,
    bottom: Float
): Path {
    val path = Path()
    if (coordinates.isEmpty()) return path

    val first = coordinates.first()
    val last = coordinates.last()
    path.moveTo(first.x, bottom)
    path.lineTo(first.x, first.y)
    coordinates.zipWithNext().forEach { (start, end) ->
        val controlOffset = (end.x - start.x) * 0.42f
        path.cubicTo(
            start.x + controlOffset,
            start.y,
            end.x - controlOffset,
            end.y,
            end.x,
            end.y
        )
    }
    path.lineTo(last.x, bottom)
    path.close()

    return path
}

private fun String.academicYearLabel(): String {
    val parts = split("-")
    return if (parts.size >= 2) {
        "${parts[0]}-${parts[1]}"
    } else {
        this
    }
}

private fun Double.formatNumber(): String {
    return String.format(Locale.US, "%.2f", this)
        .trimEnd('0')
        .trimEnd('.')
}

private fun Long.formatFetchTime(): String {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)
        .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}
