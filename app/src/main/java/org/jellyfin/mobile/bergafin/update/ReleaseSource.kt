package org.jellyfin.mobile.bergafin.update

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Uma release publicada no GitHub do Bergafin. */
data class Release(
    val tag: String,
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
)

internal object ReleaseSource {
    private const val REPO = "Agelinena/Bergafin-android"
    private const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"
    private const val VERSION_ASSET = "version.json"
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 60_000

    /**
     * A release "latest" do repositório (o GitHub nunca devolve pre-release nem rascunho aqui),
     * ou null se ela não tiver APK e version.json.
     */
    @Throws(IOException::class)
    fun latest(): Release? {
        val release = JSONObject(getText(LATEST_URL, "application/vnd.github+json"))
        val assets = release.getJSONArray("assets")
        var versionUrl: String? = null
        var apkUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.getString("name")
            val url = asset.getString("browser_download_url")
            when {
                name == VERSION_ASSET -> versionUrl = url
                name.endsWith(".apk") -> apkUrl = url
            }
        }
        if (versionUrl == null || apkUrl == null) return null
        val version = JSONObject(getText(versionUrl, null))
        return Release(
            tag = release.getString("tag_name"),
            versionName = version.getString("versionName"),
            versionCode = version.getLong("versionCode"),
            apkUrl = apkUrl,
        )
    }

    fun installedVersionCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info)
    }

    /** Abre o download (segue redirecionamentos do GitHub para o CDN). */
    @Throws(IOException::class)
    fun open(url: String, accept: String? = null): Pair<InputStream, Long> {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Bergafin-android")
            if (accept != null) setRequestProperty("Accept", accept)
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw IOException("HTTP $code em $url")
        }
        return connection.inputStream to connection.contentLengthLong
    }

    private fun getText(url: String, accept: String?): String =
        open(url, accept).first.bufferedReader().use { it.readText() }
}
