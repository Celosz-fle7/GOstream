package com.gostream.app.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.gostream.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object AppUpdateManager {

    private const val UPDATE_MANIFEST_URL = "https://raw.githubusercontent.com/Celosz-fle7/GOstream/main/update.json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun checkForUpdates(activity: Activity, isManualCheck: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(UPDATE_MANIFEST_URL)
                    .addHeader("Cache-Control", "no-cache")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    if (isManualCheck) {
                        showToast(activity, "Gagal memeriksa pembaruan dari server.")
                    }
                    return@launch
                }

                val body = response.body?.string() ?: return@launch
                val json = JSONObject(body)

                val remoteVersionCode = json.optInt("version_code", 1)
                val remoteVersionName = json.optString("version_name", "1.0.0")
                val apkUrl = json.optString("apk_url", "")
                val changelog = json.optString("changelog", "Perbaikan stabilitas dan fitur baru.")

                val currentVersionCode = getCurrentVersionCode(activity)
                val currentVersionName = getCurrentVersionName(activity)

                withContext(Dispatchers.Main) {
                    if (remoteVersionCode > currentVersionCode) {
                        showUpdateDialog(activity, remoteVersionName, changelog, apkUrl)
                    } else if (isManualCheck) {
                        Toast.makeText(
                            activity,
                            "GOstream sudah menggunakan versi terbaru (v$currentVersionName)!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (isManualCheck) {
                    showToast(activity, "Error memeriksa pembaruan: ${e.message}")
                }
            }
        }
    }

    private fun showUpdateDialog(activity: Activity, versionName: String, changelog: String, apkUrl: String) {
        if (activity.isFinishing || activity.isDestroyed) return

        AlertDialog.Builder(activity)
            .setTitle("🚀 Pembaruan Tersedia (v$versionName)")
            .setMessage("Versi baru aplikasi GOstream telah tersedia!\n\nCatatan Pembaruan:\n$changelog\n\nApakah Anda ingin memperbarui sekarang?")
            .setCancelable(false)
            .setPositiveButton("Update Sekarang") { _, _ ->
                startDownloadAndInstall(activity, apkUrl, versionName)
            }
            .setNegativeButton("Nanti Saja", null)
            .show()
    }

    private fun startDownloadAndInstall(activity: Activity, apkUrl: String, versionName: String) {
        if (apkUrl.isBlank()) {
            Toast.makeText(activity, "Tautan unduhan tidak valid.", Toast.LENGTH_SHORT).show()
            return
        }

        // Custom download progress dialog
        val dialogView = LayoutInflater.from(activity).inflate(android.R.layout.simple_list_item_2, null)
        val tvStatus = dialogView.findViewById<TextView>(android.R.id.text1).apply {
            text = "Mengunduh GOstream v$versionName..."
            textSize = 15f
        }
        val tvProgress = dialogView.findViewById<TextView>(android.R.id.text2).apply {
            text = "Persiapan mengunduh (0%)..."
            textSize = 12f
        }

        val progressDialog = AlertDialog.Builder(activity)
            .setTitle("Mengunduh Pembaruan")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(apkUrl).build()
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(activity, "Gagal mengunduh file APK.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val responseBody = response.body ?: return@launch
                val contentLength = responseBody.contentLength()

                val destDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: activity.cacheDir
                val apkFile = File(destDir, "GOstream-v$versionName.apk")
                if (apkFile.exists()) apkFile.delete()

                val inputStream = responseBody.byteStream()
                val outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val percent = ((totalBytesRead * 100) / contentLength).toInt()
                        val mbDownloaded = totalBytesRead / (1024.0 * 1024.0)
                        val mbTotal = contentLength / (1024.0 * 1024.0)

                        withContext(Dispatchers.Main) {
                            tvProgress.text = "${"%.1f".format(mbDownloaded)} MB / ${"%.1f".format(mbTotal)} MB ($percent%)"
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    promptInstallApk(activity, apkFile)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(activity, "Gagal mengunduh: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun promptInstallApk(activity: Activity, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(activity, "File installer tidak ditemukan.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            activity.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "Gagal membuka installer APK: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getCurrentVersionCode(activity: Activity): Int {
        return try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    private fun getCurrentVersionName(activity: Activity): String {
        return try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private suspend fun showToast(activity: Activity, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }
}
