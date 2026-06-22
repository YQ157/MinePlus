package com.cumtb.mineplus.api

object SchoolApiConfig {
    const val BASE_URL = "https://jwxt.cumtb.edu.cn/"

    val COOKIE_PROBE_URLS = listOf(
        BASE_URL,
        "${BASE_URL}student/for-std/course-table",
        "${BASE_URL}student/for-std/grade/sheet",
        "${BASE_URL}student/ws/schedule-table/datum"
    )
}
