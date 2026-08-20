package dev.bugiel.kiroku.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dev.bugiel.kiroku.R
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class GitHubRelease(
    val version: AppVersion,
    val title: String,
    val notes: String,
    val assetName: String,
    val downloadUrl: String,
    val sha256: String?,
    val sizeBytes: Long,
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val release: GitHubRelease) : UpdateCheckResult
}

enum class InstallResult { INSTALLER_OPENED, PERMISSION_SETTINGS_OPENED }

interface AppUpdateRepository {
    val currentVersionName: String
    suspend fun checkForUpdate(): UpdateCheckResult
    suspend fun download(release: GitHubRelease, onProgress: (Float) -> Unit): File
    fun install(file: File): InstallResult
}

class GitHubUpdateRepository(
    context: Context,
) : AppUpdateRepository {
    private val applicationContext = context.applicationContext
    private val packageManager = applicationContext.packageManager
    private val currentPackageInfo: PackageInfo
        get() = packageInfo(applicationContext.packageName)

    override val currentVersionName: String
        get() = currentPackageInfo.versionName.orEmpty()

    override suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val json = requestText(LATEST_RELEASE_URL)
        val releaseObject = JSONObject(json)
        val version = AppVersion.parse(releaseObject.getString("tag_name"))
            ?: error(text(R.string.error_release_version_invalid))
        val current = AppVersion.parse(currentVersionName)
            ?: error(text(R.string.error_installed_version_invalid))
        if (version <= current) return@withContext UpdateCheckResult.UpToDate

        val assets = releaseObject.getJSONArray("assets")
        val asset = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { item -> item.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: error(text(R.string.error_release_missing_apk))
        val digest = asset.optString("digest").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")
        UpdateCheckResult.Available(
            GitHubRelease(
                version = version,
                title = releaseObject.optString("name").ifBlank { "Kiroku $version" },
                notes = releaseObject.optString("body"),
                assetName = asset.getString("name"),
                downloadUrl = asset.getString("browser_download_url"),
                sha256 = digest,
                sizeBytes = asset.optLong("size", -1L),
            ),
        )
    }

    override suspend fun download(release: GitHubRelease, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            require(release.sizeBytes <= 0 || release.sizeBytes <= MAX_APK_BYTES) {
                text(R.string.error_apk_too_large)
            }
            val updateDirectory = File(applicationContext.cacheDir, "updates").apply { mkdirs() }
            val target = File(updateDirectory, "kiroku-${release.version}.apk")
            val temporary = File(updateDirectory, "kiroku-${release.version}.apk.part")
            temporary.delete()

            val connection = openConnection(release.downloadUrl)
            try {
                val responseLength = connection.contentLengthLong
                require(responseLength <= 0 || responseLength <= MAX_APK_BYTES) {
                    text(R.string.error_apk_too_large)
                }
                val expectedLength = release.sizeBytes.takeIf { it > 0 } ?: responseLength
                val digest = MessageDigest.getInstance("SHA-256")
                connection.inputStream.buffered().use { input ->
                    temporary.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= MAX_APK_BYTES) { text(R.string.error_apk_too_large) }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            if (expectedLength > 0) onProgress((total.toFloat() / expectedLength).coerceIn(0f, 1f))
                        }
                    }
                }
                val actualDigest = digest.digest().toHexString()
                release.sha256?.let { expected ->
                    check(actualDigest.equals(expected, ignoreCase = true)) { text(R.string.error_apk_checksum) }
                }
                target.delete()
                check(temporary.renameTo(target)) { text(R.string.error_apk_save) }
                verifyDownloadedPackage(target)
                onProgress(1f)
                target
            } catch (error: Throwable) {
                temporary.delete()
                throw error
            } finally {
                connection.disconnect()
            }
        }

    override fun install(file: File): InstallResult {
        check(file.isFile) { text(R.string.error_apk_not_found) }
        if (!packageManager.canRequestPackageInstalls()) {
            applicationContext.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    "package:${applicationContext.packageName}".toUri(),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return InstallResult.PERMISSION_SETTINGS_OPENED
        }

        val uri = FileProvider.getUriForFile(
            applicationContext,
            "${applicationContext.packageName}.files",
            file,
        )
        applicationContext.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
        return InstallResult.INSTALLER_OPENED
    }

    private fun verifyDownloadedPackage(file: File) {
        val archive = archivePackageInfo(file) ?: error(text(R.string.error_apk_invalid))
        check(archive.packageName == applicationContext.packageName) { text(R.string.error_apk_wrong_package) }
        check(packageVersionCode(archive) > packageVersionCode(currentPackageInfo)) {
            text(R.string.error_apk_not_newer)
        }
        val installedSigners = signerDigests(currentPackageInfo)
        val archiveSigners = signerDigests(archive)
        check(installedSigners.isNotEmpty() && installedSigners.intersect(archiveSigners).isNotEmpty()) {
            text(R.string.error_apk_wrong_signature)
        }
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String): PackageInfo = when {
        Build.VERSION.SDK_INT >= 33 -> packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
        )
        Build.VERSION.SDK_INT >= 28 -> packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
        else -> packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    }

    @Suppress("DEPRECATION")
    private fun archivePackageInfo(file: File): PackageInfo? = when {
        Build.VERSION.SDK_INT >= 33 -> packageManager.getPackageArchiveInfo(
            file.absolutePath,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
        )
        Build.VERSION.SDK_INT >= 28 -> packageManager.getPackageArchiveInfo(
            file.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES,
        )
        else -> packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
    }

    @Suppress("DEPRECATION")
    private fun packageVersionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
        } else {
            info.signatures.orEmpty()
        }
        return signatures.mapTo(mutableSetOf()) { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).toHexString()
        }
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun requestText(url: String): String {
        val connection = openConnection(url)
        return try {
            connection.inputStream.bufferedReader().use { reader ->
                val response = reader.readText()
                require(response.length <= MAX_RELEASE_JSON_CHARS) { text(R.string.error_github_response_large) }
                response
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Kiroku-Android/$currentVersionName")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            val status = responseCode
            if (status !in 200..299) error(text(R.string.error_github_status, status))
        }

    private fun text(resourceId: Int, vararg arguments: Any): String =
        applicationContext.getString(resourceId, *arguments)

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/totallyeli/kiroku-android/releases/latest"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val MAX_APK_BYTES = 150L * 1024 * 1024
        private const val MAX_RELEASE_JSON_CHARS = 2 * 1024 * 1024
    }
}
