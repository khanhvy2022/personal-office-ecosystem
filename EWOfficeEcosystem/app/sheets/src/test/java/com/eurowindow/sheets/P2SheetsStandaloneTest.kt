package com.eurowindow.sheets

import com.eurowindow.office.core.security.BroadcastIsolationPolicy
import com.eurowindow.office.core.security.EWOfflineUrlPolicy
import com.eurowindow.office.core.security.TempFileGuard
import com.eurowindow.office.core.storage.SafDocumentController
import com.eurowindow.sheets.engine.XlsxEngine
import com.eurowindow.sheets.model.CellAlignment
import com.eurowindow.sheets.model.XlsxCell
import com.eurowindow.sheets.model.XlsxWorkbook
import com.eurowindow.sheets.model.XlsxWorksheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Automated Test Suite for EW Sheets Native Light Architecture (<15 MB):
 * - Pure Kotlin OpenXML XLSX parser & serializer (Zero C++ .so, 0 MB native code)
 * - CSV import & export engine
 * - SAF Document Controller & spreadsheet MIME types (XLSX, CSV, PDF)
 * - Application Descriptor & zero-shared-state isolation (com.eurowindow.sheets vs documents)
 * - Ephemeral Cache cleanup (S3) & preservation of user spreadsheets
 * - Manifest security audit (0 INTERNET, 0 MANAGE_EXTERNAL_STORAGE, allowBackup=false)
 */
class P2SheetsStandaloneTest {

    private fun resolveProjectRoot(): File {
        var dir: File = File(System.getProperty("user.dir") ?: ".").canonicalFile
        while (dir.parentFile != null && !File(dir, "core").exists()) {
            dir = dir.parentFile!!
        }
        return dir
    }

    @Test
    fun verifyXlsxEngine_createsParsesAndSerializesOpenXmlWithoutErrors() {
        // 1. Create workbook with Vietnamese text, multi-sheets, formulas, and formatting
        val sheet1 = XlsxWorksheet(name = "Báo giá nhôm")
        sheet1.setCellValue(1, 1, "BÁO GIÁ CỬA NHÔM EUROWINDOW 2026")
        sheet1.getCell(1, 1).isBold = true
        sheet1.getCell(1, 1).colorHex = "166534"

        // Headers
        sheet1.setCellValue(3, 1, "Mã SP")
        sheet1.setCellValue(3, 2, "Tên sản phẩm có dấu (tiếng Việt)")
        sheet1.setCellValue(3, 3, "Số lượng")
        sheet1.setCellValue(3, 4, "Đơn giá")
        sheet1.setCellValue(3, 5, "Thành tiền")

        // Rows
        sheet1.setCellValue(4, 1, "EW-01")
        sheet1.setCellValue(4, 2, "Cửa đi mở trượt 4 cánh EA55")
        sheet1.setCellValue(4, 3, "2")
        sheet1.setCellValue(4, 4, "6500000")
        sheet1.setCellValue(4, 5, "13000000")

        sheet1.setCellValue(5, 1, "EW-02")
        sheet1.setCellValue(5, 2, "Cửa sổ mở hất kết hợp nan chớp")
        sheet1.setCellValue(5, 3, "3")
        sheet1.setCellValue(5, 4, "3200000")
        sheet1.setCellValue(5, 5, "9600000")

        // Formula row
        sheet1.setCellValue(6, 2, "TỔNG CỘNG:")
        sheet1.getCell(6, 2).isBold = true
        val sumCell = sheet1.getCell(6, 5)
        sumCell.formula = "=SUM(E4:E5)"
        sumCell.value = "22600000"
        sumCell.isBold = true

        val sheet2 = XlsxWorksheet(name = "Phụ kiện đi kèm")
        sheet2.setCellValue(1, 1, "Bản lề 3D & Khóa đa điểm Roto")

        val workbook = XlsxWorkbook(
            title = "BaoGia_DuAn",
            sheets = mutableListOf(sheet1, sheet2)
        )

        // 2. Serialize to OpenXML XLSX bytes
        val outStream = ByteArrayOutputStream()
        XlsxEngine.writeXlsx(workbook, outStream)
        val xlsxBytes = outStream.toByteArray()
        assertTrue("Generated XLSX bytes must be non-empty", xlsxBytes.isNotEmpty())

        // 3. Parse back from bytes
        val parsedWb = XlsxEngine.parseXlsx(ByteArrayInputStream(xlsxBytes))
        assertNotNull("Parsed workbook must not be null", parsedWb)
        assertEquals("Must contain 2 worksheets", 2, parsedWb.sheets.size)
        assertEquals("Báo giá nhôm", parsedWb.sheets[0].name)
        assertEquals("Phụ kiện đi kèm", parsedWb.sheets[1].name)

        val parsedSheet1 = parsedWb.sheets[0]
        assertEquals("BÁO GIÁ CỬA NHÔM EUROWINDOW 2026", parsedSheet1.getCell(1, 1).value)
        assertEquals("Cửa đi mở trượt 4 cánh EA55", parsedSheet1.getCell(4, 2).value)
        assertEquals("13000000", parsedSheet1.getCell(4, 5).value)
        assertEquals("=SUM(E4:E5)", parsedSheet1.getCell(6, 5).formula)
        assertEquals("22600000", parsedSheet1.getCell(6, 5).value)

        val parsedSheet2 = parsedWb.sheets[1]
        assertEquals("Bản lề 3D & Khóa đa điểm Roto", parsedSheet2.getCell(1, 1).value)
    }

    @Test
    fun verifyCsvExportAndParse_commaSeparatedIntegrity() {
        val sheet = XlsxWorksheet(name = "Danh mục vật tư")
        sheet.setCellValue(1, 1, "Mã")
        sheet.setCellValue(1, 2, "Mô tả")
        sheet.setCellValue(1, 3, "Số lượng")
        sheet.setCellValue(2, 1, "VT01")
        sheet.setCellValue(2, 2, "Ke góc ép nhôm định hình")
        sheet.setCellValue(2, 3, "50")

        val wb = XlsxWorkbook(title = "VatTu", sheets = mutableListOf(sheet))
        val out = ByteArrayOutputStream()
        XlsxEngine.exportToCsv(sheet, out)

        val csvString = String(out.toByteArray(), StandardCharsets.UTF_8)
        assertTrue(csvString.contains("Ke góc ép nhôm định hình"))
        assertTrue(csvString.contains("VT01"))

        val parsedCsvWb = XlsxEngine.parseXlsx(ByteArrayInputStream(out.toByteArray()), isCsv = true)
        assertEquals(1, parsedCsvWb.sheets.size)
        assertEquals("VT01", parsedCsvWb.sheets[0].getCell(2, 1).value)
        assertEquals("Ke góc ép nhôm định hình", parsedCsvWb.sheets[0].getCell(2, 2).value)
        assertEquals("50", parsedCsvWb.sheets[0].getCell(2, 3).value)
    }

    @Test
    fun verifyCellCoordinatesAndConverters() {
        assertEquals("A", XlsxCell.colToLetter(1))
        assertEquals("B", XlsxCell.colToLetter(2))
        assertEquals("Z", XlsxCell.colToLetter(26))
        assertEquals("AA", XlsxCell.colToLetter(27))
        assertEquals("AB", XlsxCell.colToLetter(28))

        assertEquals(1, XlsxCell.letterToCol("A"))
        assertEquals(2, XlsxCell.letterToCol("B"))
        assertEquals(26, XlsxCell.letterToCol("Z"))
        assertEquals(27, XlsxCell.letterToCol("AA"))
        assertEquals(28, XlsxCell.letterToCol("AB"))

        val cell = XlsxCell(5, 3, value = "Test")
        assertEquals("C5", cell.cellRef)
    }

    @Test
    fun verifySafController_sheetsMimeTypesAndIntents() {
        assertEquals(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            SafDocumentController.MIME_XLSX
        )
        assertEquals("text/csv", SafDocumentController.MIME_CSV)
        assertEquals("application/pdf", SafDocumentController.MIME_PDF)

        val openIntent = SafDocumentController.buildOpenDocumentIntent(
            arrayOf(SafDocumentController.MIME_XLSX, SafDocumentController.MIME_CSV, "application/vnd.ms-excel")
        )
        assertNotNull("Open document intent must be constructed", openIntent)

        val createXlsxIntent = SafDocumentController.buildCreateDocumentIntent(
            SafDocumentController.MIME_XLSX,
            "Bang_tinh_Eurowindow.xlsx"
        )
        assertNotNull("Create XLSX document intent must be constructed", createXlsxIntent)

        val createCsvIntent = SafDocumentController.buildCreateDocumentIntent(
            SafDocumentController.MIME_CSV,
            "Xuat_du_lieu.csv"
        )
        assertNotNull("Create CSV document intent must be constructed", createCsvIntent)
    }

    @Test
    fun verifySheetsAppDescriptor_isStrictlyIsolatedFromDocuments() {
        val sheetDesc = BroadcastIsolationPolicy.SHEETS_DESCRIPTOR
        assertEquals("com.eurowindow.sheets", sheetDesc.applicationId)
        assertEquals("com.eurowindow.sheets.fileprovider", sheetDesc.fileProviderAuthority)
        assertEquals("com.eurowindow.sheets.clipboard", sheetDesc.clipboardAuthority)
        assertEquals("ew_sheets.db", sheetDesc.databaseName)
        assertEquals("ew_sheets_prefs", sheetDesc.dataStoreName)

        val docDesc = BroadcastIsolationPolicy.DOCUMENTS_DESCRIPTOR
        assertFalse(sheetDesc.applicationId == docDesc.applicationId)
        assertFalse(sheetDesc.fileProviderAuthority == docDesc.fileProviderAuthority)
        assertFalse(sheetDesc.clipboardAuthority == docDesc.clipboardAuthority)
        assertFalse(sheetDesc.databaseName == docDesc.databaseName)
        assertFalse(sheetDesc.dataStoreName == docDesc.dataStoreName)

        assertTrue(BroadcastIsolationPolicy.verifyNoSharedNamespaces())
    }

    @Test
    fun verifySaveAndExportPdf_ephemeralArtifactCleanup() {
        val tempRoot = Files.createTempDirectory("ew_p2_sheet_test").toFile()
        try {
            val cacheDir = File(tempRoot, "cache").apply { mkdirs() }
            val userDir = File(tempRoot, "user_sheets").apply { mkdirs() }

            val userOriginalXlsx = File(userDir, "DuToan_NhomKinh.xlsx").apply {
                writeText("USER_XLSX_PERSISTED_CONTENT")
            }
            val workingCacheXlsx = File(cacheDir, "DuToan_NhomKinh.xlsx").apply {
                writeText("TEMP_CACHE_XLSX_BYTES")
            }
            val exportedPdf = File(cacheDir, "sheet_export.pdf").apply {
                writeText("%PDF-1.4 EXPORTED_SHEET_DOCUMENT")
            }
            val exportedCsv = File(cacheDir, "sheet_export.csv").apply {
                writeText("col1,col2\nval1,val2")
            }

            val report = TempFileGuard.scrubAppCacheDirectory(
                cacheDir = cacheDir,
                explicitTempFile = workingCacheXlsx,
                protectedUserFiles = setOf(userOriginalXlsx)
            )

            assertFalse(workingCacheXlsx.exists())
            assertFalse(exportedPdf.exists())
            assertFalse(exportedCsv.exists())
            assertTrue(userOriginalXlsx.exists())
            assertEquals("USER_XLSX_PERSISTED_CONTENT", userOriginalXlsx.readText())
            assertEquals(3, report.deletedFiles.size)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun verifySheetsManifest_securityAndIsolationContract() {
        val projRoot = resolveProjectRoot()
        val manifestFile = File(projRoot, "app/sheets/src/main/AndroidManifest.xml")
        assertTrue("Sheets AndroidManifest.xml must exist", manifestFile.exists())
        val manifestXml = manifestFile.readText()

        assertTrue(manifestXml.contains("android:label=\"@string/app_name\""))
        assertTrue(manifestXml.contains("android:allowBackup=\"false\""))

        assertTrue(
            manifestXml.contains("android.permission.INTERNET") &&
                manifestXml.contains("tools:node=\"remove\"")
        )

        assertTrue(
            manifestXml.contains("android.permission.MANAGE_EXTERNAL_STORAGE") &&
                manifestXml.contains("tools:node=\"remove\"")
        )

        assertTrue(manifestXml.contains("android:authorities=\"com.eurowindow.sheets.fileprovider\""))
        assertTrue(manifestXml.contains("android:name=\"androidx.core.content.FileProvider\""))
        assertTrue(manifestXml.contains(".SheetsPrototypeActivity"))
    }
}
