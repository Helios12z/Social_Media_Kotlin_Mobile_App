package com.example.socialmediaproject.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DeleteImageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }
}