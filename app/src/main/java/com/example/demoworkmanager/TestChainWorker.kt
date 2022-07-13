package com.example.demoworkmanager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TestChainWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val inputInt = inputData.getInt(Constant.CHAIN_KEY, 0)
        Log.e("TAG", "input data for second work in chain: $inputInt")
        return Result.success()
    }
}