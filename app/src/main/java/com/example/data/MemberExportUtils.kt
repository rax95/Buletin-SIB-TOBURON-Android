package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import com.example.R
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppLanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class MemberExportFormat(
    val label: String,
    val extension: String,
    val mimeType: String
) {
    EXCEL(
        label = "Excel (.xlsx)",
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ),
    PDF(
        label = "PDF (.pdf)",
        extension = "pdf",
        mimeType = "application/pdf"
    )
}

object MemberExportUtils {

    fun suggestedFileName(format: MemberExportFormat): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val prefix = AppLanguageManager.tr("Senarai_Ahli_SIB_Toburon", "SIB_Toburon_Members_List")
        return "${prefix}_${stamp}.${format.extension}"
    }

    fun suggestedMemberCvFileName(member: ChurchMemberDetail): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val safeName = member.fullName
            .trim()
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .take(40)
            .ifEmpty { "Ahli_SIB" }
        return "${safeName}_Maklumat_Ahli_${stamp}.pdf"
    }

    suspend fun exportMemberCvPdfToUri(
        context: Context,
        uri: Uri,
        member: ChurchMemberDetail
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bytes = buildMemberCvPdfBytes(context, member)
            val outputStream: OutputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException(
                    AppLanguageManager.tr(
                        "Tidak dapat membuka storan peranti untuk menyimpan fail.",
                        "Unable to open device storage to save the file."
                    )
                )
            outputStream.use { out ->
                out.write(bytes)
                out.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportMembersToUri(
        context: Context,
        uri: Uri,
        members: List<ChurchMemberDetail>,
        format: MemberExportFormat
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val bytes = when (format) {
                MemberExportFormat.EXCEL -> buildExcelWorkbookBytes(members)
                MemberExportFormat.PDF -> buildPdfDocumentBytes(members)
            }
            val outputStream: OutputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException(
                    AppLanguageManager.tr(
                        "Tidak dapat membuka storan peranti untuk menyimpan fail.",
                        "Unable to open device storage to save the file."
                    )
                )
            outputStream.use { out ->
                out.write(bytes)
                out.flush()
            }
            Result.success(members.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Builds a standards-compliant OOXML Excel (.xlsx) workbook using ZipOutputStream.
     * Opens natively in Microsoft Excel, Google Sheets, WPS Office, and LibreOffice.
     */
    private fun buildExcelWorkbookBytes(members: List<ChurchMemberDetail>): ByteArray {
        val lang = AppLanguageManager.current()
        val now = Date()
        val exportTimestamp = "${SimpleDateFormat("dd MMM yyyy", lang.locale).format(now)}, ${SimpleDateFormat("h:mm a", Locale.US).format(now)}"
        val headers = listOf(
            AppLanguageManager.tr("Bil", "No"),
            AppLanguageManager.tr("Nama Penuh", "Full Name"),
            AppLanguageManager.tr("No. MyKad", "MyKad No."),
            AppLanguageManager.tr("No. Tel", "Tel. No."),
            AppLanguageManager.tr("Jantina", "Gender"),
            AppLanguageManager.tr("Tarikh Lahir", "Date of Birth"),
            AppLanguageManager.tr("Umur (Tahun)", "Age (Years)"),
            AppLanguageManager.tr("Status Perkahwinan", "Marital Status"),
            AppLanguageManager.tr("Nama Pasangan", "Spouse Name"),
            AppLanguageManager.tr("Status Baptisan", "Baptism Status"),
            AppLanguageManager.tr("Denominasi", "Denomination"),
            AppLanguageManager.tr("No. Keanggotaan SIB", "SIB Membership No.")
        )

        val rowsXml = StringBuilder()
        // Title row (Row 1)
        rowsXml.append("""<row r="1" ht="24" customHeight="1">""")
        rowsXml.append(
            inlineStringCell(
                "A1",
                AppLanguageManager.tr("SENARAI AHLI JEMAAT SIB TOBURON", "SIB TOBURON CONGREGATION MEMBERS LIST"),
                style = 2
            )
        )
        rowsXml.append("""</row>""")

        // Subtitle / Export metadata row (Row 2)
        rowsXml.append("""<row r="2" ht="18" customHeight="1">""")
        rowsXml.append(
            inlineStringCell(
                "A2",
                AppLanguageManager.tr(
                    "Dieksport pada: $exportTimestamp  •  Jumlah Ahli: ${members.size}",
                    "Exported on: $exportTimestamp  •  Total Members: ${members.size}"
                ),
                style = 3
            )
        )
        rowsXml.append("""</row>""")

        // Blank spacer row (Row 3)
        rowsXml.append("""<row r="3"/>""")

        // Header row (Row 4)
        rowsXml.append("""<row r="4" ht="22" customHeight="1">""")
        headers.forEachIndexed { colIdx, text ->
            val ref = "${colLetter(colIdx)}4"
            rowsXml.append(inlineStringCell(ref, text, style = 1))
        }
        rowsXml.append("""</row>""")

        // Data rows starting at Row 5
        members.forEachIndexed { idx, member ->
            val rowNum = idx + 5
            val ageText = member.calculatedAge()?.toString() ?: "-"
            val birthReadable = formatBirthDateReadable(member.birthDate, lang)
            val denomText = member.denomination.takeIf { it.isNotBlank() } ?: "-"
            val spouseText = member.spouseName?.takeIf { it.isNotBlank() } ?: "-"
            val values = listOf(
                (idx + 1).toString(),
                member.fullName,
                member.myKadNumber?.takeIf { it.isNotBlank() } ?: "-",
                member.phoneNumber?.takeIf { it.isNotBlank() } ?: "-",
                member.resolvedGender().localizedLabel(lang),
                birthReadable,
                ageText,
                member.resolvedMaritalStatusLabel(lang),
                spouseText,
                member.resolvedBaptismStatus().localizedLabel(lang),
                denomText,
                member.resolvedSibMembershipLabel(lang)
            )
            rowsXml.append("""<row r="$rowNum">""")
            values.forEachIndexed { colIdx, value ->
                val ref = "${colLetter(colIdx)}$rowNum"
                if (colIdx == 0 || (colIdx == 6 && value.toIntOrNull() != null)) {
                    rowsXml.append("""<c r="$ref" s="4"><v>$value</v></c>""")
                } else {
                    rowsXml.append(inlineStringCell(ref, value, style = 4))
                }
            }
            rowsXml.append("""</row>""")
        }

        val sheetXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="6" customWidth="1"/>
    <col min="2" max="2" width="28" customWidth="1"/>
    <col min="3" max="3" width="18" customWidth="1"/>
    <col min="4" max="4" width="17" customWidth="1"/>
    <col min="5" max="5" width="12" customWidth="1"/>
    <col min="6" max="6" width="16" customWidth="1"/>
    <col min="7" max="7" width="13" customWidth="1"/>
    <col min="8" max="8" width="18" customWidth="1"/>
    <col min="9" max="9" width="22" customWidth="1"/>
    <col min="10" max="10" width="18" customWidth="1"/>
    <col min="11" max="11" width="16" customWidth="1"/>
    <col min="12" max="12" width="20" customWidth="1"/>
  </cols>
  <sheetData>
    $rowsXml
  </sheetData>
  <mergeCells count="2">
    <mergeCell ref="A1:L1"/>
    <mergeCell ref="A2:L2"/>
  </mergeCells>
</worksheet>"""

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val sheetName = AppLanguageManager.tr("Senarai Ahli", "Members List")
        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="$sheetName" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="14"/><color rgb="FF1B4332"/><name val="Calibri"/></font>
    <font><i/><sz val="10"/><color rgb="FF555555"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1B4332"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD0D7D4"/></left>
      <right style="thin"><color rgb="FFD0D7D4"/></right>
      <top style="thin"><color rgb="FFD0D7D4"/></top>
      <bottom style="thin"><color rgb="FFD0D7D4"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="5">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
  </cellXfs>
</styleSheet>"""

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml)
            writeZipEntry(zip, "_rels/.rels", rootRelsXml)
            writeZipEntry(zip, "xl/workbook.xml", workbookXml)
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml)
            writeZipEntry(zip, "xl/styles.xml", stylesXml)
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }
        return baos.toByteArray()
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun colLetter(index: Int): String = ('A' + index).toString()

    private fun inlineStringCell(ref: String, rawValue: String, style: Int): String {
        val escaped = rawValue
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
        return """<c r="$ref" t="inlineStr" s="$style"><is><t>$escaped</t></is></c>"""
    }

    /**
     * Builds a formatted A4 Landscape multi-page PDF report using Android's native PdfDocument.
     */
    private fun buildPdfDocumentBytes(members: List<ChurchMemberDetail>): ByteArray {
        val pageWidth = 842
        val pageHeight = 595
        val marginX = 36f
        val topMargin = 40f
        val bottomMargin = 44f
        val rowHeight = 24f
        val headerRowHeight = 26f
        val tableWidth = pageWidth - marginX * 2f

        val lang = AppLanguageManager.current()
        // Column proportions summing to 1.0f
        val colWeights = floatArrayOf(0.04f, 0.19f, 0.13f, 0.08f, 0.12f, 0.06f, 0.14f, 0.12f, 0.12f)
        val colTitles = listOf(
            AppLanguageManager.tr("Bil", "No"),
            AppLanguageManager.tr("Nama Penuh", "Full Name"),
            AppLanguageManager.tr("No. MyKad", "MyKad"),
            AppLanguageManager.tr("Jantina", "Gender"),
            AppLanguageManager.tr("Tarikh Lahir", "Birth Date"),
            AppLanguageManager.tr("Umur", "Age"),
            AppLanguageManager.tr("Perkahwinan", "Marital"),
            AppLanguageManager.tr("Baptisan", "Baptism"),
            AppLanguageManager.tr("No. SIB", "SIB No.")
        )
        val colWidths = colWeights.map { it * tableWidth }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 67, 50)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(85, 95, 90)
            textSize = 10f
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(27, 67, 50)
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val zebraBgPaint = Paint().apply {
            color = Color.rgb(244, 247, 245)
            style = Paint.Style.FILL
        }
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 32, 30)
            textSize = 9.5f
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(208, 215, 212)
            strokeWidth = 0.7f
            style = Paint.Style.STROKE
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 118, 114)
            textSize = 9f
        }

        val now = Date()
        val exportTimestamp = "${SimpleDateFormat("dd MMM yyyy", lang.locale).format(now)}, ${SimpleDateFormat("h:mm a", Locale.US).format(now)}"
        val firstPageStartY = topMargin + 54f
        val otherPageStartY = topMargin + 26f
        val maxContentY = pageHeight - bottomMargin

        val firstPageCapacity = ((maxContentY - firstPageStartY - headerRowHeight) / rowHeight).toInt().coerceAtLeast(1)
        val nextPageCapacity = ((maxContentY - otherPageStartY - headerRowHeight) / rowHeight).toInt().coerceAtLeast(1)

        val pages = mutableListOf<List<Pair<Int, ChurchMemberDetail>>>()
        val indexedMembers = members.mapIndexed { idx, m -> (idx + 1) to m }
        if (indexedMembers.isEmpty()) {
            pages.add(emptyList())
        } else {
            var cursor = 0
            while (cursor < indexedMembers.size) {
                val cap = if (pages.isEmpty()) firstPageCapacity else nextPageCapacity
                val end = (cursor + cap).coerceAtMost(indexedMembers.size)
                pages.add(indexedMembers.subList(cursor, end))
                cursor = end
            }
        }

        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { pageIndex, pageRows ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                var currentY = topMargin
                if (pageIndex == 0) {
                    canvas.drawText(
                        AppLanguageManager.tr("SENARAI AHLI JEMAAT SIB TOBURON", "SIB TOBURON CONGREGATION MEMBERS LIST"),
                        marginX,
                        currentY + 14f,
                        titlePaint
                    )
                    canvas.drawText(
                        AppLanguageManager.tr(
                            "Dieksport pada: $exportTimestamp   •   Jumlah Ahli Berdaftar: ${members.size}",
                            "Exported on: $exportTimestamp   •   Total Registered Members: ${members.size}"
                        ),
                        marginX,
                        currentY + 32f,
                        subtitlePaint
                    )
                    currentY = firstPageStartY
                } else {
                    canvas.drawText(
                        AppLanguageManager.tr(
                            "Senarai Ahli Jemaat SIB Toburon (Sambungan)",
                            "SIB Toburon Congregation Members List (Continued)"
                        ),
                        marginX,
                        currentY + 12f,
                        subtitlePaint
                    )
                    currentY = otherPageStartY
                }

                // Draw Table Header
                canvas.drawRect(marginX, currentY, marginX + tableWidth, currentY + headerRowHeight, headerBgPaint)
                var xCursor = marginX
                colTitles.forEachIndexed { idx, colTitle ->
                    canvas.drawText(
                        ellipsizeText(colTitle, headerTextPaint, colWidths[idx] - 10f),
                        xCursor + 6f,
                        currentY + 16.5f,
                        headerTextPaint
                    )
                    xCursor += colWidths[idx]
                }
                currentY += headerRowHeight

                // Draw Rows
                pageRows.forEachIndexed { rowInPage, (seqNo, member) ->
                    if (rowInPage % 2 == 1) {
                        canvas.drawRect(marginX, currentY, marginX + tableWidth, currentY + rowHeight, zebraBgPaint)
                    }
                    canvas.drawRect(marginX, currentY, marginX + tableWidth, currentY + rowHeight, borderPaint)

                    val ageUnit = AppLanguageManager.tr("thn", "yrs")
                    val ageStr = member.calculatedAge()?.let { "$it $ageUnit" } ?: "-"
                    val maritalSummary = if (!member.spouseName.isNullOrBlank()) {
                        "${member.resolvedMaritalStatusLabel(lang)} (${member.spouseName})"
                    } else {
                        member.resolvedMaritalStatusLabel(lang)
                    }
                    val baptismSummary = if (member.denomination.isNotBlank()) {
                        "${member.resolvedBaptismStatus().localizedLabel(lang)} (${member.denomination})"
                    } else {
                        member.resolvedBaptismStatus().localizedLabel(lang)
                    }
                    val rowValues = listOf(
                        seqNo.toString(),
                        member.fullName,
                        member.myKadNumber?.takeIf { it.isNotBlank() } ?: "-",
                        member.resolvedGender().localizedLabel(lang),
                        formatBirthDateReadable(member.birthDate, lang),
                        ageStr,
                        maritalSummary,
                        baptismSummary,
                        member.resolvedSibMembershipLabel(lang)
                    )

                    var cellX = marginX
                    rowValues.forEachIndexed { colIdx, text ->
                        canvas.drawText(
                            ellipsizeText(text, cellTextPaint, colWidths[colIdx] - 10f),
                            cellX + 6f,
                            currentY + 15.5f,
                            cellTextPaint
                        )
                        cellX += colWidths[colIdx]
                        if (colIdx < colWidths.lastIndex) {
                            canvas.drawLine(cellX, currentY, cellX, currentY + rowHeight, borderPaint)
                        }
                    }
                    currentY += rowHeight
                }

                // Footer
                canvas.drawText(
                    AppLanguageManager.tr(
                        "SIB Toburon • Dokumen Rasmi Pengurusan Admin",
                        "SIB Toburon • Official Admin Directory Report"
                    ),
                    marginX,
                    pageHeight - 18f,
                    footerPaint
                )
                val pageLabel = AppLanguageManager.tr(
                    "Halaman ${pageIndex + 1} / ${pages.size}",
                    "Page ${pageIndex + 1} / ${pages.size}"
                )
                val pageLabelWidth = footerPaint.measureText(pageLabel)
                canvas.drawText(
                    pageLabel,
                    pageWidth - marginX - pageLabelWidth,
                    pageHeight - 18f,
                    footerPaint
                )

                pdf.finishPage(page)
            }

            val baos = ByteArrayOutputStream()
            pdf.writeTo(baos)
            return baos.toByteArray()
        } finally {
            pdf.close()
        }
    }

    /**
     * Builds an A4 Portrait CV / Resume-style Member Profile PDF for an individual church member.
     */
    internal fun buildMemberCvPdfBytes(
        context: Context,
        member: ChurchMemberDetail
    ): ByteArray {
        val pageWidth = 595
        val pageHeight = 842
        val headerHeight = 144f
        val goldBarHeight = 4f
        val bodyTop = headerHeight + goldBarHeight
        val footerTop = 804f

        val pineDeep = Color.rgb(16, 46, 38)
        val pineMid = Color.rgb(27, 67, 50)
        val goldAccent = Color.rgb(212, 175, 55)
        val goldLight = Color.rgb(243, 217, 139)
        val dividerColor = Color.rgb(210, 219, 214)
        val textDark = Color.rgb(24, 30, 27)
        val textMuted = Color.rgb(90, 102, 96)
        val badgeBg = Color.rgb(225, 240, 232)

        val lang = AppLanguageManager.current()
        val now = Date()
        val exportTimestamp = "${SimpleDateFormat("dd MMM yyyy", lang.locale).format(now)}, ${SimpleDateFormat("h:mm a", Locale.US).format(now)}"
        val displayName = member.fullName.trim().ifEmpty {
            AppLanguageManager.tr("Ahli Jemaat SIB Toburon", "SIB Toburon Member")
        }
        val sibLabel = member.resolvedSibMembershipLabel(lang)
        val hasSib = !member.sibMembershipNumber.isNullOrBlank()
        val myKadDisplay = member.myKadNumber?.trim()?.takeIf { it.isNotEmpty() } ?: "-"
        val phoneDisplay = member.phoneNumber?.trim()?.takeIf { it.isNotEmpty() } ?: "-"
        val birthDisplay = member.birthDate.trim().takeIf { it.isNotEmpty() }
            ?.let { formatBirthDateReadable(it, lang) } ?: "-"
        val ageValue = member.calculatedAge()
        val ageDisplay = ageValue?.let {
            if (lang == AppLanguage.EN) "$it years old" else "$it tahun"
        } ?: "-"
        val genderDisplay = member.resolvedGender().localizedLabel(lang)
        val maritalDisplay = member.resolvedMaritalStatusLabel(lang)
        val isMarried = member.resolvedMaritalStatus() == MaritalStatus.BERKAHWIN
        val spouseValue = member.spouseName?.trim()?.takeIf { it.isNotEmpty() } ?: "-"
        val baptismStatus = member.resolvedBaptismStatus()
        val isBaptized = baptismStatus == BaptismStatus.DIBAPTIS
        val baptismDisplay = baptismStatus.localizedLabel(lang)
        val denominationDisplay = member.denomination.trim().takeIf { it.isNotEmpty() } ?: "-"
        val emailDisplay = member.memberEmail?.trim()?.takeIf { it.isNotEmpty() }
            ?: AppLanguageManager.tr("Belum dipautkan", "Not linked yet")

        val pdf = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Page background
            val whitePaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), whitePaint)

            val borderLinePaint = Paint().apply {
                color = dividerColor
                strokeWidth = 0.9f
                style = Paint.Style.STROKE
            }

            // 3. Top Executive CV Header Banner
            val headerPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, pageWidth.toFloat(), headerHeight,
                    pineDeep, pineMid, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, headerPaint)

            // Subtle decorative watermark circle in top-right of banner
            val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(18, 255, 255, 255)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(pageWidth - 36f, 24f, 88f, watermarkPaint)

            // Gold accent stripe
            val goldBarPaint = Paint().apply { color = goldAccent; style = Paint.Style.FILL }
            canvas.drawRect(0f, headerHeight, pageWidth.toFloat(), bodyTop, goldBarPaint)

            // 4. Passport Portrait (Top-Left inside Header)
            val photoRect = RectF(26f, 18f, 112f, 128f) // 86 x 110 pt (~3:4)
            val photoFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(photoRect, 9f, 9f, photoFramePaint)

            val innerPhotoRect = RectF(
                photoRect.left + 2.5f,
                photoRect.top + 2.5f,
                photoRect.right - 2.5f,
                photoRect.bottom - 2.5f
            )
            val passportBitmap = decodeBase64Bitmap(member.passportPhotoBase64)
            if (passportBitmap != null) {
                drawBitmapCenterCroppedInRoundedRect(canvas, passportBitmap, innerPhotoRect, 7f)
                passportBitmap.recycle()
            } else {
                val placeholderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(225, 237, 230)
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(innerPhotoRect, 7f, 7f, placeholderBg)
                val initials = displayName
                    .split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "ST" }
                val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = pineMid
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(initials, innerPhotoRect.centerX(), innerPhotoRect.centerY() + 4f, initialsPaint)
                val noPhotoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textMuted
                    textSize = 7.5f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(
                    AppLanguageManager.tr("TIADA FOTO", "NO PHOTO"),
                    innerPhotoRect.centerX(),
                    innerPhotoRect.bottom - 12f,
                    noPhotoPaint
                )
            }

            // 5. Church Emblem (Top-Right inside Header)
            val emblemBitmap = runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.sib_toburon_logo)
            }.getOrNull()
            if (emblemBitmap != null) {
                val emblemRect = RectF(pageWidth - 68f, 18f, pageWidth - 26f, 60f)
                val emblemBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
                canvas.drawOval(emblemRect, emblemBgPaint)
                val emblemInner = RectF(emblemRect.left + 2f, emblemRect.top + 2f, emblemRect.right - 2f, emblemRect.bottom - 2f)
                drawBitmapCenterCroppedInRoundedRect(canvas, emblemBitmap, emblemInner, 20f)
                val emblemStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = goldLight
                    style = Paint.Style.STROKE
                    strokeWidth = 1.4f
                }
                canvas.drawOval(emblemRect, emblemStroke)
                emblemBitmap.recycle()
            }

            // 6. Header Text (Member Name & Badges)
            val headerTextLeft = 130f
            val headerMaxWidth = pageWidth - 82f - headerTextLeft
            val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = goldLight
                textSize = 8.5f
                letterSpacing = 0.08f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                AppLanguageManager.tr(
                    "GEREJA SIB TOBURON  •  MAKLUMAT AHLI",
                    "SIB TOBURON CHURCH  •  MEMBER INFORMATION"
                ),
                headerTextLeft,
                32f,
                eyebrowPaint
            )

            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val nameLines = wrapText(displayName, namePaint, headerMaxWidth).take(2)
            var nameY = 58f
            nameLines.forEachIndexed { idx, line ->
                val text = if (idx == 1 && nameLines.size == 2) {
                    ellipsizeText(line, namePaint, headerMaxWidth)
                } else {
                    line
                }
                canvas.drawText(text, headerTextLeft, nameY, namePaint)
                nameY += 23f
            }

            // Header Pill Badges (SIB No. + Baptism Status)
            val badgeY = (nameY + 4f).coerceAtMost(106f)
            val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = pineDeep
                textSize = 8.8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val sibChipText = if (hasSib) "SIB: $sibLabel" else AppLanguageManager.tr("NO. SIB: Belum ada", "SIB NO: Unassigned")
            val sibChipWidth = pillTextPaint.measureText(sibChipText) + 18f
            val sibChipRect = RectF(headerTextLeft, badgeY, headerTextLeft + sibChipWidth, badgeY + 19f)
            val goldChipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = goldLight; style = Paint.Style.FILL }
            canvas.drawRoundRect(sibChipRect, 9.5f, 9.5f, goldChipPaint)
            canvas.drawText(sibChipText, sibChipRect.left + 9f, sibChipRect.top + 13f, pillTextPaint)

            val statusChipText = if (isBaptized && member.denomination.isNotBlank()) {
                "$baptismDisplay • ${member.denomination.trim()}"
            } else {
                baptismDisplay
            }
            val statusChipX = sibChipRect.right + 8f
            val availableStatusW = (pageWidth - 26f - statusChipX - 18f).coerceAtLeast(60f)
            val fittedStatusText = ellipsizeText(statusChipText, pillTextPaint, availableStatusW)
            val statusChipW = pillTextPaint.measureText(fittedStatusText) + 18f
            val statusChipRect = RectF(statusChipX, badgeY, statusChipX + statusChipW, badgeY + 19f)
            val statusChipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isBaptized) badgeBg else Color.argb(46, 255, 255, 255)
                style = Paint.Style.FILL
            }
            val statusChipTextPaint = Paint(pillTextPaint).apply {
                color = if (isBaptized) pineDeep else Color.WHITE
            }
            canvas.drawRoundRect(statusChipRect, 9.5f, 9.5f, statusChipBg)
            canvas.drawText(fittedStatusText, statusChipRect.left + 9f, statusChipRect.top + 13f, statusChipTextPaint)

            // 7. FULL-WIDTH MAIN CV BODY
            val mainX = 30f
            val mainRight = pageWidth - 30f
            val mainW = mainRight - mainX // 535f
            var mainY = bodyTop + 28f

            val rowLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textMuted
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rowValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textDark
                textSize = 10.2f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // Section 1: Personal Details
            mainY = drawCvSectionHeader(
                canvas,
                title = AppLanguageManager.tr("BUTIRAN PERIBADI", "PERSONAL DETAILS"),
                x = mainX, y = mainY, width = mainW,
                pineColor = pineDeep, goldColor = goldAccent, ruleColor = dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("Nama Penuh", "Full Name"),
                displayName, rowLabelPaint, rowValuePaint, dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("No. MyKad", "MyKad No."),
                myKadDisplay, rowLabelPaint, rowValuePaint, dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("No. Tel", "Tel. No."),
                phoneDisplay, rowLabelPaint, rowValuePaint, dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("Tarikh Lahir", "Date of Birth"),
                birthDisplay, rowLabelPaint, rowValuePaint, dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("Umur", "Age"),
                ageDisplay, rowLabelPaint, rowValuePaint, dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("Jantina", "Gender"),
                genderDisplay, rowLabelPaint, rowValuePaint, dividerColor,
                drawBottomDivider = false
            )
            mainY += 18f

            // Section 2: Family & Marital Status
            mainY = drawCvSectionHeader(
                canvas,
                title = AppLanguageManager.tr("KELUARGA & PERKAHWINAN", "FAMILY & MARITAL STATUS"),
                x = mainX, y = mainY, width = mainW,
                pineColor = pineDeep, goldColor = goldAccent, ruleColor = dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("Status Perkahwinan", "Marital Status"),
                maritalDisplay, rowLabelPaint, rowValuePaint, dividerColor,
                drawBottomDivider = isMarried || !member.spouseName.isNullOrBlank()
            )
            if (isMarried || !member.spouseName.isNullOrBlank()) {
                mainY = drawDetailKeyValueRow(
                    canvas, mainX, mainY, mainW,
                    member.resolvedSpouseFieldLabel(lang),
                    spouseValue, rowLabelPaint, rowValuePaint, dividerColor,
                    drawBottomDivider = false
                )
            }
            mainY += 18f

            // Section 3: Spiritual & Church Membership Record
            mainY = drawCvSectionHeader(
                canvas,
                title = AppLanguageManager.tr("REKOD KEROHANIAN & KEANGGOTAAN", "SPIRITUAL & MEMBERSHIP RECORD"),
                x = mainX, y = mainY, width = mainW,
                pineColor = pineDeep, goldColor = goldAccent, ruleColor = dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("Status Baptisan", "Baptism Status"),
                baptismDisplay, rowLabelPaint, rowValuePaint, dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("Denominasi Gereja", "Church Denomination"),
                denominationDisplay, rowLabelPaint, rowValuePaint, dividerColor
            )
            mainY = drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("No. Keanggotaan SIB", "SIB Membership No."),
                sibLabel, rowLabelPaint, rowValuePaint, dividerColor
            )
            drawDetailKeyValueRow(
                canvas, mainX, mainY, mainW,
                AppLanguageManager.tr("Akaun Google Terpaut", "Linked Google Account"),
                emailDisplay, rowLabelPaint, rowValuePaint, dividerColor,
                drawBottomDivider = false
            )

            // 9. Document Footer
            canvas.drawLine(0f, footerTop, pageWidth.toFloat(), footerTop, borderLinePaint)
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textMuted
                textSize = 8.5f
            }
            canvas.drawText(
                AppLanguageManager.tr(
                    "Gereja SIB Toburon  •  Dokumen Maklumat Ahli Rasmi",
                    "SIB Toburon Church  •  Official Member Information Document"
                ),
                26f,
                footerTop + 22f,
                footerPaint
            )
            val rightFooter = AppLanguageManager.tr(
                "Dieksport: $exportTimestamp   •   Halaman 1 / 1",
                "Exported: $exportTimestamp   •   Page 1 / 1"
            )
            val rightFooterW = footerPaint.measureText(rightFooter)
            canvas.drawText(rightFooter, pageWidth - 26f - rightFooterW, footerTop + 22f, footerPaint)

            pdf.finishPage(page)

            val baos = ByteArrayOutputStream()
            pdf.writeTo(baos)
            return baos.toByteArray()
        } finally {
            pdf.close()
        }
    }

    private fun drawCvSectionHeader(
        canvas: android.graphics.Canvas,
        title: String,
        x: Float,
        y: Float,
        width: Float,
        pineColor: Int,
        goldColor: Int,
        ruleColor: Int
    ): Float {
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pineColor
            textSize = 11f
            letterSpacing = 0.05f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, x, y, headerPaint)
        val ruleY = y + 6f
        canvas.drawLine(x, ruleY, x + width, ruleY, Paint().apply { color = ruleColor; strokeWidth = 0.9f })
        canvas.drawLine(x, ruleY, x + 52f, ruleY, Paint().apply { color = goldColor; strokeWidth = 2.2f })
        return y + 24f
    }

    private fun drawDetailKeyValueRow(
        canvas: android.graphics.Canvas,
        x: Float,
        startY: Float,
        width: Float,
        label: String,
        value: String,
        labelPaint: Paint,
        valuePaint: Paint,
        dividerColor: Int,
        drawBottomDivider: Boolean = true
    ): Float {
        val labelColWidth = width * 0.36f
        val valueX = x + labelColWidth + 12f
        val valueWidth = width - labelColWidth - 12f

        canvas.drawText(ellipsizeText(label, labelPaint, labelColWidth), x, startY, labelPaint)
        val lines = wrapText(value.ifBlank { "-" }, valuePaint, valueWidth).take(3)
        var y = startY
        lines.forEach { line ->
            canvas.drawText(line, valueX, y, valuePaint)
            y += 14.5f
        }
        val rowBottom = y - 1f
        if (drawBottomDivider) {
            canvas.drawLine(
                x, rowBottom, x + width, rowBottom,
                Paint().apply { color = dividerColor; strokeWidth = 0.6f }
            )
        }
        return rowBottom + 15f
    }

    private fun decodeBase64Bitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val cleaned = base64.substringAfter("base64,", base64).trim()
            val bytes = Base64.decode(cleaned, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun drawBitmapCenterCroppedInRoundedRect(
        canvas: android.graphics.Canvas,
        bitmap: Bitmap,
        dst: RectF,
        cornerRadius: Float
    ) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return
        val dstRatio = dst.width() / dst.height()
        val srcRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val srcRect = if (srcRatio > dstRatio) {
            val cropW = (bitmap.height * dstRatio).toInt().coerceAtLeast(1)
            val left = ((bitmap.width - cropW) / 2).coerceAtLeast(0)
            Rect(left, 0, (left + cropW).coerceAtMost(bitmap.width), bitmap.height)
        } else {
            val cropH = (bitmap.width / dstRatio).toInt().coerceAtLeast(1)
            val top = ((bitmap.height - cropH) / 2).coerceAtLeast(0)
            Rect(0, top, bitmap.width, (top + cropH).coerceAtMost(bitmap.height))
        }
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(dst, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, srcRect, dst, bitmapPaint)
        canvas.restore()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return listOf("-")
        val result = mutableListOf<String>()
        val words = trimmed.split(Regex("\\s+"))
        var currentLine = StringBuilder()

        fun flushLine() {
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
                currentLine = StringBuilder()
            }
        }

        words.forEach { word ->
            val candidate = if (currentLine.isEmpty()) word else "${currentLine} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine.clear()
                currentLine.append(candidate)
            } else {
                flushLine()
                if (paint.measureText(word) <= maxWidth) {
                    currentLine.append(word)
                } else {
                    // Break extra-long tokens (such as emails or long IDs) cleanly
                    var chunk = StringBuilder()
                    word.forEach { ch ->
                        val next = chunk.toString() + ch
                        if (paint.measureText(next) <= maxWidth || chunk.isEmpty()) {
                            chunk.append(ch)
                        } else {
                            result.add(chunk.toString())
                            chunk = StringBuilder().append(ch)
                        }
                    }
                    if (chunk.isNotEmpty()) {
                        currentLine.append(chunk.toString())
                    }
                }
            }
        }
        flushLine()
        return if (result.isEmpty()) listOf("-") else result
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end).trimEnd() + ellipsis
    }
}
