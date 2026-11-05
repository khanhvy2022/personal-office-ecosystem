package com.eurowindow.documents.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eurowindow.office.core.database.RecentDocumentRecord
import java.text.SimpleDateFormat
import java.util.*

val PrimaryNavy = Color(0xFF1E3A8A)
val AccentBlue = Color(0xFF2563EB)
val SurfaceBg = Color(0xFFF8FAFC)
val CardBorder = Color(0xFFE2E8F0)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF94A3B8)
val DangerRed = Color(0xFFDC2626)
val SuccessGreen = Color(0xFF16A34A)

enum class ScreenTab {
    DOCUMENTS,
    SETTINGS
}

data class SampleCorpusDoc(
    val title: String,
    val fileName: String,
    val description: String,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsMainScreen(
    recentDocuments: List<RecentDocumentRecord>,
    sampleDocuments: List<SampleCorpusDoc>,
    isSpellcheckDisabled: Boolean,
    onSpellcheckToggled: (Boolean) -> Unit,
    onOpenSaf: () -> Unit,
    onCreateSaf: () -> Unit,
    onOpenRecent: (RecentDocumentRecord) -> Unit,
    onDeleteRecent: (RecentDocumentRecord) -> Unit,
    onOpenSample: (SampleCorpusDoc) -> Unit,
    onClearCache: () -> Unit,
    statusMessage: String
) {
    var selectedTab by remember { mutableStateOf(ScreenTab.DOCUMENTS) }
    var selectedRecentForAction by remember { mutableStateOf<RecentDocumentRecord?>(null) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryNavy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "W",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "EW Documents",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "Standalone DOCX Reader & Editor · Offline-First",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == ScreenTab.DOCUMENTS,
                        onClick = { selectedTab = ScreenTab.DOCUMENTS },
                        label = { Text("Tài liệu") },
                        icon = { Text(if (selectedTab == ScreenTab.DOCUMENTS) "📁" else "📄") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = PrimaryNavy,
                            indicatorColor = AccentBlue.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == ScreenTab.SETTINGS,
                        onClick = { selectedTab = ScreenTab.SETTINGS },
                        label = { Text("Cài đặt") },
                        icon = { Text(if (selectedTab == ScreenTab.SETTINGS) "⚙️" else "🔧") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = PrimaryNavy,
                            indicatorColor = AccentBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == ScreenTab.DOCUMENTS) {
                    FloatingActionButton(
                        onClick = onCreateSaf,
                        containerColor = PrimaryNavy,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("➕", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tạo mới", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceBg)
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    ScreenTab.DOCUMENTS -> {
                        DocumentsListContent(
                            recentDocuments = recentDocuments,
                            sampleDocuments = sampleDocuments,
                            statusMessage = statusMessage,
                            onOpenSaf = onOpenSaf,
                            onCreateSaf = onCreateSaf,
                            onOpenRecent = onOpenRecent,
                            onRecentActionMenu = { selectedRecentForAction = it },
                            onOpenSample = onOpenSample
                        )
                    }
                    ScreenTab.SETTINGS -> {
                        DocumentsSettingsContent(
                            isSpellcheckDisabled = isSpellcheckDisabled,
                            onSpellcheckToggled = onSpellcheckToggled,
                            onClearCache = onClearCache
                        )
                    }
                }

                // Action Menu BottomSheet / Dialog
                selectedRecentForAction?.let { record ->
                    AlertDialog(
                        onDismissRequest = { selectedRecentForAction = null },
                        title = {
                            Text(
                                text = record.displayName,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Đường dẫn URI: ${record.uriString}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Mở lần cuối: ${formatTimestamp(record.lastOpenedEpochMs)}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val target = record
                                    selectedRecentForAction = null
                                    onOpenRecent(target)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                            ) {
                                Text("Mở tài liệu")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = {
                                    val target = record
                                    selectedRecentForAction = null
                                    onDeleteRecent(target)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                            ) {
                                Text("Xóa khỏi gần đây")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentsListContent(
    recentDocuments: List<RecentDocumentRecord>,
    sampleDocuments: List<SampleCorpusDoc>,
    statusMessage: String,
    onOpenSaf: () -> Unit,
    onCreateSaf: () -> Unit,
    onOpenRecent: (RecentDocumentRecord) -> Unit,
    onRecentActionMenu: (RecentDocumentRecord) -> Unit,
    onOpenSample: (SampleCorpusDoc) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Status Bar Card
        if (statusMessage.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ℹ️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryNavy
                        )
                    }
                }
            }
        }

        // Quick Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOpenSaf,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("📂  Mở tệp (SAF)")
                }
                OutlinedButton(
                    onClick = onCreateSaf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("📝  Tạo DOCX")
                }
            }
        }

        // Section: Recent Documents
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tài liệu gần đây",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = "${recentDocuments.size} tệp",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        if (recentDocuments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📄", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa có tài liệu gần đây",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mở tệp DOCX từ bộ nhớ thiết bị hoặc tạo văn bản mới để bắt đầu.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentDocuments, key = { it.uriString }) { doc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRecent(doc) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryNavy.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DOCX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.displayName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatTimestamp(doc.lastOpenedEpochMs),
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        IconButton(onClick = { onRecentActionMenu(doc) }) {
                            Text("⋮", fontSize = 18.sp, color = TextMuted)
                        }
                    }
                }
            }
        }

        // Section: Standard Eurowindow Test Documents
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tài liệu mẫu kiểm định (Corpus)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
        }

        items(sampleDocuments, key = { it.fileName }) { sample ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSample(sample) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧪", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sample.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = PrimaryNavy.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = sample.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sample.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentsSettingsContent(
    isSpellcheckDisabled: Boolean,
    onSpellcheckToggled: (Boolean) -> Unit,
    onClearCache: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cài đặt ứng dụng",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TextPrimary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Bảo mật & Ngoại tuyến",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PrimaryNavy
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tắt kiểm tra chính tả từ xa",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Không tải từ điển ngoại vi, tối ưu hóa bộ nhớ và tốc độ xử lý.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = isSpellcheckDisabled,
                        onCheckedChange = onSpellcheckToggled
                    )
                }

                HorizontalDivider(color = CardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dọn dẹp vùng nhớ đệm (Cache)",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Xóa toàn bộ tệp làm việc tạm thời và tệp PDF trung gian an toàn.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    OutlinedButton(onClick = onClearCache) {
                        Text("Dọn dẹp", fontSize = 12.sp)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Thông tin phiên bản",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = PrimaryNavy
                )
                Text("EW Documents: v0.2.0-p2-prototype", fontSize = 12.sp, color = TextPrimary)
                Text("Bộ máy: Collabora LibreOfficeKit ARM64 (v26.04.4.2)", fontSize = 12.sp, color = TextSecondary)
                Text("Chế độ mạng: Không kết nối Internet (100% Offline-first)", fontSize = 12.sp, color = SuccessGreen)
                Text("Giấy phép bộ máy: Mozilla Public License v2.0 (MPL-2.0)", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

fun formatTimestamp(epochMs: Long): String {
    if (epochMs <= 0) return "Chưa mở"
    val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
