package com.eurowindow.office.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CorpusItem(
    val title: String,
    val assetName: String,
    val description: String
)

/**
 * Minimal P1 Prototype Shell (strictly scoped to P1 Engine Packaging, Security Hardening & Device Validation).
 * Does NOT implement P2/P3 production UI.
 */
@Composable
fun EWPrototypeValidationScreen(
    appTitle: String,
    appSubtitle: String,
    accentColor: Color,
    applicationId: String,
    fileProviderAuthority: String,
    dataDirPath: String,
    cacheDirPath: String,
    statusMessage: String,
    corpusItems: List<CorpusItem>,
    onOpenSafDocument: () -> Unit,
    onCreateSafDocument: () -> Unit,
    onOpenCorpusItem: (CorpusItem) -> Unit,
    onScrubTempCache: () -> Unit
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Banner
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = appTitle,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = appSubtitle,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }

                // Runtime Isolation & Security Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "P1 Runtime & Security Telemetry",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text("App ID: $applicationId", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("Authority: $fileProviderAuthority", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("Data Dir: $dataDirPath", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("Cache Dir: $cacheDirPath", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("S1 INTERNET: REMOVED | S2 MANAGE_STORAGE: REMOVED | S4 allowBackup: false", fontSize = 11.sp, color = Color(0xFF15803D))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Status: $statusMessage",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = accentColor
                        )
                    }
                }

                // SAF Actions (S2)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "S2 Storage Access Framework (SAF) Operations",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenSafDocument,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Open (SAF)", fontSize = 13.sp)
                            }
                            Button(
                                onClick = onCreateSafDocument,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Create New (SAF)", fontSize = 13.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = onScrubTempCache,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("S3: Run Deterministic Temp Cache Cleanup", fontSize = 12.sp)
                        }
                    }
                }

                // P1 Embedded Test Corpus
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "P1.3 Embedded Validation Corpus (Offline LOKit Engine)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        corpusItems.forEach { item ->
                            OutlinedButton(
                                onClick = { onOpenCorpusItem(item) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(item.description, fontSize = 11.sp, color = Color(0xFF475569))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
