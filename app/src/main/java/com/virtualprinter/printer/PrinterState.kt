package com.virtualprinter.printer

import android.print.PrinterInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PrinterState {
    private val _status = MutableStateFlow(PrinterInfo.STATUS_IDLE)
    val status: StateFlow<Int> = _status

    fun setStatus(newStatus: Int) {
        _status.value = newStatus
    }

    fun isReady(): Boolean {
        return _status.value == PrinterInfo.STATUS_IDLE
    }
} 