package com.example.demoworkmanager

import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.work.*
import com.bumptech.glide.Glide
import com.example.demoworkmanager.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private var workManager: WorkManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        workManager = WorkManager.getInstance(applicationContext)
        binding.btnDownload.setOnClickListener {
            downloadFile(Constant.IMG_URL,FileMimeType.PNG)
        }
        binding.btnCancel.setOnClickListener {
            workManager?.cancelAllWork()
        }
        binding.btnTestPeriodicalWork.setOnClickListener {
            chainWork()
        }
    }

    private fun downloadFile(fileUrl: String, fileMimeType: String) {
        val inputData = Data.Builder().putString(FileParams.KEY_FILE_URL, fileUrl)
            .putString(FileParams.KEY_FILE_TYPE, fileMimeType)
            .build()
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequest.Builder(TestOneTimeWorker::class.java).setInputData(inputData)
            .setConstraints(constraints).addTag(Constant.DOWNLOAD_TAG).build()

        workManager?.enqueueUniqueWork("just download", ExistingWorkPolicy.KEEP, request)
        var observer = object : Observer<WorkInfo> {
            override fun onChanged(t: WorkInfo?) {
                Log.e("TAG", "One-time Work state " + t?.state)
                when (t?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        binding.btnDownload.text = "Image download success"
                        Glide.with(this@MainActivity).load(Uri.parse(t.outputData.getString(FileParams.KEY_FILE_URI))).into(binding.imv!!)
                        workManager?.getWorkInfoByIdLiveData(request.id)?.removeObserver(this)
                    }
                    WorkInfo.State.FAILED -> {binding.btnDownload.text = "Retry"}
                    WorkInfo.State.RUNNING -> {binding.btnDownload.text = "Downloading"}
                }
            }

        }
        workManager?.getWorkInfoByIdLiveData(request.id)?.observe(this@MainActivity,observer)
    }

    private fun periodicWork() {
        val periodicRequest = PeriodicWorkRequest.Builder(TestPeriodicWorker::class.java,15,TimeUnit.MINUTES)
            // setting a backoff on case the work needs to retry
            .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .addTag("LOG_TIME_TAG").build()
        workManager?.enqueueUniquePeriodicWork("LOG_TIME_NAME_UNIQUE",ExistingPeriodicWorkPolicy.KEEP,periodicRequest)
        workManager?.getWorkInfosByTagLiveData("LOG_TIME_TAG")?.observe(this@MainActivity){ listWorkInfo ->
            if (listWorkInfo == null || listWorkInfo.isEmpty()) {
                Log.e("TAG", "No work info found")
            } else {
                var workInfo = listWorkInfo[0]
                Log.e("TAG", "Periodic Work state " + workInfo.state)
            }
        }
    }

    private fun chainWork() {
        val chain1 = OneTimeWorkRequest.Builder(TestOneTimeWorker::class.java).build()
        val chain2 = OneTimeWorkRequest.Builder(TestChainWorker::class.java).build()
        val continueWork = workManager?.beginUniqueWork("Chain",ExistingWorkPolicy.KEEP,chain1)?.then(chain2)
        continueWork?.enqueue()
    }
}