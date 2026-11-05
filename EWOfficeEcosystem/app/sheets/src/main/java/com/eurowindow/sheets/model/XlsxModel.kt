package com.eurowindow.sheets.model

import java.util.UUID

enum class CellAlignment {
    LEFT, CENTER, RIGHT
}

data class XlsxCell(
    val row: Int, // 1-based (1, 2, 3...)
    val col: Int, // 1-based (1 = A, 2 = B, 3 = C...)
    var value: String = "",
    var formula: String = "",
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var alignment: CellAlignment = CellAlignment.LEFT,
    var colorHex: String = "1E293B",
    var fillHex: String = "" // Empty = transparent
) {
    val cellRef: String
        get() = "${colToLetter(col)}$row"

    companion object {
        fun colToLetter(col: Int): String {
            var c = col
            val sb = StringBuilder()
            while (c > 0) {
                val rem = (c - 1) % 26
                sb.append(('A'.code + rem).toChar())
                c = (c - 1) / 26
            }
            return sb.reverse().toString()
        }

        fun letterToCol(letters: String): Int {
            var col = 0
            for (ch in letters.uppercase()) {
                col = col * 26 + (ch.code - 'A'.code + 1)
            }
            return col
        }
    }
}

data class XlsxWorksheet(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Trang tính 1",
    val cells: MutableMap<String, XlsxCell> = mutableMapOf(),
    var maxRow: Int = 30,
    var maxCol: Int = 10
) {
    fun getCell(row: Int, col: Int): XlsxCell {
        val ref = "${XlsxCell.colToLetter(col)}$row"
        return cells.getOrPut(ref) {
            XlsxCell(row, col)
        }
    }

    fun setCellValue(row: Int, col: Int, value: String) {
        val cell = getCell(row, col)
        cell.value = value
        if (row > maxRow) maxRow = row
        if (col > maxCol) maxCol = col
    }
}

data class XlsxWorkbook(
    var title: String = "Bảng tính Eurowindow",
    val sheets: MutableList<XlsxWorksheet> = mutableListOf()
) {
    companion object {
        fun createDefault(): XlsxWorkbook {
            val sheet = XlsxWorksheet(name = "Báo giá nhôm kính")
            // Populate sample data
            sheet.setCellValue(1, 1, "BÁO GIÁ CỬA NHÔM KÍNH EUROWINDOW")
            sheet.getCell(1, 1).isBold = true
            sheet.getCell(1, 1).colorHex = "166534"

            sheet.setCellValue(2, 1, "Bảng dự toán công trình năm 2026 - Ngoại tuyến")
            sheet.getCell(2, 1).isItalic = true

            // Headers
            val headers = listOf("STT", "Hạng mục sản phẩm", "Kích thước (mm)", "Số lượng", "Đơn giá (VNĐ)", "Thành tiền (VNĐ)")
            for ((idx, h) in headers.withIndex()) {
                val col = idx + 1
                sheet.setCellValue(4, col, h)
                val cell = sheet.getCell(4, col)
                cell.isBold = true
                cell.fillHex = "DCFCE7" // light green
                cell.alignment = if (col in 4..6) CellAlignment.RIGHT else CellAlignment.LEFT
            }

            // Row 1
            sheet.setCellValue(5, 1, "1")
            sheet.setCellValue(5, 2, "Cửa đi mở quay 2 cánh EA55")
            sheet.setCellValue(5, 3, "1800 x 2400")
            sheet.setCellValue(5, 4, "2")
            sheet.setCellValue(5, 5, "8500000")
            sheet.setCellValue(5, 6, "17000000")

            // Row 2
            sheet.setCellValue(6, 1, "2")
            sheet.setCellValue(6, 2, "Cửa sổ mở hất EA55")
            sheet.setCellValue(6, 3, "1200 x 1400")
            sheet.setCellValue(6, 4, "4")
            sheet.setCellValue(6, 5, "4200000")
            sheet.setCellValue(6, 6, "16800000")

            // Row 3
            sheet.setCellValue(7, 1, "3")
            sheet.setCellValue(7, 2, "Kính hộp 2 lớp cường lực")
            sheet.setCellValue(7, 3, "Tiêu chuẩn")
            sheet.setCellValue(7, 4, "1")
            sheet.setCellValue(7, 5, "5500000")
            sheet.setCellValue(7, 6, "5500000")

            // Total
            sheet.setCellValue(9, 2, "TỔNG CỘNG:")
            sheet.getCell(9, 2).isBold = true
            sheet.setCellValue(9, 6, "39300000")
            sheet.getCell(9, 6).isBold = true
            sheet.getCell(9, 6).colorHex = "166534"

            return XlsxWorkbook(
                title = "Bang_tinh_Eurowindow",
                sheets = mutableListOf(sheet, XlsxWorksheet(name = "Vật tư phụ"))
            )
        }
    }
}
