package com.virtualprinter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import android.print.PrintManager
import android.content.Context

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            setupUI()
        } catch (e: Exception) {
            handleSetupError(e)
        }
    }

    private fun setupUI() {
        statusText = findViewById(R.id.statusText)
        
        // Add debug buttons
        findViewById<Button>(R.id.btnOpenSettings).setOnClickListener {
            openPrintSettings()
        }

        // Test print
        findViewById<Button>(R.id.btnTestPrint).setOnClickListener {
            testPrint()
        }
    }

    private fun openPrintSettings() {
        try {
            startActivity(Intent(Settings.ACTION_PRINT_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open print settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testPrint() {
        try {
            val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print(
                "Test Print Job",
                TestPrintDocumentAdapter(),
                null
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Error starting print job", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSetupError(error: Exception) {
        Toast.makeText(this, "Error setting up app: ${error.message}", Toast.LENGTH_LONG).show()
    }
} 