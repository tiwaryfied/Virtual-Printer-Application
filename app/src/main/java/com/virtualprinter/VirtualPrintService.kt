package com.virtualprinter

import android.printservice.PrintService
import android.printservice.PrintJob
import android.print.PrintJobInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import com.virtualprinter.printer.PrintJobProcessor
import com.virtualprinter.printer.PrinterState

class VirtualPrintService : PrintService() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val printerState = PrinterState()
    private val TAG = "VirtualPrintService"
    private var discoverySession: VirtualPrinterDiscoverySession? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VirtualPrintService created")
    }

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        return VirtualPrinterDiscoverySession(this).also {
            discoverySession = it
        }
    }

    override fun onPrintJobQueued(printJob: PrintJob) {
        if (!isValidPrintJob(printJob)) {
            printJob.fail(getString(R.string.invalid_print_job))
            return
        }

        serviceScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Error in print job processing", throwable)
            printJob.fail(getString(R.string.print_error))
        }) {
            processPrintJob(printJob)
        }
    }

    private fun isValidPrintJob(printJob: PrintJob): Boolean {
        return printJob.info != null && printJob.document != null
    }

    private suspend fun processPrintJob(printJob: PrintJob) {
        try {
            if (!printerState.isReady()) {
                printJob.fail(getString(R.string.printer_not_ready))
                return
            }

            printJob.start()
            printerState.setStatus(PrinterInfo.STATUS_PROCESSING)
            updatePrinterState(PrinterInfo.STATUS_PROCESSING)

            val processor = PrintJobProcessor(printJob)
            when (processor.process()) {
                PrintJobProcessor.Result.SUCCESS -> {
                    printJob.complete()
                    printerState.setStatus(PrinterInfo.STATUS_IDLE)
                }
                PrintJobProcessor.Result.ERROR -> {
                    printJob.fail(getString(R.string.processing_error))
                    printerState.setStatus(PrinterInfo.STATUS_IDLE)
                }
                PrintJobProcessor.Result.CANCELLED -> {
                    printJob.cancel()
                    printerState.setStatus(PrinterInfo.STATUS_IDLE)
                }
            }
            updatePrinterState(PrinterInfo.STATUS_IDLE)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing print job", e)
            printJob.fail(getString(R.string.print_error))
            printerState.setStatus(PrinterInfo.STATUS_IDLE)
            updatePrinterState(PrinterInfo.STATUS_IDLE)
        }
    }

    private fun updatePrinterState(status: Int) {
        discoverySession?.updatePrinterState(status)
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        serviceScope.launch {
            try {
                printJob.cancel()
                printerState.setStatus(PrinterInfo.STATUS_IDLE)
                updatePrinterState(PrinterInfo.STATUS_IDLE)
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling print job", e)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        discoverySession = null
        super.onDestroy()
    }
} 