package com.example.widgetcatchingup.data.updater

import com.example.widgetcatchingup.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val checkedSuccessfully: Boolean = true
)

object GitHubUpdateChecker {

    private const val REPO_OWNER = "nicosuar22"
    private const val REPO_NAME = "widget-catching-up"

    suspend fun checkLatestRelease(): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = BuildConfig.VERSION_NAME
        try {
            val url = URL("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "WidgetCatchingUpApp")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                val tagName = json.optString("tag_name", "")
                val htmlUrl = json.optString("html_url", "")
                val body = json.optString("body", "Nueva versión disponible.")

                var apkDownloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                val isNewer = isVersionNewer(tagName, currentVersion)
                return@withContext UpdateInfo(
                    hasUpdate = isNewer,
                    latestVersion = tagName,
                    downloadUrl = apkDownloadUrl,
                    releaseNotes = body,
                    checkedSuccessfully = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext UpdateInfo(
            hasUpdate = false,
            latestVersion = currentVersion,
            downloadUrl = "",
            releaseNotes = "",
            checkedSuccessfully = false
        )
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank() || current.isBlank()) return false
        val cleanLatest = latest.removePrefix("v").removePrefix("V").trim()
        val cleanCurrent = current.removePrefix("v").removePrefix("V").trim()

        if (cleanLatest.equals(cleanCurrent, ignoreCase = true)) return false

        try {
            val latestParts = cleanLatest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(latestParts.size, currentParts.size)

            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
        } catch (e: Exception) {
            return false
        }

        return false
    }
}
