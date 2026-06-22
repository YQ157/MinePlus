package com.cumtb.mineplus.api

object SchoolApiConfig {
    const val BASE_URL = "https://jwxt.cumtb.edu.cn/"

    private val TRUSTED_SCHOOL_HOSTS = setOf(
        "jwxt.cumtb.edu.cn",
        "auth.cumtb.edu.cn"
    )

    val COOKIE_PROBE_URLS = listOf(
        BASE_URL,
        "${BASE_URL}student/for-std/course-table",
        "${BASE_URL}student/for-std/grade/sheet",
        "${BASE_URL}student/ws/schedule-table/datum"
    )

    fun isTrustedSchoolHost(host: String?): Boolean {
        val normalizedHost = host
            ?.lowercase()
            ?.trimEnd('.')
            ?: return false

        return normalizedHost in TRUSTED_SCHOOL_HOSTS ||
            normalizedHost.endsWith(".cumtb.edu.cn")
    }
}
