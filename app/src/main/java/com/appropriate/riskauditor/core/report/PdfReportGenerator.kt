package com.appropriate.riskauditor.core.report

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import com.appropriate.riskauditor.core.model.DeviceScanSummary
import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.model.RiskLevel
import com.appropriate.riskauditor.core.rules.DeviceSummaryCalculator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * On-device executive PDF report generator using Android native PdfDocument.
 * Adheres strictly to Section 7.8:
 * - A4 dimensions 595x842 pt, margins 40 pt.
 * - Sanitizes text, strips bidi overrides and control chars.
 * - Deletes reports older than 7 days.
 * - Page 1: Device metadata, band counts, 9-category summary table, disclaimer.
 * - Following pages: sorted apps with up to 5 findings and page footers.
 */
class PdfReportGenerator(private val context: Context) {

    fun generateReport(summary: DeviceScanSummary): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        cleanupOldReports(reportsDir)

        val fileDateFormat = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)
        val timestampStr = fileDateFormat.format(Date(summary.timestampEpochMillis))
        val reportFile = File(reportsDir, "ps01-report-$timestampStr.pdf")

        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentWidth = pageWidth - (margin * 2)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0D47A1")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#37474F")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionHeaderPaint = Paint().apply {
            color = Color.parseColor("#1A237E")
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
            color = Color.parseColor("#CFD8DC")
            strokeWidth = 1f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        fun drawFooter(pNum: Int) {
            val footerY = pageHeight - 30f
            canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f, linePaint)
            val footerText = "Generated on this device. Permissions alone do not prove an app is malicious. • Page $pNum"
            canvas.drawText(footerText, margin, footerY + 2f, subTitlePaint)
        }

        var y = margin + 15f

        // Document Title
        canvas.drawText("PS-01 On-Device App Risk Auditor Report", margin, y, titlePaint)
        y += 18f

        val scanDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        val dateStr = scanDateFormat.format(Date(summary.timestampEpochMillis))
        canvas.drawText("Scan Date: $dateStr • On-Device Audit (No Cloud)", margin, y, subTitlePaint)
        y += 16f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 20f

        // Device Model & Version
        canvas.drawText("1. Device & Audit Overview", margin, y, sectionHeaderPaint)
        y += 16f
        canvas.drawText("Device Model: ${PdfTextUtil.sanitize(Build.MODEL)} (Manufacturer: ${PdfTextUtil.sanitize(Build.MANUFACTURER)})", margin + 5f, y, bodyPaint)
        y += 14f
        canvas.drawText("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", margin + 5f, y, bodyPaint)
        y += 14f
        canvas.drawText("Total Applications Audited: ${summary.totalAppsScanned} (${summary.userAppsScanned} User, ${summary.systemAppsScanned} System)", margin + 5f, y, bodyPaint)
        y += 22f

        // Risk Band Distribution
        canvas.drawText("2. Risk Band Posture", margin, y, sectionHeaderPaint)
        y += 16f

        val critPaint = Paint().apply { color = Color.parseColor("#B71C1C"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
        val highPaint = Paint().apply { color = Color.parseColor("#E65100"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
        val medPaint = Paint().apply { color = Color.parseColor("#F57F17"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }
        val lowPaint = Paint().apply { color = Color.parseColor("#1B5E20"); textSize = 10f; typeface = Typeface.DEFAULT_BOLD }

        canvas.drawText("CRITICAL (75-100): ${summary.criticalRiskCount} apps", margin + 5f, y, critPaint)
        canvas.drawText("HIGH (50-74): ${summary.highRiskCount} apps", margin + 140f, y, highPaint)
        canvas.drawText("MEDIUM (25-49): ${summary.mediumRiskCount} apps", margin + 260f, y, medPaint)
        canvas.drawText("LOW (0-24): ${summary.lowRiskCount} apps", margin + 395f, y, lowPaint)
        y += 22f

        // 9-Category Summary Table (text-based)
        canvas.drawText("3. Nine-Category Security Matrix", margin, y, sectionHeaderPaint)
        y += 16f

        // Compute 9 categories using DeviceSummaryCalculator
        val devSummary = DeviceSummaryCalculator.calculate(summary.appReports)
        canvas.drawText("Category", margin + 5f, y, boldBodyPaint)
        canvas.drawText("Apps with Granted Access", margin + 220f, y, boldBodyPaint)
        canvas.drawText("Apps Declaring", margin + 380f, y, boldBodyPaint)
        y += 8f
        canvas.drawLine(margin + 5f, y, pageWidth - margin, y, linePaint)
        y += 14f

        for (catStat in devSummary.categoryStats) {
            val catName = catStat.category.displayName
            canvas.drawText(catName, margin + 5f, y, bodyPaint)
            canvas.drawText("${catStat.appsWithAccess} apps", margin + 220f, y, bodyPaint)
            canvas.drawText("${catStat.appsDeclaring} apps", margin + 380f, y, bodyPaint)
            y += 13f
        }
        y += 15f

        // Disclaimer at bottom of Page 1
        canvas.drawText("Disclaimer: Generated on this device. Permissions alone do not prove an app is malicious.", margin + 5f, y, subTitlePaint)
        drawFooter(pageNumber)
        pdfDoc.finishPage(page)

        // Following Pages: Prioritized Audited Applications
        val sortedApps = summary.appReports.sortedWith(
            compareByDescending<com.appropriate.riskauditor.core.model.AppRiskReport> { it.riskScore }
                .thenBy { it.inventory.appLabel.lowercase() }
        )

        if (sortedApps.isNotEmpty()) {
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDoc.startPage(pageInfo)
            canvas = page.canvas
            y = margin + 15f
            canvas.drawText("4. Detailed Application Risk Assessments", margin, y, sectionHeaderPaint)
            y += 20f

            for (app in sortedApps) {
                // Check if enough vertical room for app block (~100pt)
                if (y > pageHeight - 120f) {
                    drawFooter(pageNumber)
                    pdfDoc.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDoc.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin + 15f
                    canvas.drawText("4. Detailed Application Risk Assessments (Continued)", margin, y, sectionHeaderPaint)
                    y += 20f
                }

                val scoreColor = when (app.riskLevel) {
                    RiskLevel.CRITICAL -> "#B71C1C"
                    RiskLevel.HIGH -> "#E65100"
                    RiskLevel.MEDIUM -> "#F57F17"
                    RiskLevel.LOW -> "#1B5E20"
                }
                val appTitlePaint = Paint().apply {
                    color = Color.parseColor(scoreColor)
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }

                val cappedLabel = PdfTextUtil.capLabel(app.inventory.appLabel, 60)
                canvas.drawText("$cappedLabel (${PdfTextUtil.sanitize(app.inventory.packageName)})", margin + 5f, y, appTitlePaint)
                canvas.drawText("Score: ${app.riskScore}/100 [${app.riskLevel.displayName}]", pageWidth - margin - 150f, y, appTitlePaint)
                y += 14f

                val installerStr = if (app.inventory.isSideloaded) "Installed outside a store" else (app.inventory.installerPackage ?: "Store")
                canvas.drawText("Installer: $installerStr  •  Target SDK: ${app.inventory.targetSdk ?: "Unknown"}", margin + 10f, y, subTitlePaint)
                y += 14f

                // Up to 5 findings with one-line explanation
                val findingsToShow = app.findings.take(5)
                if (findingsToShow.isEmpty()) {
                    canvas.drawText("• No significant risk findings observed.", margin + 15f, y, bodyPaint)
                    y += 13f
                } else {
                    for (finding in findingsToShow) {
                        val line = "• ${finding.title}: ${finding.explanation}"
                        val ellipsized = PdfTextUtil.ellipsize(line, bodyPaint, contentWidth - 20f)
                        canvas.drawText(ellipsized, margin + 15f, y, bodyPaint)
                        y += 13f
                    }
                }
                y += 10f
                canvas.drawLine(margin + 5f, y, pageWidth - margin, y, linePaint)
                y += 14f
            }

            drawFooter(pageNumber)
            pdfDoc.finishPage(page)
        }

        FileOutputStream(reportFile).use { outStream ->
            pdfDoc.writeTo(outStream)
        }
        pdfDoc.close()

        return reportFile
    }

    private fun cleanupOldReports(dir: File) {
        try {
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".pdf") && file.lastModified() < sevenDaysAgo) {
                    file.delete()
                }
            }
        } catch (ignored: Exception) {
        }
    }
}

typealias PdfReportExporter = PdfReportGenerator
