package com.eurowindow.documents.model

import java.util.UUID

enum class DocxAlignment {
    LEFT, CENTER, RIGHT, JUSTIFY
}

data class DocxParagraph(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    var alignment: DocxAlignment = DocxAlignment.LEFT,
    var headingLevel: Int = 0, // 0 = Body, 1 = H1, 2 = H2, 3 = H3
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    var isUnderline: Boolean = false,
    var isBullet: Boolean = false,
    var fontSizeSp: Float = 16f,
    var colorHex: String = "1E293B",
    var tableData: DocxTable? = null
)

data class DocxTable(
    val rows: MutableList<DocxTableRow> = mutableListOf()
)

data class DocxTableRow(
    val cells: MutableList<String> = mutableListOf()
)

data class DocxDocument(
    var title: String = "Tài liệu Eurowindow",
    val paragraphs: MutableList<DocxParagraph> = mutableListOf()
) {
    companion object {
        fun createDefault(): DocxDocument {
            return DocxDocument(
                title = "Văn bản mới",
                paragraphs = mutableListOf(
                    DocxParagraph(
                        text = "VĂN BẢN EUROWINDOW",
                        alignment = DocxAlignment.CENTER,
                        headingLevel = 1,
                        isBold = true,
                        fontSizeSp = 22f,
                        colorHex = "0F2E5A"
                    ),
                    DocxParagraph(
                        text = "Soạn thảo văn bản cá nhân & bảo mật ngoại tuyến.",
                        alignment = DocxAlignment.CENTER,
                        fontSizeSp = 14f,
                        colorHex = "64748B"
                    ),
                    DocxParagraph(
                        text = "Nhập nội dung văn bản của bạn tại đây...",
                        alignment = DocxAlignment.LEFT,
                        fontSizeSp = 16f
                    )
                )
            )
        }
    }
}
