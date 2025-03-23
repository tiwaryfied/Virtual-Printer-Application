package com.virtualprinter.printer

import android.printservice.PrintJob
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.FileInputStream

class PrintJobProcessor(private val printJob: PrintJob) {
    private val TAG = "PrintJobProcessor"
    private val TIMEOUT_MILLIS = 30000L // 30 seconds timeout

    enum class Result {
        SUCCESS, ERROR, CANCELLED
    }

    suspend fun process(): Result = withContext(Dispatchers.IO) {
        try {
            withTimeout(TIMEOUT_MILLIS) {
                processWithTimeout()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing print job", e)
            Result.ERROR
        }
    }

    private suspend fun processWithTimeout(): Result {
        val document = printJob.document ?: return Result.ERROR

        try {
            document.data?.let { fd ->
                val size = processDocument(fd)
                Log.d(TAG, "Processed document size: $size bytes")
            } ?: run {
                Log.e(TAG, "No document data available")
                return Result.ERROR
            }

            return when {
                printJob.isCompleted -> Result.SUCCESS
                printJob.isCancelled -> Result.CANCELLED
                else -> Result.SUCCESS
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error processing document", e)
            return Result.ERROR
        }
    }

    private suspend fun processDocument(fd: ParcelFileDescriptor): Long = withContext(Dispatchers.IO) {
        var size = 0L
        try {
            FileInputStream(fd.fileDescriptor).use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    size += bytesRead
                    // Simulate processing time
                    kotlinx.coroutines.delay(100)
                    
                    if (printJob.isCancelled) {
                        throw IOException("Print job cancelled")
                    }
                }
            }
        } catch (e: IOException) {
            throw IOException("Error reading document", e)
        }
        size
    }
} 