package com.example.cryptile.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cryptile.app_data.room_files.SafeData
import com.google.gson.Gson

class CleanerWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    val TAG: String = this::class.java.simpleName

    companion object {
        const val safeJsonKey = "safe_json"
    }

    override fun doWork(): Result {
        return try {
            val dataReceived = inputData.getString(safeJsonKey)
            Log.d(TAG, "data received = $dataReceived")
            Gson()
                .fromJson(dataReceived, SafeData::class.java)
                .clearCache()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
