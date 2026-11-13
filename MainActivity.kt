package com.tablesnap.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.xml.sax.helpers.AttributesImpl

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TableSnapApp(this) }
    }
}

private enum class OcrScript(val label: String) {
    LATIN("Latin · English / Malay / Indonesian / etc."),
    CHINESE("Chinese"), DEVANAGARI("Devanagari · Hindi"), JAPANESE("Japanese"), KOREAN("Korean")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableSnapApp(context: Context) {
    var rows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf(context.getString(R.string.initial_message)) }
    var script by remember { mutableStateOf(OcrScript.LATIN) }
    var menuOpen by remember { mutableStateOf(false) }
    var sourceName by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            busy = true
            message = context.getString(R.string.processing)
            sourceName = uri.lastPathSegment ?: "image"
            runOcr(context, uri, script) { result, error ->
                busy = false
                if (error != null) message = error
                else if (result.isEmpty()) message = context.getString(R.string.no_text)
                else {
                    rows = normalizeRows(result)
                    message = context.getString(R.string.recognized_rows, rows.size)
                }
            }
        }
    }
    val xlsxSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) try { context.contentResolver.openOutputStream(uri)?.use { XlsxWriter.write(it, rows) }; message = context.getString(R.string.xlsx_exported) } catch (e: Exception) { message = "Export failed: ${e.localizedMessage}" }
    }
    val csvSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) try { context.contentResolver.openOutputStream(uri)?.use { CsvWriter.write(it, rows) }; message = context.getString(R.string.csv_exported) } catch (e: Exception) { message = "Export failed: ${e.localizedMessage}" }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF176B54), secondary = Color(0xFF56A58C), background = Color(0xFFF6F8F7))) {
        Scaffold(containerColor = Color(0xFFF6F8F7), topBar = {
            TopAppBar(title = {
                Column {
                    Text("TableSnap", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(stringResource(R.string.subtitle), fontSize = 12.sp, color = Color.Gray)
                }
            }, actions = {
                IconButton(onClick = { rows = emptyList(); message = context.getString(R.string.ready) }) { Icon(Icons.Default.Refresh, "Reset") }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6F8F7)))
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE5ECE8))) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.hero_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Your image is processed on-device. Nothing is uploaded to a TableSnap server.", color = Color(0xFF66736D), fontSize = 13.sp)
                        ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { menuOpen = it }) {
                            OutlinedTextField(value = script.label, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.recognition_language)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuOpen) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                            ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                OcrScript.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { script = option; menuOpen = false }) }
                            }
                        }
                        Button(onClick = { imagePicker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Processing…" else stringResource(R.string.choose_image), fontWeight = FontWeight.SemiBold)
                        }
                        Text(message, color = Color(0xFF66736D), fontSize = 12.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(stringResource(R.string.table_preview), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(if (rows.isEmpty()) stringResource(R.string.no_data) else "${rows.size} rows · ${rows.maxOfOrNull { it.size } ?: 0} columns", color = Color.Gray, fontSize = 12.sp)
                    }
                    TextButton(onClick = { rows = rows + listOf(List(maxOf(1, rows.maxOfOrNull { it.size } ?: 1)) { "" }) }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Text(stringResource(R.string.add_row))
                    }
                }

                if (rows.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE5ECE8))) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Image, null, tint = Color(0xFF9BB8AC), modifier = Modifier.size(42.dp))
                                Text(stringResource(R.string.empty_title), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.empty_tip), color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Card(modifier = Modifier.weight(1f).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE5ECE8))) {
                        Column {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.edit_cells), color = Color(0xFF718078), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { rows = rows.map { it + "" } }) { Text(stringResource(R.string.add_column)) }
                            }
                            HorizontalDivider(color = Color(0xFFE8EEEA))
                            LazyColumn(Modifier.fillMaxSize()) {
                                itemsIndexed(rows) { rowIndex, row ->
                                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${rowIndex + 1}", modifier = Modifier.width(28.dp), color = Color.Gray, fontSize = 11.sp)
                                        row.forEachIndexed { colIndex, cell ->
                                            BasicTextField(value = cell, onValueChange = { newValue -> rows = rows.toMutableList().also { list -> list[rowIndex] = list[rowIndex].toMutableList().also { it[colIndex] = newValue } } }, modifier = Modifier.width(132.dp).padding(end = 6.dp).then(Modifier), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF20342B)))
                                            if (colIndex < row.lastIndex) Spacer(Modifier.width(3.dp))
                                        }
                                        IconButton(onClick = { rows = rows.toMutableList().also { it.removeAt(rowIndex) } }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Delete, "Delete row", tint = Color(0xFF9A7777), modifier = Modifier.size(16.dp)) }
                                    }
                                    HorizontalDivider(color = Color(0xFFF0F3F1))
                                }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { csvSaver.launch("tablesnap-export.csv") }, enabled = rows.isNotEmpty() && !busy, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.export_csv))
                    }
                    Button(onClick = { xlsxSaver.launch("tablesnap-export.xlsx") }, enabled = rows.isNotEmpty() && !busy, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.FileDownload, null); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.export_excel))
                    }
                }
            }
        }
    }
}

private fun runOcr(context: Context, uri: Uri, script: OcrScript, callback: (List<List<String>>, String?) -> Unit) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = when (script) {
            OcrScript.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            OcrScript.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            OcrScript.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            OcrScript.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
        recognizer.process(image)
            .addOnSuccessListener { text -> callback(extractCells(text), null); recognizer.close() }
            .addOnFailureListener { e -> callback(emptyList(), "Could not read image: ${e.localizedMessage ?: "unknown error"}"); recognizer.close() }
    } catch (e: Exception) { callback(emptyList(), "Could not open image: ${e.localizedMessage ?: "unknown error"}") }
}

private data class WordBox(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerY get() = (top + bottom) / 2
    val height get() = (bottom - top).coerceAtLeast(1)
}

private fun extractCells(result: Text): List<List<String>> {
    val words = result.textBlocks.flatMap { block -> block.lines.flatMap { line ->
        line.elements.mapNotNull { element -> element.boundingBox?.let { WordBox(element.text, it.left, it.top, it.right, it.bottom) } }
    } }.sortedWith(compareBy<WordBox> { it.centerY }.thenBy { it.left })
    if (words.isEmpty()) return result.text.lines().filter { it.isNotBlank() }.map { listOf(it.trim()) }

    val rows = mutableListOf<MutableList<WordBox>>()
    for (word in words) {
        val target = rows.minByOrNull { row -> kotlin.math.abs(row.map { it.centerY }.average().toInt() - word.centerY) }
        if (target != null && kotlin.math.abs(target.map { it.centerY }.average().toInt() - word.centerY) <= (word.height * 0.65).coerceAtLeast(8)) target.add(word)
        else rows.add(mutableListOf(word))
    }
    return rows.sortedBy { it.map(WordBox::centerY).average() }.map { row ->
        val sorted = row.sortedBy { it.left }
        val typicalHeight = sorted.map { it.height }.average().toInt().coerceAtLeast(8)
        val cells = mutableListOf<String>()
        var current = StringBuilder()
        var previous: WordBox? = null
        for (word in sorted) {
            val gap = previous?.let { word.left - it.right } ?: 0
            if (previous != null && gap > typicalHeight * 1.35) {
                cells.add(current.toString().trim()); current = StringBuilder()
            } else if (current.isNotEmpty()) current.append(' ')
            current.append(word.text)
            previous = word
        }
        if (current.isNotEmpty()) cells.add(current.toString().trim())
        cells
    }.filter { it.any(String::isNotBlank) }
}

private fun normalizeRows(input: List<List<String>>): List<List<String>> {
    val width = input.maxOfOrNull { it.size } ?: 1
    return input.map { row -> row + List((width - row.size).coerceAtLeast(0)) { "" } }
}

private object CsvWriter {
    fun write(out: OutputStream, rows: List<List<String>>) {
        val text = rows.joinToString("\r\n") { row -> row.joinToString(",") { cell -> "\"${cell.replace("\"", "\"\"")}\"" } }
        out.write(("\uFEFF" + text).toByteArray(Charsets.UTF_8))
    }
}

/** Minimal standards-based XLSX writer. Cells are emitted as inline strings to avoid formula injection. */
private object XlsxWriter {
    fun write(out: OutputStream, rows: List<List<String>>) {
        ZipOutputStream(out).use { zip ->
            entry(zip, "[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            entry(zip, "_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry(zip, "xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="TableSnap" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            entry(zip, "xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            val sheet = buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
                rows.forEachIndexed { r, row ->
                    append("<row r=\"${r + 1}\">")
                    row.forEachIndexed { c, value ->
                        if (value.isNotEmpty()) {
                            val ref = columnName(c + 1) + (r + 1)
                            append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xml(value)}</t></is></c>")
                        }
                    }
                    append("</row>")
                }
                append("</sheetData></worksheet>")
            }
            entry(zip, "xl/worksheets/sheet1.xml", sheet)
        }
    }
    private fun entry(zip: ZipOutputStream, name: String, value: String) { zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
    private fun xml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
    private fun columnName(n: Int): String { var x = n; val b = StringBuilder(); while (x > 0) { val r = (x - 1) % 26; b.insert(0, ('A'.code + r).toChar()); x = (x - 1) / 26 }; return b.toString() }
}
