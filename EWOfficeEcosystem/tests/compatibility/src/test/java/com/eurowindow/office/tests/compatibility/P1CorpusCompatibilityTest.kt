package com.eurowindow.office.tests.compatibility

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class P1CorpusCompatibilityTest {

    private fun resolveRepoRoot(): File {
        var dir: File = File(System.getProperty("user.dir") ?: ".").canonicalFile
        while (dir.parentFile != null && !File(dir, "tests/corpus").exists()) {
            dir = dir.parentFile!!
        }
        return dir
    }

    @Test
    fun verifyDocxCorpus_vietnameseUnicodeTablesImagesAndPageLayout() {
        val repoRoot = resolveRepoRoot()
        val docxDir = File(repoRoot, "tests/corpus/docx")
        assertTrue("DOCX corpus directory must exist", docxDir.exists())

        val f1 = File(docxDir, "01_vietnamese_unicode_small.docx")
        ZipFile(f1).use { zf ->
            val docXml = zf.getInputStream(zf.getEntry("word/document.xml")).bufferedReader(Charsets.UTF_8).readText()
            // Verify Vietnamese Unicode ("Việt Nam — Độc lập - Tự do - Hạnh phúc")
            assertTrue("Must contain Vietnamese diacritics", docXml.contains("Vi\u1ec7t Nam") && docXml.contains("\u0110\u1ed9c l\u1eadp"))
            assertTrue("Must contain A4 page size w:w=11906", docXml.contains("11906"))
        }

        val f2 = File(docxDir, "02_tables_and_links_medium.docx")
        ZipFile(f2).use { zf ->
            val docXml = zf.getInputStream(zf.getEntry("word/document.xml")).bufferedReader(Charsets.UTF_8).readText()
            val relsXml = zf.getInputStream(zf.getEntry("word/_rels/document.xml.rels")).bufferedReader(Charsets.UTF_8).readText()
            assertTrue("Must contain OOXML table <w:tbl>", docXml.contains("<w:tbl>"))
            assertTrue("Must contain table rows <w:tr>", docXml.contains("<w:tr>"))
            assertTrue("Must contain table cells <w:tc>", docXml.contains("<w:tc>"))
            assertTrue("Must contain hyperlink element <w:hyperlink", docXml.contains("<w:hyperlink"))
            assertTrue("Must contain external hyperlink relationship", relsXml.contains("https://eurowindow.biz"))
        }

        val f3 = File(docxDir, "03_images_headers_pagebreaks.docx")
        ZipFile(f3).use { zf ->
            assertTrue("Must contain embedded PNG media", zf.getEntry("word/media/image1.png") != null)
            assertTrue("Must contain header1.xml", zf.getEntry("word/header1.xml") != null)
            assertTrue("Must contain footer1.xml", zf.getEntry("word/footer1.xml") != null)
        }
    }

    @Test
    fun verifyXlsxCorpus_formulasMultiSheetFormattingAndLargeDataset() {
        val repoRoot = resolveRepoRoot()
        val xlsxDir = File(repoRoot, "tests/corpus/xlsx")
        assertTrue("XLSX corpus directory must exist", xlsxDir.exists())

        val x1 = File(xlsxDir, "01_formulas_multisheet_small.xlsx")
        ZipFile(x1).use { zf ->
            val sheet1 = zf.getInputStream(zf.getEntry("xl/worksheets/sheet1.xml")).bufferedReader(Charsets.UTF_8).readText()
            assertTrue("Must contain SUM formula", sheet1.contains("<f>SUM(D3:D17)</f>"))
            assertTrue("Must contain IF formula", sheet1.contains("<f>IF(D18&gt;1000000"))
            assertTrue("Must contain SUMIF formula", sheet1.contains("<f>SUMIF(B3:B17"))
            assertTrue("Must contain VLOOKUP formula", sheet1.contains("VLOOKUP"))
            assertTrue("Must contain XLOOKUP formula", sheet1.contains("_xlfn.XLOOKUP"))
            assertTrue("Must contain second worksheet", zf.getEntry("xl/worksheets/sheet2.xml") != null)
        }

        val x3 = File(xlsxDir, "03_large_dataset_2000rows.xlsx")
        ZipFile(x3).use { zf ->
            val sheet1 = zf.getInputStream(zf.getEntry("xl/worksheets/sheet1.xml")).bufferedReader(Charsets.UTF_8).readText()
            assertTrue("Must contain row 2001 in 2000-row dataset", sheet1.contains("2001"))
            assertTrue("Must contain formula cells", sheet1.contains("<f>"))
        }
    }
}
