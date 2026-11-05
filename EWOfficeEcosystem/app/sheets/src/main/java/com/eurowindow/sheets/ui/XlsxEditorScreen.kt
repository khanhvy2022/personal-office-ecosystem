package com.eurowindow.sheets.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurowindow.sheets.model.CellAlignment
import com.eurowindow.sheets.model.XlsxCell
import com.eurowindow.sheets.model.XlsxWorkbook
import com.eurowindow.sheets.model.XlsxWorksheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XlsxEditorScreen(
    workbook: XlsxWorkbook,
    isModified: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    var activeSheetIndex by remember { mutableIntStateOf(0) }
    var selectedRow by remember { mutableIntStateOf(1) }
    var selectedCol by remember { mutableIntStateOf(1) }

    var showTextColorMenu by remember { mutableStateOf(false) }
    var showFillColorMenu by remember { mutableStateOf(false) }
    var showFunctionMenu by remember { mutableStateOf(false) }

    val primaryGreen = Color(0xFF166534)
    val accentGreen = Color(0xFF26A269)

    val currentSheet = workbook.sheets.getOrNull(activeSheetIndex) ?: XlsxWorksheet()
    val currentCell = currentSheet.getCell(selectedRow, selectedCol)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        BasicTextField(
                            value = workbook.title,
                            onValueChange = { workbook.title = it },
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
                            color = if (isModified) Color(0xFFFFD54F) else Color(0xFFDCFCE7),
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
                    IconButton(onClick = onExportCsv) {
                        Text("CSV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
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
                            contentDescription = "Lưu XLSX",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryGreen
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
                    SheetIconButton(
                        icon = Icons.Default.FormatBold,
                        isActive = currentCell.isBold,
                        onClick = { currentCell.isBold = !currentCell.isBold }
                    )

                    // Italic Toggle
                    SheetIconButton(
                        icon = Icons.Default.FormatItalic,
                        isActive = currentCell.isItalic,
                        onClick = { currentCell.isItalic = !currentCell.isItalic }
                    )

                    // Text Color Dropdown
                    Box {
                        SheetIconButton(
                            icon = Icons.Default.FormatColorText,
                            isActive = currentCell.colorHex != "1E293B",
                            onClick = { showTextColorMenu = true }
                        )
                        DropdownMenu(
                            expanded = showTextColorMenu,
                            onDismissRequest = { showTextColorMenu = false }
                        ) {
                            val colors = listOf(
                                "Màu đen" to "1E293B",
                                "Xanh lá Eurowindow" to "166534",
                                "Xanh dương" to "2563EB",
                                "Màu đỏ" to "DC2626"
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
                                        currentCell.colorHex = hex
                                        showTextColorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Fill Color Dropdown
                    Box {
                        SheetIconButton(
                            icon = Icons.Default.FormatColorFill,
                            isActive = currentCell.fillHex.isNotBlank(),
                            onClick = { showFillColorMenu = true }
                        )
                        DropdownMenu(
                            expanded = showFillColorMenu,
                            onDismissRequest = { showFillColorMenu = false }
                        ) {
                            val fills = listOf(
                                "Không màu" to "",
                                "Xanh nhạt" to "DCFCE7",
                                "Vàng nhạt" to "FEF3C7",
                                "Xám nhạt" to "F1F5F9",
                                "Xanh biển nhạt" to "DBEAFE"
                            )
                            fills.forEach { (label, hex) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (hex.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(android.graphics.Color.parseColor("#$hex")))
                                                )
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Text(label)
                                        }
                                    },
                                    onClick = {
                                        currentCell.fillHex = hex
                                        showFillColorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Alignment
                    SheetIconButton(
                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                        isActive = currentCell.alignment == CellAlignment.LEFT,
                        onClick = { currentCell.alignment = CellAlignment.LEFT }
                    )
                    SheetIconButton(
                        icon = Icons.Default.FormatAlignCenter,
                        isActive = currentCell.alignment == CellAlignment.CENTER,
                        onClick = { currentCell.alignment = CellAlignment.CENTER }
                    )
                    SheetIconButton(
                        icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                        isActive = currentCell.alignment == CellAlignment.RIGHT,
                        onClick = { currentCell.alignment = CellAlignment.RIGHT }
                    )

                    // Add Row
                    SheetIconButton(
                        icon = Icons.Default.TableRows,
                        isActive = false,
                        onClick = {
                            currentSheet.maxRow++
                            selectedRow = currentSheet.maxRow
                        }
                    )

                    // Add Column
                    SheetIconButton(
                        icon = Icons.Default.ViewColumn,
                        isActive = false,
                        onClick = {
                            currentSheet.maxCol++
                            selectedCol = currentSheet.maxCol
                        }
                    )

                    // Quick Formulas (fx)
                    Box {
                        SheetIconButton(
                            icon = Icons.Default.Functions,
                            isActive = currentCell.formula.isNotBlank(),
                            onClick = { showFunctionMenu = true }
                        )
                        DropdownMenu(
                            expanded = showFunctionMenu,
                            onDismissRequest = { showFunctionMenu = false }
                        ) {
                            val funcs = listOf("=SUM(", "=AVERAGE(", "=MIN(", "=MAX(", "=COUNT(")
                            funcs.forEach { fn ->
                                DropdownMenuItem(
                                    text = { Text(fn) },
                                    onClick = {
                                        currentCell.formula = fn
                                        currentCell.value = fn
                                        showFunctionMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            // Formula / Active Cell Indicator Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentCell.cellRef,
                            color = primaryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text("fx", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Spacer(Modifier.width(8.dp))

                    var editVal by remember(currentCell.cellRef, currentCell.value) { mutableStateOf(currentCell.value) }
                    BasicTextField(
                        value = editVal,
                        onValueChange = {
                            editVal = it
                            currentCell.value = it
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF1E293B)
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(accentGreen)
                    )
                }
            }

            // Sheet Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                workbook.sheets.forEachIndexed { sIdx, s ->
                    val isTabActive = sIdx == activeSheetIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTabActive) Color.White else Color(0xFFCBD5E1))
                            .clickable { activeSheetIndex = sIdx }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = s.name,
                            fontWeight = if (isTabActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTabActive) primaryGreen else Color(0xFF475569),
                            fontSize = 12.sp
                        )
                    }
                }

                // Add sheet button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            val newNum = workbook.sheets.size + 1
                            workbook.sheets.add(XlsxWorksheet(name = "Trang tính $newNum"))
                            activeSheetIndex = workbook.sheets.size - 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm trang tính",
                        tint = primaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Interactive Spreadsheet Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val horizontalScroll = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScroll)
                ) {
                    // Column Header Row (Top: A, B, C...)
                    Row(modifier = Modifier.background(Color(0xFFF1F5F9))) {
                        // Empty top-left corner
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 28.dp)
                                .border(0.5.dp, Color(0xFFCBD5E1))
                                .background(Color(0xFFE2E8F0))
                        )
                        for (col in 1..currentSheet.maxCol) {
                            val colLetter = XlsxCell.colToLetter(col)
                            Box(
                                modifier = Modifier
                                    .size(width = 100.dp, height = 28.dp)
                                    .border(0.5.dp, Color(0xFFCBD5E1))
                                    .background(if (col == selectedCol) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = colLetter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (col == selectedCol) primaryGreen else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Rows (1, 2, 3...)
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(currentSheet.maxRow) { rIdx ->
                            val rowNum = rIdx + 1
                            Row {
                                // Row number header on the left
                                Box(
                                    modifier = Modifier
                                        .size(width = 44.dp, height = 34.dp)
                                        .border(0.5.dp, Color(0xFFCBD5E1))
                                        .background(if (rowNum == selectedRow) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$rowNum",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rowNum == selectedRow) primaryGreen else Color(0xFF475569)
                                    )
                                }

                                // Cells in this row
                                for (col in 1..currentSheet.maxCol) {
                                    val cell = currentSheet.getCell(rowNum, col)
                                    val isSelected = rowNum == selectedRow && col == selectedCol

                                    var cellText by remember(cell.cellRef, cell.value) { mutableStateOf(cell.value) }

                                    val cellBg = when {
                                        isSelected -> Color(0xFFDCFCE7)
                                        cell.fillHex.isNotBlank() -> try {
                                            Color(android.graphics.Color.parseColor("#${cell.fillHex.removePrefix("#")}"))
                                        } catch (_: Exception) { Color.White }
                                        else -> Color.White
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(width = 100.dp, height = 34.dp)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) accentGreen else Color(0xFFE2E8F0)
                                            )
                                            .background(cellBg)
                                            .clickable {
                                                selectedRow = rowNum
                                                selectedCol = col
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        contentAlignment = when (cell.alignment) {
                                            CellAlignment.CENTER -> Alignment.Center
                                            CellAlignment.RIGHT -> Alignment.CenterEnd
                                            CellAlignment.LEFT -> Alignment.CenterStart
                                        }
                                    ) {
                                        BasicTextField(
                                            value = cellText,
                                            onValueChange = {
                                                cellText = it
                                                cell.value = it
                                                selectedRow = rowNum
                                                selectedCol = col
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = if (cell.isBold) FontWeight.Bold else FontWeight.Normal,
                                                fontStyle = if (cell.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                textAlign = when (cell.alignment) {
                                                    CellAlignment.CENTER -> TextAlign.Center
                                                    CellAlignment.RIGHT -> TextAlign.Right
                                                    CellAlignment.LEFT -> TextAlign.Left
                                                },
                                                color = try {
                                                    Color(android.graphics.Color.parseColor("#${cell.colorHex.removePrefix("#")}"))
                                                } catch (_: Exception) { Color(0xFF1E293B) }
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(accentGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) Color(0xFFDCFCE7) else Color.Transparent)
            .border(
                width = if (isActive) 1.5.dp else 0.dp,
                color = if (isActive) Color(0xFF166534) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color(0xFF166534) else Color(0xFF475569),
            modifier = Modifier.size(22.dp)
        )
    }
}
