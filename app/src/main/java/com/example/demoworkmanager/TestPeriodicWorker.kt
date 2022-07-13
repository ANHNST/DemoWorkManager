package com.example.demoworkmanager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.*

class TestPeriodicWorker (
    private val context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {
    override fun doWork(): Result {
        val dateFormat = SimpleDateFormat("HH:mm:ss")
        val time = dateFormat.format(Calendar.getInstance().time)
        Log.e("TAG", "Current time: $time")
        return Result.success()
    }

    override fun onStopped() {
        Log.e("TAG", "OnStopped called for this worker");
        super.onStopped()
    }


}