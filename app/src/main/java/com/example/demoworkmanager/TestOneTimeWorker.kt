package com.example.demoworkmanager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class TestOneTimeWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val outputInt = 5
        Log.e("TAG", "work chain 1 output: $outputInt")
        return Result.success(
            workDataOf(
                Constant.CHAIN_KEY to outputInt
            )
        )
    }
}