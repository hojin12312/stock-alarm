package com.example.playground.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class UpdateInstaller(private val appContext: Context) {

    fun start(info: UpdateInfo) {
        if (!ensureInstallPermission()) return
        val downloadManager =
            appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val targetFile = File(apkDir(), "stock-alarm-${info.versionCode}.apk")
        if (targetFile.exists()) targetFile.delete()

        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("주식 알리미 업데이트")
            .setDescription("v${info.versionName} 다운로드 중")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(targetFile))
            .setMimeType(APK_MIME)

        val downloadId = downloadManager.enqueue(request)
        registerCompletionReceiver(downloadId, targetFile)
        Log.i(TAG, "다운로드 시작 id=$downloadId path=${targetFile.absolutePath}")
    }

    private fun ensureInstallPermission(): Boolean {
        val pm = appContext.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !pm.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${appContext.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            return false
        }
        return true
    }

    private fun apkDir(): File {
        val dir = File(appContext.getExternalFilesDir(null), "apk")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun registerCompletionReceiver(downloadId: Long, file: File) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val finishedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (finishedId != downloadId) return
                appContext.unregisterReceiver(this)
                launchInstaller(file)
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun launchInstaller(file: File) {
        if (!file.exists()) {
            Log.w(TAG, "다운로드된 파일을 찾지 못함: ${file.absolutePath}")
            return
        }
        val authority = "${appContext.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(appContext, authority, file)
        val install = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(install)
    }

    companion object {
        private const val TAG = "UpdateInstaller"
        private const val APK_MIME = "application/vnd.android.package-archive"
    }
}
