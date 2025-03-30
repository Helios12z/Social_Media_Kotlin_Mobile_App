package com.example.socialmediaproject.ui.accountdetail

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.socialmediaproject.service.UpdateAccountWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

class AccountDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val workmanager=WorkManager.getInstance(application)
    val isavataruploading=MutableLiveData(false)
    val iswalluploading=MutableLiveData(false)
    private val _isUploading=MediatorLiveData<Boolean>().apply {
        addSource(isavataruploading) {value=it==true || iswalluploading.value==true}
        addSource(iswalluploading) {value=it==true || isavataruploading.value==true}
    }
    val isuploading: LiveData<Boolean> = _isUploading
    private val _workstatus=MutableLiveData<Boolean>()
    val workStatus: LiveData<Boolean> get()= _workstatus

    fun observeWorkStatus(workID: UUID) {
        workmanager.getWorkInfoByIdLiveData(workID).observeForever {
            workinfo->_workstatus.value=workinfo?.state == WorkInfo.State.RUNNING
        }
    }

    fun startUploadWorker(data: Data, context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<UpdateAccountWorker>()
            .setInputData(data)
            .addTag("upload")
            .build()
        observeWorkStatus(workRequest.id)
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun checkOngoingWork() {
        workmanager.getWorkInfosByTagLiveData("upload").observeForever { workInfos ->
            val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING }
            _workstatus.value = isRunning
        }
    }
}