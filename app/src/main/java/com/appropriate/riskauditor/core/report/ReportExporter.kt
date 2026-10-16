package com.appropriate.riskauditor.core.report

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.appropriate.riskauditor.core.model.DeviceScanSummary

/**
 * Handles safe sharing of generated PDF audit reports via Android FileProvider.
 */
class ReportExporter(private val context: Context) {

    private val pdfGenerator = PdfReportGenerator(context)

    fun exportAndShareReport(summary: DeviceScanSummary): Boolean {
        return try {
            val file = pdfGenerator.generateReport(summary)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "App Risk Audit Report - ${summary.scanId.take(8)}")
                putExtra(Intent.EXTRA_TEXT, "Attached is the local on-device application risk audit report generated for this device.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Security Audit Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }
}
