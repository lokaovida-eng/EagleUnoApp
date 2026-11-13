package com.eagleuno.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eagleuno.app.ui.theme.MyApplicationTheme
import com.eagleuno.app.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        window.setBackgroundDrawableResource(android.R.color.black)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    CartoonNeonNotepadScreen()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DATA MODEL FOR NOTES
// ---------------------------------------------------------------------------
data class NoteItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

// ---------------------------------------------------------------------------
// MAIN CARTOON NEON NOTEPAD SCREEN
// ---------------------------------------------------------------------------
@Composable
fun CartoonNeonNotepadScreen() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("neon_notes_prefs", Context.MODE_PRIVATE) }

    // States
    var currentTitle by remember { mutableStateOf("") }
    var currentContent by remember { mutableStateOf("") }
    var savedNotes by remember { mutableStateOf(loadNotesFromPrefs(prefs)) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeBgIndex by remember { mutableIntStateOf(prefs.getInt("bg_color_index", 0)) }

    // Background Themes
    val bgThemes = listOf(
        Color(0xFF03060B), // Dark Cyber Neon
        Color(0xFF160A26), // Cartoon Purple
        Color(0xFF260818), // Midnight Pink
        Color(0xFF031824), // Electric Cyan
        Color(0xFF0E0E18)  // Anime Dark
    )
    val targetBgColor = bgThemes.getOrElse(activeBgIndex) { bgThemes[0] }
    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 350),
        label = "BgColorAnimation"
    )

    fun triggerHaptic() {
        try {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    fun saveCurrentNote() {
        triggerHaptic()
        if (currentTitle.isBlank() && currentContent.isBlank()) return

        val newNote = NoteItem(
            title = currentTitle.ifBlank { "Nota sem título" },
            content = currentContent
        )
        val updatedList = listOf(newNote) + savedNotes.filterNot { it.id == newNote.id }
        savedNotes = updatedList
        saveNotesToPrefs(prefs, updatedList)

        // Sync with Widgets
        NormalWidgetProvider.updateAllWidgets(context, newNote.title, newNote.content)
        CoverScreenWidgetProvider.updateAllCoverWidgets(context, newNote.title, newNote.content)

        Toast.makeText(context, "Nota salva com sucesso! 💾", Toast.LENGTH_SHORT).show()
    }

    fun startNewNote() {
        triggerHaptic()
        currentTitle = ""
        currentContent = ""
    }

    fun copyCurrentNote() {
        triggerHaptic()
        if (currentContent.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Nota Neon", currentContent)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Copiado para a área de transferência! 📋", Toast.LENGTH_SHORT).show()
    }

    fun shareCurrentNote() {
        triggerHaptic()
        if (currentContent.isBlank()) return
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, currentTitle.ifBlank { "Nota Neon" })
                putExtra(Intent.EXTRA_TEXT, "${if (currentTitle.isNotBlank()) "$currentTitle\n\n" else ""}$currentContent")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Nota")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteNote(noteId: String) {
        triggerHaptic()
        val updatedList = savedNotes.filterNot { it.id == noteId }
        savedNotes = updatedList
        saveNotesToPrefs(prefs, updatedList)

        val latestNote = updatedList.firstOrNull()
        NormalWidgetProvider.updateAllWidgets(context, latestNote?.title ?: "Bloco Neon", latestNote?.content ?: "Nenhuma nota recente")
        CoverScreenWidgetProvider.updateAllCoverWidgets(context, latestNote?.title ?: "Bloco Neon", latestNote?.content ?: "Nenhuma nota recente")
    }

    val wordCount = remember(currentContent) {
        if (currentContent.isBlank()) 0 else currentContent.trim().split("\\s+".toRegex()).size
    }
    val charCount = currentContent.length

    // Outer Fullscreen Container (100% Zero Black Borders for Z Flip 5 Cover Screen)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgColor)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(6.dp),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp),
                start = 8.dp,
                end = 8.dp
            )
    ) {
        val isCompactDisplay = this.maxHeight < 450.dp || this.maxWidth < 400.dp

        // Cartoon Corner Borders
        CartoonCornerBorders(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF00FFCC)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Title
                Text(
                    text = "BLOCO NEON ⚡",
                    fontSize = if (isCompactDisplay) 15.sp else 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00FFCC)
                )

                // Header Action Buttons (📜 Histórico, ⚙ Configs)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Histórico Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF141E30))
                            .border(1.5.dp, Color(0xFFFF0055), RoundedCornerShape(10.dp))
                            .clickable {
                                triggerHaptic()
                                showHistoryDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📜 Histórico",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF0055)
                        )
                    }

                    // Config Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141E30))
                            .border(1.5.dp, Color(0xFFFFCC00), CircleShape)
                            .clickable {
                                triggerHaptic()
                                showSettingsDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚙",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFCC00)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. MIDDLE NOTE EDITOR (Com mascote Águia de Anime)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xBB080D18))
                    .border(2.dp, Color(0xFF00FFCC), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Note Title Field & Actions (Copy 📋, Share 📤)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = currentTitle,
                            onValueChange = { currentTitle = it },
                            textStyle = TextStyle(
                                fontSize = if (isCompactDisplay) 16.sp else 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFF0055)
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (currentTitle.isEmpty()) {
                                        Text(
                                            text = "Título da Nota...",
                                            fontSize = if (isCompactDisplay) 16.sp else 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFFF0055).copy(alpha = 0.45f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (currentContent.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📋",
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { copyCurrentNote() }
                                        .padding(4.dp)
                                )
                                Text(
                                    text = "📤",
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { shareCurrentNote() }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    HorizontalDivider(color = Color(0xFF00FFCC).copy(alpha = 0.5f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(6.dp))

                    // Content Field
                    Box(modifier = Modifier.weight(1f)) {
                        // Anime Fierce Eagle Mascot em Fundo
                        if (currentContent.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AnimeFierceEagleCanvas(
                                        modifier = Modifier.size(if (isCompactDisplay) 110.dp else 140.dp),
                                        eyeGlowColor = Color(0xFF00FFCC)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Aguardando nota... 🦅",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FFCC).copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Actual Note Body Field
                        BasicTextField(
                            value = currentContent,
                            onValueChange = { currentContent = it },
                            textStyle = TextStyle(
                                fontSize = if (isCompactDisplay) 13.sp else 15.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            ),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (currentContent.isEmpty()) {
                                        Text(
                                            text = "Escreva sua nota cartoon neon aqui...",
                                            fontSize = if (isCompactDisplay) 13.sp else 15.sp,
                                            fontWeight = FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White.copy(alpha = 0.35f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    // Contador de Caracteres / Palavras
                    Text(
                        text = "$charCount caracteres • $wordCount palavras",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. BOTTOM ACTION BUTTONS (Nova Nota ➕, Salvar 💾)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nova Nota Button
                Button(
                    onClick = { startNewNote() },
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompactDisplay) 40.dp else 46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141E30)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFCC00))
                ) {
                    Text(
                        text = "➕ Nova",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFCC00)
                    )
                }

                // Salvar Button
                Button(
                    onClick = { saveCurrentNote() },
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompactDisplay) 40.dp else 46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055))
                ) {
                    Text(
                        text = "💾 Salvar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }

        // DIÁLOGO HISTÓRICO DE NOTAS
        if (showHistoryDialog) {
            Dialog(onDismissRequest = { showHistoryDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0B101D))
                        .border(2.dp, Color(0xFFFF0055), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "📜 Minhas Notas Salvas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF0055),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Campo de Busca
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141E30))
                                .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "🔍 Pesquisar notas...",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredNotes = remember(savedNotes, searchQuery) {
                            if (searchQuery.isBlank()) savedNotes
                            else savedNotes.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.content.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        if (filteredNotes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (savedNotes.isEmpty()) "Nenhuma nota salva ainda." else "Nenhuma nota encontrada.",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredNotes) { note ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF141E30))
                                            .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(12.dp))
                                            .clickable {
                                                triggerHaptic()
                                                currentTitle = note.title
                                                currentContent = note.content
                                                showHistoryDialog = false
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = note.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF00FFCC),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = note.content,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = formatDate(note.timestampMillis),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.White.copy(alpha = 0.45f)
                                                )
                                            }

                                            TextButton(onClick = { deleteNote(note.id) }) {
                                                Text(text = "❌", fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                triggerHaptic()
                                showHistoryDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141E30))
                        ) {
                            Text(text = "Fechar", color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // DIÁLOGO CONFIGURAÇÕES (Trocar Cor de Fundo)
        if (showSettingsDialog) {
            Dialog(onDismissRequest = { showSettingsDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0B101D))
                        .border(2.dp, Color(0xFFFFCC00), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚙ Cores de Fundo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFCC00)
                        )

                        Text(
                            text = "Escolha a cor do seu bloco cartoon neon:",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bgThemes.forEachIndexed { index, color ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (activeBgIndex == index) 3.dp else 1.dp,
                                            color = if (activeBgIndex == index) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            triggerHaptic()
                                            activeBgIndex = index
                                            prefs.edit().putInt("bg_color_index", index).apply()
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                triggerHaptic()
                                showSettingsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055))
                        ) {
                            Text(text = "Pronto ⚡", color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DESENHO VETORIAL CARTOON: MOLDURA E MASCOTE ÁGUIA DE ANIME
// ---------------------------------------------------------------------------
@Composable
fun CartoonCornerBorders(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FFCC)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerLength = minOf(w, h) * 0.18f
        val strokeW = 4.dp.toPx()

        // Canto Superior Esquerdo
        drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth = strokeW, cap = StrokeCap.Round)

        // Canto Superior Direito
        drawLine(color, Offset(w, 0f), Offset(w - cornerLength, 0f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color, Offset(w, 0f), Offset(w, cornerLength), strokeWidth = strokeW, cap = StrokeCap.Round)

        // Canto Inferior Esquerdo
        drawLine(color, Offset(0f, h), Offset(cornerLength, h), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color, Offset(0f, h), Offset(0f, h - cornerLength), strokeWidth = strokeW, cap = StrokeCap.Round)

        // Canto Inferior Direito
        drawLine(color, Offset(w, h), Offset(w - cornerLength, h), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color, Offset(w, h), Offset(w, h - cornerLength), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
fun AnimeFierceEagleCanvas(
    modifier: Modifier = Modifier,
    eyeGlowColor: Color = Color(0xFF00FFCC)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Aura Neon Externa
        drawCircle(
            color = eyeGlowColor.copy(alpha = 0.18f),
            radius = minOf(w, h) * 0.45f,
            center = Offset(cx, cy)
        )

        // Cabeça com Penas Espetadas Anime
        val headPath = Path().apply {
            moveTo(cx, cy - h * 0.40f)
            lineTo(cx + w * 0.18f, cy - h * 0.28f)
            lineTo(cx + w * 0.35f, cy - h * 0.35f)
            lineTo(cx + w * 0.25f, cy - h * 0.15f)
            lineTo(cx + w * 0.42f, cy - h * 0.10f)
            lineTo(cx + w * 0.28f, cy + h * 0.08f)
            lineTo(cx + w * 0.35f, cy + h * 0.22f)
            lineTo(cx + w * 0.15f, cy + h * 0.18f)
            lineTo(cx, cy + h * 0.38f)
            lineTo(cx - w * 0.15f, cy + h * 0.18f)
            lineTo(cx - w * 0.35f, cy + h * 0.22f)
            lineTo(cx - w * 0.28f, cy + h * 0.08f)
            lineTo(cx - w * 0.42f, cy - h * 0.10f)
            lineTo(cx - w * 0.25f, cy - h * 0.15f)
            lineTo(cx - w * 0.35f, cy - h * 0.35f)
            lineTo(cx - w * 0.18f, cy - h * 0.28f)
            close()
        }
        drawPath(path = headPath, color = Color(0xFF0C101A))
        drawPath(
            path = headPath,
            color = eyeGlowColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Sobrancelhas Agressivas Anime (Cara de Mau)
        val leftEyebrow = Path().apply {
            moveTo(cx - w * 0.30f, cy - h * 0.18f)
            lineTo(cx - w * 0.05f, cy - h * 0.04f)
            lineTo(cx - w * 0.05f, cy - h * 0.09f)
            lineTo(cx - w * 0.28f, cy - h * 0.22f)
            close()
        }
        val rightEyebrow = Path().apply {
            moveTo(cx + w * 0.30f, cy - h * 0.18f)
            lineTo(cx + w * 0.05f, cy - h * 0.04f)
            lineTo(cx + w * 0.05f, cy - h * 0.09f)
            lineTo(cx + w * 0.28f, cy - h * 0.22f)
            close()
        }
        drawPath(path = leftEyebrow, color = Color(0xFFFF0055))
        drawPath(path = rightEyebrow, color = Color(0xFFFF0055))

        // Olhos de Anime Inclinados
        val leftEye = Path().apply {
            moveTo(cx - w * 0.26f, cy - h * 0.12f)
            lineTo(cx - w * 0.08f, cy - h * 0.02f)
            lineTo(cx - w * 0.22f, cy + h * 0.04f)
            close()
        }
        val rightEye = Path().apply {
            moveTo(cx + w * 0.26f, cy - h * 0.12f)
            lineTo(cx + w * 0.08f, cy - h * 0.02f)
            lineTo(cx + w * 0.22f, cy + h * 0.04f)
            close()
        }
        drawPath(path = leftEye, color = Color.White)
        drawPath(path = rightEye, color = Color.White)

        // Pupilas Neon
        drawCircle(
            color = eyeGlowColor,
            radius = 6.dp.toPx(),
            center = Offset(cx - w * 0.15f, cy - h * 0.03f)
        )
        drawCircle(
            color = eyeGlowColor,
            radius = 6.dp.toPx(),
            center = Offset(cx + w * 0.15f, cy - h * 0.03f)
        )

        // Bico Afiado de Águia
        val beakPath = Path().apply {
            moveTo(cx - w * 0.10f, cy + h * 0.02f)
            lineTo(cx + w * 0.10f, cy + h * 0.02f)
            lineTo(cx + w * 0.14f, cy + h * 0.12f)
            lineTo(cx, cy + h * 0.32f)
            lineTo(cx - w * 0.14f, cy + h * 0.12f)
            close()
        }
        drawPath(path = beakPath, color = Color(0xFFFFCC00))
        drawPath(
            path = beakPath,
            color = Color.White,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ---------------------------------------------------------------------------
// HELPER FUNCTIONS FOR LOCAL STORAGE
// ---------------------------------------------------------------------------
fun saveNotesToPrefs(prefs: SharedPreferences, notes: List<NoteItem>) {
    try {
        val jsonArray = JSONArray()
        for ((id, title, content, timestampMillis) in notes) {
            val obj = JSONObject().apply {
                put("id", id)
                put("title", title)
                put("content", content)
                put("timestampMillis", timestampMillis)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("saved_notes_json", jsonArray.toString()).apply()
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

fun loadNotesFromPrefs(prefs: SharedPreferences): List<NoteItem> {
    val list = mutableListOf<NoteItem>()
    try {
        val jsonStr = prefs.getString("saved_notes_json", null) ?: return emptyList()
        val jsonArray = JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                NoteItem(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    title = obj.optString("title", "Sem título"),
                    content = obj.optString("content", ""),
                    timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis())
                )
            )
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    return list
}

fun formatDate(millis: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(millis))
    } catch (_: Exception) {
        ""
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun CartoonNeonNotepadScreenPreview() {
    MyApplicationTheme {
        CartoonNeonNotepadScreen()
    }
}
