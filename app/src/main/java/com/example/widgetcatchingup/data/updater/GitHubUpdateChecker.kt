package com.example.widgetcatchingup.data.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object GitHubUpdateChecker {

    private const val REPO_OWNER = "nicosuar22"
    private const val REPO_NAME = "widget-catching-up"
    private const val CURRENT_VERSION = "v1.0.0"

    suspend fun checkLatestRelease(): UpdateInfo = withContext(Dispatchers.IO) {
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

                val isNewer = isVersionNewer(tagName, CURRENT_VERSION)
                return@withContext UpdateInfo(
                    hasUpdate = isNewer,
                    latestVersion = tagName,
                    downloadUrl = apkDownloadUrl,
                    releaseNotes = body
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext UpdateInfo(
            hasUpdate = false,
            latestVersion = CURRENT_VERSION,
            downloadUrl = "",
            releaseNotes = ""
        )
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank() || latest == current) return false
        val cleanLatest = latest.removePrefix("v").removePrefix("V")
        val cleanCurrent = current.removePrefix("v").removePrefix("V")
        return cleanLatest != cleanCurrent
    }
}
