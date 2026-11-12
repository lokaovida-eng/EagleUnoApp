package com.eagleuno.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
    var activeBgIndex by remember { mutableIntStateOf(prefs.getInt("bg_color_index", 0)) }

    // Background Themes
    val bgThemes = listOf(
        Color(0xFF0A0E18), // Dark Cyber Neon
        Color(0xFF180A28), // Cartoon Purple
        Color(0xFF280818), // Midnight Pink
        Color(0xFF041A24), // Electric Cyan
        Color(0xFF0F0F1A)  // Anime Dark
    )
    val currentBgColor = bgThemes.getOrElse(activeBgIndex) { bgThemes[0] }

    fun triggerHaptic() {
        try {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    fun saveCurrentNote() {
        triggerHaptic()
        if (currentTitle.isBlank() && currentContent.isBlank()) return

        val newNote = NoteItem(
            title = if (currentTitle.isBlank()) "Nota sem título" else currentTitle,
            content = currentContent
        )
        val updatedList = listOf(newNote) + savedNotes.filterNot { it.id == newNote.id }
        savedNotes = updatedList
        saveNotesToPrefs(prefs, updatedList)

        // Sync with Widgets
        NormalWidgetProvider.updateAllWidgets(context, newNote.title, newNote.content)
        CoverScreenWidgetProvider.updateAllCoverWidgets(context, newNote.title, newNote.content)
    }

    fun startNewNote() {
        triggerHaptic()
        currentTitle = ""
        currentContent = ""
    }

    fun deleteNote(noteId: String) {
        triggerHaptic()
        val updatedList = savedNotes.filterNot { it.id == noteId }
        savedNotes = updatedList
        saveNotesToPrefs(prefs, updatedList)

        val latestNote = updatedList.firstOrNull()
        NormalWidgetProvider.updateAllWidgets(context, latestNote?.title ?: "", latestNote?.content ?: "")
        CoverScreenWidgetProvider.updateAllCoverWidgets(context, latestNote?.title ?: "", latestNote?.content ?: "")
    }

    // Outer Fullscreen Container (100% Zero Black Borders for Z Flip 5 Cover Screen)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(currentBgColor)
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
                    // Note Title Field
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
                        modifier = Modifier.fillMaxWidth(),
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

                    Spacer(modifier = Modifier.height(6.dp))

                    HorizontalDivider(color = Color(0xFF00FFCC).copy(alpha = 0.5f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(6.dp))

                    // Content Field
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Anime Fierce Eagle Mascot (Águia de Anime com Cara de Mau) em Fundo Suave
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
                        .fillMaxHeight(0.80f)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        if (savedNotes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhuma nota salva ainda.",
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
                                items(savedNotes) { note ->
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
        for (note in notes) {
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("timestampMillis", note.timestampMillis)
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
