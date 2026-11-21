package com.example.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
        return "Senarai_Ahli_SIB_Toburon_${stamp}.${format.extension}"
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
                ?: throw IllegalStateException("Tidak dapat membuka storan peranti untuk menyimpan fail.")
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
        val exportTimestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
        val headers = listOf(
            "No",
            "Nama Penuh",
            "Jantina",
            "Tarikh Lahir",
            "Umur (Tahun)",
            "Status Baptisan",
            "Denominasi"
        )

        val rowsXml = StringBuilder()
        // Title row (Row 1)
        rowsXml.append("""<row r="1" ht="24" customHeight="1">""")
        rowsXml.append(inlineStringCell("A1", "SENARAI AHLI JEMAAT SIB TOBURON", style = 2))
        rowsXml.append("""</row>""")

        // Subtitle / Export metadata row (Row 2)
        rowsXml.append("""<row r="2" ht="18" customHeight="1">""")
        rowsXml.append(inlineStringCell("A2", "Dieksport pada: $exportTimestamp  •  Jumlah Ahli: ${members.size}", style = 3))
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
            val birthReadable = formatBirthDateReadable(member.birthDate)
            val values = listOf(
                (idx + 1).toString(),
                member.fullName,
                member.resolvedGender().label,
                birthReadable,
                ageText,
                member.resolvedBaptismStatus().label,
                member.denomination
            )
            rowsXml.append("""<row r="$rowNum">""")
            values.forEachIndexed { colIdx, value ->
                val ref = "${colLetter(colIdx)}$rowNum"
                if (colIdx == 0 || (colIdx == 4 && value.toIntOrNull() != null)) {
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
    <col min="2" max="2" width="32" customWidth="1"/>
    <col min="3" max="3" width="14" customWidth="1"/>
    <col min="4" max="4" width="18" customWidth="1"/>
    <col min="5" max="5" width="15" customWidth="1"/>
    <col min="6" max="6" width="18" customWidth="1"/>
    <col min="7" max="7" width="18" customWidth="1"/>
  </cols>
  <sheetData>
    $rowsXml
  </sheetData>
  <mergeCells count="2">
    <mergeCell ref="A1:G1"/>
    <mergeCell ref="A2:G2"/>
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

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Senarai Ahli" sheetId="1" r:id="rId1"/>
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

        // Column proportions summing to 1.0f
        val colWeights = floatArrayOf(0.05f, 0.28f, 0.11f, 0.16f, 0.08f, 0.16f, 0.16f)
        val colTitles = listOf("No", "Nama Penuh", "Jantina", "Tarikh Lahir", "Umur", "Status Baptisan", "Denominasi")
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

        val exportTimestamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
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
                    canvas.drawText("SENARAI AHLI JEMAAT SIB TOBURON", marginX, currentY + 14f, titlePaint)
                    canvas.drawText(
                        "Dieksport pada: $exportTimestamp   •   Jumlah Ahli Berdaftar: ${members.size}",
                        marginX,
                        currentY + 32f,
                        subtitlePaint
                    )
                    currentY = firstPageStartY
                } else {
                    canvas.drawText("Senarai Ahli Jemaat SIB Toburon (Sambungan)", marginX, currentY + 12f, subtitlePaint)
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

                    val ageStr = member.calculatedAge()?.let { "$it thn" } ?: "-"
                    val rowValues = listOf(
                        seqNo.toString(),
                        member.fullName,
                        member.resolvedGender().label,
                        formatBirthDateReadable(member.birthDate),
                        ageStr,
                        member.resolvedBaptismStatus().label,
                        member.denomination
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
                    "Buletin SIB Toburon • Dokumen Rasmi Pengurusan Admin",
                    marginX,
                    pageHeight - 18f,
                    footerPaint
                )
                val pageLabel = "Halaman ${pageIndex + 1} / ${pages.size}"
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
