package com.eurowindow.sheets.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.eurowindow.sheets.model.CellAlignment
import com.eurowindow.sheets.model.XlsxCell
import com.eurowindow.sheets.model.XlsxWorkbook
import com.eurowindow.sheets.model.XlsxWorksheet
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pure Kotlin OpenXML XLSX & CSV engine.
 * Requires zero native C++ libraries (0 MB .so footprint).
 * Operates 100% offline with zero external network access.
 */
object XlsxEngine {

    /**
     * Parse XLSX or CSV stream into an XlsxWorkbook model.
     */
    fun parseXlsx(inputStream: InputStream, isCsv: Boolean = false): XlsxWorkbook {
        if (isCsv) {
            return parseCsv(inputStream)
        }

        val entries = mutableMapOf<String, ByteArray>()
        try {
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        entries[entry.name] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
            return XlsxWorkbook.createDefault()
        }

        if (entries.isEmpty() || !entries.containsKey("xl/workbook.xml")) {
            return XlsxWorkbook.createDefault()
        }

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()

        // 1. Parse sharedStrings.xml
        val sharedStrings = mutableListOf<String>()
        entries["xl/sharedStrings.xml"]?.let { sstBytes ->
            try {
                val sstDoc = builder.parse(ByteArrayInputStream(sstBytes))
                val siList = sstDoc.getElementsByTagNameNS("*", "si")
                for (i in 0 until siList.length) {
                    val siNode = siList.item(i)
                    sharedStrings.add(siNode.textContent.trim())
                }
            } catch (_: Exception) {}
        }

        // 2. Parse workbook.xml for sheet names
        val sheetNames = mutableListOf<String>()
        entries["xl/workbook.xml"]?.let { wbBytes ->
            try {
                val wbDoc = builder.parse(ByteArrayInputStream(wbBytes))
                val sList = wbDoc.getElementsByTagNameNS("*", "sheet")
                for (i in 0 until sList.length) {
                    val sElem = sList.item(i) as Element
                    val name = sElem.getAttribute("name")
                    sheetNames.add(name.ifBlank { "Sheet${i + 1}" })
                }
            } catch (_: Exception) {}
        }
        if (sheetNames.isEmpty()) {
            sheetNames.add("Sheet1")
        }

        val workbook = XlsxWorkbook()

        // 3. Parse worksheets (sheet1.xml, sheet2.xml...)
        for ((idx, sName) in sheetNames.withIndex()) {
            val sheetEntryName = "xl/worksheets/sheet${idx + 1}.xml"
            val sheetBytes = entries[sheetEntryName] ?: continue
            val worksheet = XlsxWorksheet(name = sName)

            try {
                val sheetDoc = builder.parse(ByteArrayInputStream(sheetBytes))
                val rowList = sheetDoc.getElementsByTagNameNS("*", "row")

                for (rIdx in 0 until rowList.length) {
                    val rowElem = rowList.item(rIdx) as Element
                    val rNum = rowElem.getAttribute("r").toIntOrNull() ?: (rIdx + 1)
                    val cList = rowElem.getElementsByTagNameNS("*", "c")

                    for (cIdx in 0 until cList.length) {
                        val cElem = cList.item(cIdx) as Element
                        val ref = cElem.getAttribute("r")
                        val cellType = cElem.getAttribute("t")

                        // Extract value
                        val vNode = cElem.getElementsByTagNameNS("*", "v")
                        val rawVal = if (vNode.length > 0) vNode.item(0).textContent.trim() else ""

                        val cellText = if (cellType == "s") {
                            val sstIdx = rawVal.toIntOrNull()
                            if (sstIdx != null && sstIdx in sharedStrings.indices) {
                                sharedStrings[sstIdx]
                            } else rawVal
                        } else {
                            rawVal
                        }

                        // Extract formula
                        val fNode = cElem.getElementsByTagNameNS("*", "f")
                        val formulaText = if (fNode.length > 0) "=${fNode.item(0).textContent.trim()}" else ""

                        // Extract row and col from ref
                        val (row, col) = parseCellRef(ref, rNum, cIdx + 1)
                        val cell = worksheet.getCell(row, col)
                        cell.value = cellText
                        cell.formula = formulaText
                        if (row > worksheet.maxRow) worksheet.maxRow = row
                        if (col > worksheet.maxCol) worksheet.maxCol = col
                    }
                }
            } catch (_: Exception) {}

            workbook.sheets.add(worksheet)
        }

        if (workbook.sheets.isEmpty()) {
            return XlsxWorkbook.createDefault()
        }
        return workbook
    }

    private fun parseCellRef(ref: String, fallbackRow: Int, fallbackCol: Int): Pair<Int, Int> {
        if (ref.isBlank()) return Pair(fallbackRow, fallbackCol)
        val letters = ref.filter { it.isLetter() }
        val digits = ref.filter { it.isDigit() }
        val col = if (letters.isNotEmpty()) XlsxCell.letterToCol(letters) else fallbackCol
        val row = digits.toIntOrNull() ?: fallbackRow
        return Pair(row, col)
    }

    /**
     * Parse standard CSV file.
     */
    private fun parseCsv(inputStream: InputStream): XlsxWorkbook {
        val sheet = XlsxWorksheet(name = "Dữ liệu CSV")
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            var row = 1
            var line: String? = reader.readLine()
            while (line != null) {
                val tokens = line.split(",")
                for ((cIdx, token) in tokens.withIndex()) {
                    sheet.setCellValue(row, cIdx + 1, token.trim().removeSurrounding("\""))
                }
                row++
                line = reader.readLine()
            }
        }
        return XlsxWorkbook(title = "Du_lieu_CSV", sheets = mutableListOf(sheet))
    }

    /**
     * Write workbook to Microsoft OpenXML XLSX archive.
     */
    fun writeXlsx(workbook: XlsxWorkbook, outputStream: OutputStream) {
        val sheets = if (workbook.sheets.isEmpty()) mutableListOf(XlsxWorksheet()) else workbook.sheets

        // Gather all unique strings for sharedStrings.xml
        val stringTable = mutableListOf<String>()
        val stringMap = mutableMapOf<String, Int>()

        for (sheet in sheets) {
            for ((_, cell) in sheet.cells) {
                val v = cell.value
                if (v.isNotBlank() && v.toDoubleOrNull() == null) {
                    if (!stringMap.containsKey(v)) {
                        stringMap[v] = stringTable.size
                        stringTable.add(v)
                    }
                }
            }
        }

        ZipOutputStream(outputStream).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(buildContentTypesXml(sheets.size).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(PACKAGE_RELS_XML.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zos.write(buildWorkbookRelsXml(sheets.size).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            zos.write(buildWorkbookXml(sheets).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 5. xl/sharedStrings.xml
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zos.write(buildSharedStringsXml(stringTable).toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 6. xl/worksheets/sheet*.xml
            for ((idx, sheet) in sheets.withIndex()) {
                zos.putNextEntry(ZipEntry("xl/worksheets/sheet${idx + 1}.xml"))
                zos.write(buildSheetXml(sheet, stringMap).toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
            }
        }
    }

    private fun buildSheetXml(sheet: XlsxWorksheet, stringMap: Map<String, Int>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("<sheetData>")

        // Group cells by row
        val rowMap = mutableMapOf<Int, MutableList<XlsxCell>>()
        for ((_, cell) in sheet.cells) {
            if (cell.value.isNotBlank() || cell.formula.isNotBlank()) {
                rowMap.getOrPut(cell.row) { mutableListOf() }.add(cell)
            }
        }

        val sortedRowKeys = rowMap.keys.sorted()
        for (rNum in sortedRowKeys) {
            sb.append("""<row r="$rNum">""")
            val cellsInRow = rowMap[rNum]!!.sortedBy { it.col }
            for (c in cellsInRow) {
                val ref = c.cellRef
                val v = c.value
                val isNum = v.toDoubleOrNull() != null
                if (c.formula.isNotBlank()) {
                    val formulaClean = c.formula.removePrefix("=")
                    sb.append("""<c r="$ref"><f>${escapeXml(formulaClean)}</f><v>${escapeXml(v)}</v></c>""")
                } else if (isNum) {
                    sb.append("""<c r="$ref"><v>$v</v></c>""")
                } else {
                    val sstIdx = stringMap[v] ?: 0
                    sb.append("""<c r="$ref" t="s"><v>$sstIdx</v></c>""")
                }
            }
            sb.append("</row>")
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun buildSharedStringsXml(stringTable: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${stringTable.size}" uniqueCount="${stringTable.size}">""")
        for (str in stringTable) {
            sb.append("<si><t>${escapeXml(str)}</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun buildWorkbookXml(sheets: List<XlsxWorksheet>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("<sheets>")
        for ((idx, s) in sheets.withIndex()) {
            val sheetId = idx + 1
            sb.append("""<sheet name="${escapeXml(s.name)}" sheetId="$sheetId" r:id="rId$sheetId"/>""")
        }
        sb.append("</sheets></workbook>")
        return sb.toString()
    }

    private fun buildContentTypesXml(sheetCount: Int): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        sb.append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        sb.append("""<Default Extension="xml" ContentType="application/xml"/>""")
        sb.append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        sb.append("""<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>""")
        for (i in 1..sheetCount) {
            sb.append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        sb.append("</Types>")
        return sb.toString()
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            sb.append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        sb.append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>""")
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Export active sheet to CSV.
     */
    fun exportToCsv(sheet: XlsxWorksheet, outputStream: OutputStream) {
        outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            for (r in 1..sheet.maxRow) {
                val rowCells = (1..sheet.maxCol).map { c ->
                    val raw = sheet.getCell(r, c).value
                    if (raw.contains(",") || raw.contains("\"")) {
                        "\"${raw.replace("\"", "\"\"")}\""
                    } else raw
                }
                writer.write(rowCells.joinToString(","))
                writer.newLine()
            }
        }
    }

    /**
     * Export spreadsheet to vector PDF.
     */
    fun exportToPdf(workbook: XlsxWorkbook, outputStream: OutputStream) {
        val pdfDocument = PdfDocument()
        val pageWidth = 842 // A4 Landscape for spreadsheets
        val pageHeight = 595
        val margin = 40f
        val sheet = workbook.sheets.firstOrNull() ?: XlsxWorksheet()

        val maxCol = minOf(12, maxOf(5, sheet.maxCol))
        val maxRow = minOf(40, maxOf(10, sheet.maxRow))

        val colWidth = (pageWidth - (margin * 2)) / maxCol.toFloat()
        val rowHeight = 22f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#166534")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10f
        }

        canvas.drawText("${workbook.title} - ${sheet.name}", margin, margin, headerPaint)
        canvas.drawText("Trang $pageNumber", pageWidth - margin - 40f, margin, subPaint)

        var yPos = margin + 20f

        // Column headers (A, B, C...)
        val colHdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bgHdrPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        for (c in 1..maxCol) {
            val x = margin + ((c - 1) * colWidth)
            canvas.drawRect(x, yPos, x + colWidth, yPos + rowHeight, bgHdrPaint)
            canvas.drawRect(x, yPos, x + colWidth, yPos + rowHeight, borderPaint)
            canvas.drawText(XlsxCell.colToLetter(c), x + (colWidth / 2f) - 4f, yPos + 15f, colHdrPaint)
        }
        yPos += rowHeight

        // Data rows
        for (r in 1..maxRow) {
            if (yPos + rowHeight > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin + 20f
                canvas.drawText("${workbook.title} - ${sheet.name} (tiếp)", margin, margin, headerPaint)
                canvas.drawText("Trang $pageNumber", pageWidth - margin - 40f, margin, subPaint)
            }

            for (c in 1..maxCol) {
                val cell = sheet.getCell(r, c)
                val x = margin + ((c - 1) * colWidth)

                if (cell.fillHex.isNotBlank()) {
                    val fillPaint = Paint().apply {
                        color = Color.parseColor("#${cell.fillHex.removePrefix("#")}")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(x, yPos, x + colWidth, yPos + rowHeight, fillPaint)
                }

                canvas.drawRect(x, yPos, x + colWidth, yPos + rowHeight, borderPaint)

                if (cell.value.isNotBlank()) {
                    cellPaint.typeface = if (cell.isBold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.DEFAULT
                    val text = if (cell.value.length > 18) cell.value.take(16) + ".." else cell.value
                    val isNum = cell.value.toDoubleOrNull() != null
                    val textX = if (isNum || cell.alignment == CellAlignment.RIGHT) {
                        x + colWidth - cellPaint.measureText(text) - 6f
                    } else {
                        x + 6f
                    }
                    canvas.drawText(text, textX, yPos + 15f, cellPaint)
                }
            }
            yPos += rowHeight
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    private const val PACKAGE_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
}
