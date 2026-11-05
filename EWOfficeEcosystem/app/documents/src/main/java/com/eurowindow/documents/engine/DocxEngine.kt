package com.eurowindow.documents.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.eurowindow.documents.model.DocxAlignment
import com.eurowindow.documents.model.DocxDocument
import com.eurowindow.documents.model.DocxParagraph
import com.eurowindow.documents.model.DocxTable
import com.eurowindow.documents.model.DocxTableRow
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * High-performance, lightweight pure Kotlin OpenXML DOCX & PDF engine.
 * Uses standard java.xml DOM parsing supported universally on both Android & JVM.
 * Requires zero native C++ libraries (0 MB .so footprint).
 * Operates 100% offline with zero dependencies on cloud or external services.
 */
object DocxEngine {

    /**
     * Parse any DOCX file from an input stream into our document model.
     */
    fun parseDocx(inputStream: InputStream): DocxDocument {
        val document = DocxDocument()
        var documentXmlBytes: ByteArray? = null

        // 1. Read ZIP archive and extract word/document.xml
        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    documentXmlBytes = zis.readBytes()
                    break
                }
                entry = zis.nextEntry
            }
        }

        if (documentXmlBytes == null) {
            return DocxDocument.createDefault()
        }

        // 2. Parse word/document.xml using standard DOM parser
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val domDoc = builder.parse(ByteArrayInputStream(documentXmlBytes))
            domDoc.documentElement.normalize()

            // Find w:body element
            val bodyList = domDoc.getElementsByTagNameNS("*", "body")
            val bodyNode = if (bodyList.length > 0) bodyList.item(0) else domDoc.documentElement

            val childNodes = bodyNode.childNodes
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i)
                if (node.nodeType != Node.ELEMENT_NODE) continue
                val elemName = node.localName ?: node.nodeName

                if (elemName.endsWith("tbl")) {
                    // Table element
                    val table = parseTableElement(node as Element)
                    document.paragraphs.add(DocxParagraph(tableData = table))
                } else if (elemName.endsWith("p")) {
                    // Paragraph element
                    val paragraph = parseParagraphElement(node as Element)
                    if (paragraph.text.isNotBlank() || document.paragraphs.isEmpty()) {
                        document.paragraphs.add(paragraph)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }

        if (document.paragraphs.isEmpty()) {
            return DocxDocument.createDefault()
        }
        return document
    }

    private fun parseParagraphElement(pElem: Element): DocxParagraph {
        val paragraph = DocxParagraph()
        val textBuilder = StringBuilder()

        val childNodes = pElem.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val elem = node as Element
            val localName = elem.localName ?: elem.nodeName

            if (localName.endsWith("pPr")) {
                // Paragraph properties
                parseParagraphProperties(elem, paragraph)
            } else if (localName.endsWith("r")) {
                // Run element
                parseRunElement(elem, paragraph, textBuilder)
            }
        }
        paragraph.text = textBuilder.toString()
        return paragraph
    }

    private fun parseParagraphProperties(pPrElem: Element, paragraph: DocxParagraph) {
        val childNodes = pPrElem.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val elem = node as Element
            val localName = elem.localName ?: elem.nodeName

            if (localName.endsWith("jc")) {
                val jcVal = elem.getAttribute("w:val").ifEmpty { elem.getAttribute("val") }
                paragraph.alignment = when (jcVal) {
                    "center" -> DocxAlignment.CENTER
                    "right" -> DocxAlignment.RIGHT
                    "both" -> DocxAlignment.JUSTIFY
                    else -> DocxAlignment.LEFT
                }
            }
        }
    }

    private fun parseRunElement(rElem: Element, paragraph: DocxParagraph, textBuilder: StringBuilder) {
        val childNodes = rElem.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val elem = node as Element
            val localName = elem.localName ?: elem.nodeName

            if (localName.endsWith("rPr")) {
                val rPrChildren = elem.childNodes
                for (j in 0 until rPrChildren.length) {
                    val rChild = rPrChildren.item(j)
                    if (rChild.nodeType != Node.ELEMENT_NODE) continue
                    val rElemChild = rChild as Element
                    val propName = rElemChild.localName ?: rElemChild.nodeName

                    if (propName.endsWith("b")) {
                        paragraph.isBold = true
                    } else if (propName.endsWith("i")) {
                        paragraph.isItalic = true
                    } else if (propName.endsWith("u")) {
                        paragraph.isUnderline = true
                    } else if (propName.endsWith("sz")) {
                        val szVal = (rElemChild.getAttribute("w:val").ifEmpty { rElemChild.getAttribute("val") }).toIntOrNull()
                        if (szVal != null && szVal > 0) {
                            paragraph.fontSizeSp = szVal / 2f
                            if (szVal >= 36) paragraph.headingLevel = 1
                            else if (szVal >= 28) paragraph.headingLevel = 2
                        }
                    } else if (propName.endsWith("color")) {
                        val colVal = rElemChild.getAttribute("w:val").ifEmpty { rElemChild.getAttribute("val") }
                        if (colVal.isNotBlank() && colVal != "auto") {
                            paragraph.colorHex = colVal
                        }
                    }
                }
            } else if (localName.endsWith("t")) {
                textBuilder.append(elem.textContent)
            }
        }
    }

    private fun parseTableElement(tblElem: Element): DocxTable {
        val table = DocxTable()
        val childNodes = tblElem.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val elem = node as Element
            val localName = elem.localName ?: elem.nodeName

            if (localName.endsWith("tr")) {
                val row = DocxTableRow()
                val trChildren = elem.childNodes
                for (j in 0 until trChildren.length) {
                    val tcNode = trChildren.item(j)
                    if (tcNode.nodeType != Node.ELEMENT_NODE) continue
                    val tcElem = tcNode as Element
                    val tcLocalName = tcElem.localName ?: tcElem.nodeName

                    if (tcLocalName.endsWith("tc")) {
                        val cellText = tcElem.textContent.trim()
                        row.cells.add(cellText)
                    }
                }
                table.rows.add(row)
            }
        }
        return table
    }

    /**
     * Write document model back to a valid Microsoft OpenXML DOCX archive.
     */
    fun writeDocx(document: DocxDocument, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(CONTENT_TYPES_XML.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(PACKAGE_RELS_XML.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 3. word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(DOCUMENT_RELS_XML.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()

            // 4. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            val docXml = buildDocumentXml(document)
            zos.write(docXml.toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun buildDocumentXml(document: DocxDocument): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        for (p in document.paragraphs) {
            if (p.tableData != null) {
                // Table
                sb.append("<w:tbl>")
                sb.append("<w:tblPr><w:tblBorders>")
                sb.append("""<w:top w:val="single" w:sz="4" w:space="0" w:color="CCCCCC"/>""")
                sb.append("""<w:left w:val="single" w:sz="4" w:space="0" w:color="CCCCCC"/>""")
                sb.append("""<w:bottom w:val="single" w:sz="4" w:space="0" w:color="CCCCCC"/>""")
                sb.append("""<w:right w:val="single" w:sz="4" w:space="0" w:color="CCCCCC"/>""")
                sb.append("""<w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>""")
                sb.append("""<w:insideV w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>""")
                sb.append("</w:tblBorders></w:tblPr>")

                for (row in p.tableData!!.rows) {
                    sb.append("<w:tr>")
                    for (cell in row.cells) {
                        sb.append("<w:tc>")
                        sb.append("<w:p>")
                        sb.append("<w:r>")
                        sb.append("<w:t>${escapeXml(cell)}</w:t>")
                        sb.append("</w:r>")
                        sb.append("</w:p>")
                        sb.append("</w:tc>")
                    }
                    sb.append("</w:tr>")
                }
                sb.append("</w:tbl>")
            } else {
                // Paragraph
                sb.append("<w:p>")
                sb.append("<w:pPr>")
                when (p.alignment) {
                    DocxAlignment.CENTER -> sb.append("""<w:jc w:val="center"/>""")
                    DocxAlignment.RIGHT -> sb.append("""<w:jc w:val="right"/>""")
                    DocxAlignment.JUSTIFY -> sb.append("""<w:jc w:val="both"/>""")
                    DocxAlignment.LEFT -> sb.append("""<w:jc w:val="left"/>""")
                }
                sb.append("</w:pPr>")

                sb.append("<w:r>")
                sb.append("<w:rPr>")
                if (p.isBold) sb.append("<w:b/>")
                if (p.isItalic) sb.append("<w:i/>")
                if (p.isUnderline) sb.append("""<w:u w:val="single"/>""")
                val halfPoints = (p.fontSizeSp * 2).toInt()
                sb.append("""<w:sz w:val="$halfPoints"/>""")
                if (p.colorHex.isNotBlank()) {
                    sb.append("""<w:color w:val="${p.colorHex}"/>""")
                }
                sb.append("</w:rPr>")
                sb.append("""<w:t xml:space="preserve">${escapeXml(p.text)}</w:t>""")
                sb.append("</w:r>")
                sb.append("</w:p>")
            }
        }

        // Section properties (A4 margins)
        sb.append("<w:sectPr>")
        sb.append("""<w:pgSz w:w="11906" w:h="16838"/>""")
        sb.append("""<w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1417"/>""")
        sb.append("</w:sectPr>")
        sb.append("</w:body></w:document>")

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
     * Export document model to crisp, vector PDF using Android's native PdfDocument.
     */
    fun exportToPdf(document: DocxDocument, outputStream: OutputStream) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 points (72 DPI)
        val pageHeight = 842
        val margin = 50f
        val contentWidth = pageWidth - (margin * 2)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = margin + 20f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 12f
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
        }

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            c.drawText(document.title, margin, margin - 15f, headerPaint)
            c.drawText("Trang $pNum", pageWidth - margin - 40f, pageHeight - margin + 20f, headerPaint)
            c.drawLine(margin, margin - 5f, pageWidth - margin, margin - 5f, headerPaint)
        }

        drawHeaderAndFooter(canvas, pageNumber)

        for (p in document.paragraphs) {
            if (p.tableData != null) {
                // Draw table
                val tbl = p.tableData!!
                if (tbl.rows.isNotEmpty()) {
                    val colCount = tbl.rows.maxOfOrNull { it.cells.size } ?: 1
                    val colWidth = contentWidth / colCount.toFloat()
                    val rowHeight = 26f

                    if (yPosition + (tbl.rows.size * rowHeight) > pageHeight - margin) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = margin + 20f
                        drawHeaderAndFooter(canvas, pageNumber)
                    }

                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#CBD5E1")
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }
                    val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#1E293B")
                        textSize = 10f
                    }

                    for (r in tbl.rows) {
                        for ((cIdx, cellText) in r.cells.withIndex()) {
                            val x = margin + (cIdx * colWidth)
                            canvas.drawRect(x, yPosition, x + colWidth, yPosition + rowHeight, borderPaint)
                            val displayText = if (cellText.length > 20) cellText.take(18) + "..." else cellText
                            canvas.drawText(displayText, x + 6f, yPosition + 17f, cellTextPaint)
                        }
                        yPosition += rowHeight
                    }
                    yPosition += 10f
                }
            } else {
                // Draw paragraph
                val style = when {
                    p.isBold && p.isItalic -> Typeface.BOLD_ITALIC
                    p.isBold -> Typeface.BOLD
                    p.isItalic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, style)
                paint.textSize = when (p.headingLevel) {
                    1 -> 18f
                    2 -> 15f
                    else -> 11f
                }
                try {
                    val colorInt = Color.parseColor("#${p.colorHex.removePrefix("#")}")
                    paint.color = colorInt
                } catch (_: Exception) {
                    paint.color = Color.parseColor("#1E293B")
                }

                // Line wrapping
                val words = p.text.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = StringBuilder()

                for (w in words) {
                    val testLine = if (currentLine.isEmpty()) w else "$currentLine $w"
                    if (paint.measureText(testLine) <= contentWidth) {
                        currentLine.append(if (currentLine.isEmpty()) w else " $w")
                    } else {
                        lines.add(currentLine.toString())
                        currentLine = StringBuilder(w)
                    }
                }
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }

                val lineHeight = paint.textSize * 1.4f
                if (yPosition + (lines.size * lineHeight) > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = margin + 20f
                    drawHeaderAndFooter(canvas, pageNumber)
                }

                for (line in lines) {
                    var x = margin
                    if (p.alignment == DocxAlignment.CENTER) {
                        val lineWidth = paint.measureText(line)
                        x = margin + (contentWidth - lineWidth) / 2f
                    } else if (p.alignment == DocxAlignment.RIGHT) {
                        val lineWidth = paint.measureText(line)
                        x = pageWidth - margin - lineWidth
                    }
                    canvas.drawText(line, x, yPosition, paint)
                    yPosition += lineHeight
                }
                yPosition += 6f // Paragraph spacing
            }
        }

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    private const val PACKAGE_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private const val DOCUMENT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>"""
}
