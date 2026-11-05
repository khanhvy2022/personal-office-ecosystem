package com.eurowindow.documents.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurowindow.documents.model.DocxAlignment
import com.eurowindow.documents.model.DocxDocument
import com.eurowindow.documents.model.DocxParagraph
import com.eurowindow.documents.model.DocxTable
import com.eurowindow.documents.model.DocxTableRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocxEditorScreen(
    document: DocxDocument,
    isModified: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit
) {
    var selectedParagraphIndex by remember { mutableIntStateOf(0) }
    var showColorMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val primaryNavy = Color(0xFF0F2E5A)
    val accentBlue = Color(0xFF2260B4)

    val currentParagraph = document.paragraphs.getOrNull(selectedParagraphIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        BasicTextField(
                            value = document.title,
                            onValueChange = { document.title = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(Color.White)
                        )
                        Text(
                            text = if (isModified) "Đã chỉnh sửa • Chưa lưu" else "Đã lưu • Ngoại tuyến",
                            color = if (isModified) Color(0xFFFFD54F) else Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onExportPdf) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Xuất PDF",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onSave) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Lưu DOCX",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryNavy
                )
            )
        },
        bottomBar = {
            // Touch-first Bottom Formatting Bar (Thanh công cụ dưới đáy ngón tay)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                color = Color.White,
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bold Toggle
                    FormatIconButton(
                        icon = Icons.Default.FormatBold,
                        isActive = currentParagraph?.isBold == true,
                        onClick = {
                            currentParagraph?.let { it.isBold = !it.isBold }
                        }
                    )

                    // Italic Toggle
                    FormatIconButton(
                        icon = Icons.Default.FormatItalic,
                        isActive = currentParagraph?.isItalic == true,
                        onClick = {
                            currentParagraph?.let { it.isItalic = !it.isItalic }
                        }
                    )

                    // Underline Toggle
                    FormatIconButton(
                        icon = Icons.Default.FormatUnderlined,
                        isActive = currentParagraph?.isUnderline == true,
                        onClick = {
                            currentParagraph?.let { it.isUnderline = !it.isUnderline }
                        }
                    )

                    // Size / Heading Menu
                    Box {
                        FormatIconButton(
                            icon = Icons.Default.FormatSize,
                            isActive = currentParagraph?.headingLevel != 0,
                            onClick = { showSizeMenu = true }
                        )
                        DropdownMenu(
                            expanded = showSizeMenu,
                            onDismissRequest = { showSizeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tiêu đề 1 (Lớn)") },
                                onClick = {
                                    currentParagraph?.let {
                                        it.headingLevel = 1
                                        it.fontSizeSp = 22f
                                        it.isBold = true
                                    }
                                    showSizeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tiêu đề 2 (Vừa)") },
                                onClick = {
                                    currentParagraph?.let {
                                        it.headingLevel = 2
                                        it.fontSizeSp = 18f
                                        it.isBold = true
                                    }
                                    showSizeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Đoạn văn thường (16pt)") },
                                onClick = {
                                    currentParagraph?.let {
                                        it.headingLevel = 0
                                        it.fontSizeSp = 16f
                                    }
                                    showSizeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Chữ nhỏ (13pt)") },
                                onClick = {
                                    currentParagraph?.let {
                                        it.headingLevel = 0
                                        it.fontSizeSp = 13f
                                    }
                                    showSizeMenu = false
                                }
                            )
                        }
                    }

                    // Text Color Menu
                    Box {
                        FormatIconButton(
                            icon = Icons.Default.FormatColorText,
                            isActive = currentParagraph?.colorHex != "1E293B",
                            onClick = { showColorMenu = true }
                        )
                        DropdownMenu(
                            expanded = showColorMenu,
                            onDismissRequest = { showColorMenu = false }
                        ) {
                            val colors = listOf(
                                "Màu đen" to "1E293B",
                                "Xanh Eurowindow" to "0F2E5A",
                                "Xanh dương" to "2563EB",
                                "Màu đỏ" to "DC2626",
                                "Xanh lá" to "16A34A"
                            )
                            colors.forEach { (label, hex) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor("#$hex")))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(label)
                                        }
                                    },
                                    onClick = {
                                        currentParagraph?.colorHex = hex
                                        showColorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Alignment Controls
                    FormatIconButton(
                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                        isActive = currentParagraph?.alignment == DocxAlignment.LEFT,
                        onClick = { currentParagraph?.alignment = DocxAlignment.LEFT }
                    )
                    FormatIconButton(
                        icon = Icons.Default.FormatAlignCenter,
                        isActive = currentParagraph?.alignment == DocxAlignment.CENTER,
                        onClick = { currentParagraph?.alignment = DocxAlignment.CENTER }
                    )
                    FormatIconButton(
                        icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                        isActive = currentParagraph?.alignment == DocxAlignment.RIGHT,
                        onClick = { currentParagraph?.alignment = DocxAlignment.RIGHT }
                    )
                    FormatIconButton(
                        icon = Icons.Default.FormatAlignJustify,
                        isActive = currentParagraph?.alignment == DocxAlignment.JUSTIFY,
                        onClick = { currentParagraph?.alignment = DocxAlignment.JUSTIFY }
                    )

                    // Insert Table
                    FormatIconButton(
                        icon = Icons.Default.TableChart,
                        isActive = false,
                        onClick = {
                            val newTable = DocxTable(
                                rows = mutableListOf(
                                    DocxTableRow(mutableListOf("STT", "Hạng mục", "Ghi chú")),
                                    DocxTableRow(mutableListOf("1", "Cửa nhôm Eurowindow", "Đạt chuẩn")),
                                    DocxTableRow(mutableListOf("2", "Phụ kiện đồng bộ", "Chính hãng"))
                                )
                            )
                            document.paragraphs.add(
                                selectedParagraphIndex + 1,
                                DocxParagraph(tableData = newTable)
                            )
                            selectedParagraphIndex++
                        }
                    )

                    // Add Paragraph below
                    FormatIconButton(
                        icon = Icons.Default.Add,
                        isActive = false,
                        onClick = {
                            val newP = DocxParagraph(text = "")
                            val insertIdx = if (selectedParagraphIndex in document.paragraphs.indices) {
                                selectedParagraphIndex + 1
                            } else {
                                document.paragraphs.size
                            }
                            document.paragraphs.add(insertIdx, newP)
                            selectedParagraphIndex = insertIdx
                        }
                    )
                }
            }
        }
    ) { padding ->
        // Document Canvas (Giấy soạn thảo A4 trắng trên nền xám nhạt)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F3F5))
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(document.paragraphs, key = { _, p -> p.id }) { index, p ->
                        if (p.tableData != null) {
                            // Render Editable Table
                            DocxTableView(
                                table = p.tableData!!,
                                isSelected = selectedParagraphIndex == index,
                                onClick = { selectedParagraphIndex = index },
                                onDelete = {
                                    document.paragraphs.removeAt(index)
                                    if (selectedParagraphIndex >= document.paragraphs.size) {
                                        selectedParagraphIndex = maxOf(0, document.paragraphs.size - 1)
                                    }
                                }
                            )
                        } else {
                            // Render Editable Paragraph
                            DocxParagraphView(
                                paragraph = p,
                                isSelected = selectedParagraphIndex == index,
                                onFocus = { selectedParagraphIndex = index },
                                onEnterPressed = {
                                    val newP = DocxParagraph(
                                        alignment = p.alignment,
                                        fontSizeSp = 16f,
                                        headingLevel = 0
                                    )
                                    document.paragraphs.add(index + 1, newP)
                                    selectedParagraphIndex = index + 1
                                },
                                onDelete = {
                                    if (document.paragraphs.size > 1) {
                                        document.paragraphs.removeAt(index)
                                        selectedParagraphIndex = maxOf(0, index - 1)
                                    }
                                }
                            )
                        }
                    }

                    // Spacer at bottom so content is never hidden behind floating bottom bar
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DocxParagraphView(
    paragraph: DocxParagraph,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onEnterPressed: () -> Unit,
    onDelete: () -> Unit
) {
    var textVal by remember(paragraph.text) { mutableStateOf(paragraph.text) }

    val align = when (paragraph.alignment) {
        DocxAlignment.CENTER -> TextAlign.Center
        DocxAlignment.RIGHT -> TextAlign.Right
        DocxAlignment.JUSTIFY -> TextAlign.Justify
        DocxAlignment.LEFT -> TextAlign.Left
    }

    val style = TextStyle(
        fontSize = paragraph.fontSizeSp.sp,
        fontWeight = if (paragraph.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (paragraph.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (paragraph.isUnderline) TextDecoration.Underline else TextDecoration.None,
        textAlign = align,
        color = try {
            Color(android.graphics.Color.parseColor("#${paragraph.colorHex.removePrefix("#")}"))
        } catch (_: Exception) {
            Color(0xFF1E293B)
        },
        lineHeight = (paragraph.fontSizeSp * 1.4f).sp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        BasicTextField(
            value = textVal,
            onValueChange = {
                textVal = it
                paragraph.text = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) onFocus() },
            textStyle = style,
            cursorBrush = SolidColor(Color(0xFF2260B4)),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Default
            ),
            decorationBox = { innerTextField ->
                if (textVal.isEmpty()) {
                    Text(
                        text = "Nhập văn bản tại đây...",
                        style = style.copy(color = Color(0xFF94A3B8))
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun DocxTableView(
    table: DocxTable,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(
                1.dp,
                if (isSelected) Color(0xFF2260B4) else Color(0xFFCBD5E1),
                RoundedCornerShape(6.dp)
            )
            .background(if (isSelected) Color(0xFFF8FAFC) else Color.White)
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bảng dữ liệu (${table.rows.size} hàng)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa bảng",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        table.rows.forEachIndexed { rIdx, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (rIdx == 0) Color(0xFFF1F5F9) else Color.White)
                    .border(0.5.dp, Color(0xFFE2E8F0)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.cells.forEachIndexed { cIdx, cellText ->
                    var cellVal by remember(cellText) { mutableStateOf(cellText) }
                    BasicTextField(
                        value = cellVal,
                        onValueChange = {
                            cellVal = it
                            row.cells[cIdx] = it
                        },
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, Color(0xFFE2E8F0))
                            .padding(6.dp),
                        textStyle = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = if (rIdx == 0) FontWeight.Bold else FontWeight.Normal,
                            color = Color(0xFF1E293B)
                        ),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
fun FormatIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFFE0E7FF) else Color.Transparent)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) Color(0xFF2260B4) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color(0xFF0F2E5A) else Color(0xFF475569),
            modifier = Modifier.size(22.dp)
        )
    }
}
