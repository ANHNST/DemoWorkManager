package com.example.demoworkmanager

import android.net.Uri
import android.os.Bundle
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
    }

    private fun downloadFile(fileUrl: String, fileMimeType: String) {
        val inputData = Data.Builder().putString(FileParams.KEY_FILE_URL, fileUrl)
            .putString(FileParams.KEY_FILE_TYPE, fileMimeType)
            .build()
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequest.Builder(DownloadFileWorker::class.java).setInputData(inputData)
            .setConstraints(constraints).addTag(Constant.DOWNLOAD_TAG).build()

        workManager?.enqueueUniqueWork("just download", ExistingWorkPolicy.KEEP, request)
        var observer = object : Observer<WorkInfo> {
            override fun onChanged(t: WorkInfo?) {
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
}