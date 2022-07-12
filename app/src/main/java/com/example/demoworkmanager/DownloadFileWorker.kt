package com.example.demoworkmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.net.URL


class DownloadFileWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {



    override suspend fun doWork(): Result {
        // get data from inputData in workerParameters
        val url = inputData.getString(FileParams.KEY_FILE_URL) ?: ""
        val fileMimeType = inputData.getString(FileParams.KEY_FILE_TYPE) ?: FileMimeType.TXT
        val fileNameWithOutExtension = inputData.getString(FileParams.KEY_FILE_NAME) ?: "downloadWorkManagerFile"
        val fileRelativePath = inputData.getString(FileParams.KEY_FILE_PATH) ?: Environment.DIRECTORY_DOWNLOADS

        val fileName = fileNameWithOutExtension + getFileExtension(fileMimeType)
        makeStatusNotification(context)
        if (url == "") return Result.failure()
        val fileResultUri = downloadFile(url, fileName, fileMimeType, fileRelativePath)
        return if (fileResultUri != null) {
            Result.success(
                workDataOf(FileParams.KEY_FILE_URI to fileResultUri.toString())
            )
        } else {
            if (runAttemptCount > 3) {
                Result.failure()
            } else {
                Result.retry()
            }

        }

    }

    private fun downloadFile(fileUrl: String, fileName: String, mimeType: String, fileRelativePath: String): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, fileRelativePath)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            return if (uri!=null){
                URL(fileUrl).openStream().use { input->
                    resolver.openOutputStream(uri).use { output->
                        input.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                    }
                }
                uri
            }else{
                null
            }
        }else{
            val target = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            URL(fileUrl).openStream().use { input->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            return target.toUri()
        }
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun makeStatusNotification(context: Context) {
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = NotificationConstants.CHANNEL_NAME
            val description = NotificationConstants.CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NotificationConstants.CHANNEL_ID, name, importance)
            channel.description = description

            // Add the channel
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

            notificationManager?.createNotificationChannel(channel)
        }

        // Create the notification
        val builder = NotificationCompat.Builder(context, NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("Downloading File")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(LongArray(0))

        // Show the notification
        NotificationManagerCompat.from(context).notify(NotificationConstants.NOTIFICATION_ID, builder.build())
    }

    private fun getFileExtension(mimeType: String): String {
        when (mimeType) {
            FileMimeType.PDF -> return ".pdf"
            FileMimeType.PNG -> return ".png"
            FileMimeType.JPG -> return ".jpg"
            FileMimeType.MP3 -> return ".mp3"
            FileMimeType.MP4 -> return ".mp4"
            FileMimeType.TXT -> return ".txt"
            else -> return ".txt"
        }
    }

}

object FileParams {
    const val KEY_FILE_URL = "key_file_url"
    const val KEY_FILE_URI = "key_file_uri"
    const val KEY_FILE_TYPE = "key_file_type"
    const val KEY_FILE_NAME = "key_file_name"
    const val KEY_FILE_PATH = "key_file_path"
}

object FileMimeType {
    const val PDF = "application/pdf"
    const val PNG = "image/png"
    const val JPG = "image/jpeg"
    const val MP3 = "audio/mpeg"
    const val MP4 = "video/mp4"
    const val TXT = "text/plain"
}

object NotificationConstants{
    const val CHANNEL_NAME = "download_file_worker_demo_channel"
    const val CHANNEL_DESCRIPTION = "download_file_worker_demo_description"
    const val CHANNEL_ID = "download_file_worker_demo_channel_123456"
    const val NOTIFICATION_ID = 1
}