package com.appropriate.riskauditor.core.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.model.RiskLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * On-device executive PDF report generator using Android native PdfDocument.
 * Completely offline and self-contained; text is sanitized to prevent canvas overflow.
 */
class PdfReportGenerator(private val context: Context) {

    fun generateReport(summary: DeviceScanSummary): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val reportFile = File(reportsDir, "RiskAudit_Report_${summary.scanId.take(8)}.pdf")

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 dimensions in points
        var pageNumber = 1
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0D47A1")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#37474F")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#263238")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#B0BEC5")
            strokeWidth = 1f
        }

        var yPos = 45f

        // Document Header
        canvas.drawText("ON-DEVICE APP RISK AUDITOR REPORT", 40f, yPos, titlePaint)
        yPos += 18f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        val dateStr = dateFormat.format(Date(summary.timestampEpochMillis))
        canvas.drawText("Generated on-device without cloud transmission • $dateStr", 40f, yPos, subTitlePaint)
        yPos += 16f
        canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
        yPos += 24f

        // Device & Audit Metadata Box
        canvas.drawText("1. Device & Scan Overview", 40f, yPos, headerPaint)
        yPos += 16f
        canvas.drawText("Device Model: ${sanitize(Build.MANUFACTURER.uppercase())} ${sanitize(Build.MODEL)} (API ${Build.VERSION.SDK_INT})", 45f, yPos, bodyPaint)
        yPos += 14f
        canvas.drawText("Scan ID: ${sanitize(summary.scanId)}", 45f, yPos, bodyPaint)
        yPos += 14f
        canvas.drawText("Total Audited Apps: ${summary.totalAppsScanned} (${summary.userAppsScanned} User, ${summary.systemAppsScanned} System)", 45f, yPos, bodyPaint)
        yPos += 14f
        canvas.drawText("Average Risk Score: ${summary.averageRiskScore} / 100", 45f, yPos, bodyPaint)
        yPos += 22f

        // Risk Posture Distribution
        canvas.drawText("2. Risk Distribution Summary", 40f, yPos, headerPaint)
        yPos += 16f

        val critPaint = Paint().apply { color = Color.parseColor("#D32F2F"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
        val highPaint = Paint().apply { color = Color.parseColor("#F57C00"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
        val medPaint = Paint().apply { color = Color.parseColor("#FBC02D"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
        val lowPaint = Paint().apply { color = Color.parseColor("#388E3C"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }

        canvas.drawText("CRITICAL: ${summary.criticalRiskCount} apps", 45f, yPos, critPaint)
        canvas.drawText("HIGH: ${summary.highRiskCount} apps", 165f, yPos, highPaint)
        canvas.drawText("MEDIUM: ${summary.mediumRiskCount} apps", 285f, yPos, medPaint)
        canvas.drawText("LOW: ${summary.lowRiskCount} apps", 415f, yPos, lowPaint)
        yPos += 22f
        canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
        yPos += 24f

        // High & Critical Risk Findings Detail
        canvas.drawText("3. Prioritized Applications (High & Critical Risk)", 40f, yPos, headerPaint)
        yPos += 18f

        val flaggedApps = summary.highAndCriticalReports
        if (flaggedApps.isEmpty()) {
            canvas.drawText("No applications flagged with High or Critical risk posture.", 45f, yPos, bodyPaint)
            yPos += 20f
        } else {
            for (app in flaggedApps) {
                // Check if page overflow
                if (yPos > 740f) {
                    pdfDoc.finishPage(page)
                    pageNumber++
                    val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDoc.startPage(nextPageInfo)
                    canvas = page.canvas
                    yPos = 45f
                    canvas.drawText("3. Prioritized Applications (Continued)", 40f, yPos, headerPaint)
                    yPos += 22f
                }

                val scoreColor = if (app.riskLevel == RiskLevel.CRITICAL) "#D32F2F" else "#F57C00"
                val appHeaderPaint = Paint().apply { color = Color.parseColor(scoreColor); textSize = 11f; typeface = Typeface.DEFAULT_BOLD }

                canvas.drawText("${sanitize(app.inventory.appLabel)} (${sanitize(app.inventory.packageName)})", 45f, yPos, appHeaderPaint)
                canvas.drawText("Score: ${app.riskScore}/100 [${app.riskLevel.displayName}]", 420f, yPos, appHeaderPaint)
                yPos += 14f

                canvas.drawText("Target SDK: ${app.inventory.targetSdk ?: "Unknown"}  •  Installer: ${sanitize(app.inventory.installerPackage ?: "Sideloaded/Unknown")}", 50f, yPos, bodyPaint)
                yPos += 14f

                canvas.drawText("Key Evidence Findings (Why Flagged):", 50f, yPos, boldBodyPaint)
                yPos += 13f

                for (finding in app.findings.take(3)) {
                    val bulletText = "• ${sanitize(finding.title)}: ${sanitize(finding.evidence)}"
                    val truncated = if (bulletText.length > 95) bulletText.take(92) + "…" else bulletText
                    canvas.drawText(truncated, 55f, yPos, bodyPaint)
                    yPos += 12f
                }

                if (app.findings.isNotEmpty()) {
                    val recText = "Recommendation: ${sanitize(app.findings.first().recommendation)}"
                    val truncatedRec = if (recText.length > 95) recText.take(92) + "…" else recText
                    canvas.drawText(truncatedRec, 55f, yPos, subTitlePaint)
                    yPos += 14f
                }

                yPos += 10f
            }
        }

        // Footer / Disclaimer
        if (yPos > 760f) {
            pdfDoc.finishPage(page)
            pageNumber++
            val nextPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            page = pdfDoc.startPage(nextPageInfo)
            canvas = page.canvas
            yPos = 50f
        }

        yPos = 790f
        canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
        yPos += 14f
        canvas.drawText("PS-01 On-Device App Risk Auditor • Private & Deterministic Local Audit • No cloud data transmitted", 40f, yPos, subTitlePaint)

        pdfDoc.finishPage(page)

        FileOutputStream(reportFile).use { outStream ->
            pdfDoc.writeTo(outStream)
        }
        pdfDoc.close()

        return reportFile
    }

    private fun sanitize(input: String?): String {
        if (input == null) return "N/A"
        return input.replace("\n", " ")
            .replace("\r", " ")
            .filter { it.code in 32..126 || it.code in 160..255 }
            .trim()
    }
}
