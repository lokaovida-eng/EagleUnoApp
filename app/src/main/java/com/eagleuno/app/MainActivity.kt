package com.eagleuno.app

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleuno.app.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlin.math.max

enum class ThemeStyle(
    val title: String,
    val imageResId: Int,
    val primaryColor: Color
) {
    VERMELHO_METAL("Vermelho Metal", R.drawable.theme_red, Color(0xFFE50914)),
    CAMUFLADO_ROSA("Camuflado Rosa", R.drawable.theme_art_4, Color(0xFFFF007F)),
    PRATA_METAL("Prata Metal", R.drawable.theme_silver, Color(0xFF00E5FF))
}

enum class ScreenState {
    HOME,
    SPEEDOMETER,
    SETTINGS,
    THEMES,
    ABOUT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Transparência total da janela no Android
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    EagleUnoApp()
                }
            }
        }
    }
}

@Composable
fun EagleUnoApp() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var selectedTheme by remember { mutableStateOf(ThemeStyle.VERMELHO_METAL) }
    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
    // ATIVADO POR PADRÃO: Efeito RGB vibrante
    var isRgbEnabled by remember { mutableStateOf(true) }

    var isAppVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAppVisible = true
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isAppVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "SeamlessAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isAppVisible) 1f else 0.98f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "SeamlessScale"
    )

    var focusedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentScreen) {
        focusedIndex = 0
    }

    // Estados de Velocidade, Distância e Horas Dirigidas
    var speedKmH by remember { mutableFloatStateOf(0f) }
    var distanceKm by remember { mutableFloatStateOf(0.00f) }
    var movingTimeMillis by remember { mutableLongStateOf(0L) }
    var previousSpeed by remember { mutableFloatStateOf(0f) }
    var accelerationRate by remember { mutableFloatStateOf(0f) }

    // Cronômetro que RODA em movimento e PAUSA quando parado
    // ZERA SOMENTE quando o usuário clica em "Zerar KM"
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            if (speedKmH > 0.5f) {
                movingTimeMillis += 1000L
            }
        }
    }

    LaunchedEffect(speedKmH) {
        accelerationRate = speedKmH - previousSpeed
        previousSpeed = speedKmH
    }

    val isMoving = speedKmH > 0.8f
    val isAccelerating = isMoving && accelerationRate > 0.2f
    val isDecelerating = isMoving && accelerationRate < -0.2f

    // Frequência dinâmica da piscada estilo RPM de volante de corrida
    val pulseDurationMillis = when {
        isAccelerating -> {
            val factor = (speedKmH * 5f + accelerationRate * 80f).toInt()
            max(90, 800 - factor)
        }
        isDecelerating -> {
            val factor = (speedKmH * 4f + kotlin.math.abs(accelerationRate) * 80f).toInt()
            max(100, 800 - factor)
        }
        else -> 500
    }

    val infiniteTransition = rememberInfiniteTransition(label = "RPMNeonGlow")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RPMAlphaGlow"
    )

    val rainbowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF0033),
        targetValue = Color(0xFF00E5FF),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RainbowAnimation"
    )

    val redNeonColor = Color(0xFFFF0033).copy(alpha = alphaGlow)
    val babyBlueNeonColor = Color(0xFF00F0FF).copy(alpha = alphaGlow)

    val activeAccentColor = when {
        isAccelerating -> redNeonColor
        isDecelerating -> babyBlueNeonColor
        isRgbEnabled -> rainbowColor
        else -> selectedTheme.primaryColor
    }

    // GPS Sensor Listener
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationListener = object : LocationListener {
            var lastLocation: Location? = null

            override fun onLocationChanged(location: Location) {
                try {
                    val currentSpeed = if (location.hasSpeed()) location.speed * 3.6f else 0f
                    speedKmH = currentSpeed.coerceAtLeast(0f)

                    if (lastLocation != null && currentSpeed > 0.8f && location.hasAccuracy() && location.accuracy < 35f) {
                        val deltaMeters = lastLocation!!.distanceTo(location)
                        if (deltaMeters in 0.8..300.0) {
                            distanceKm += (deltaMeters / 1000f)
                        }
                    }
                    lastLocation = location

                    NormalWidgetProvider.updateAllWidgets(context, speedKmH, distanceKm)
                    CoverScreenWidgetProvider.updateAllCoverWidgets(context, speedKmH)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in API level 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            if (locationManager != null &&
                (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                 context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            ) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0.5f,
                    locationListener
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        onDispose {
            try {
                locationManager?.removeUpdates(locationListener)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun triggerHaptic() {
        try {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    // Navegação dos Botões Físicos
    fun onPhysicalLeftClick() {
        triggerHaptic()
        when (currentScreen) {
            ScreenState.HOME -> focusedIndex = (focusedIndex - 1 + 2) % 2 // 2 Opções na Home
            ScreenState.SPEEDOMETER -> focusedIndex = (focusedIndex - 1 + 2) % 2
            ScreenState.SETTINGS -> focusedIndex = (focusedIndex - 1 + 4) % 4 // 4 Opções nas Configurações
            ScreenState.THEMES -> {
                val total = ThemeStyle.entries.size + 1
                focusedIndex = (focusedIndex - 1 + total) % total
            }
            ScreenState.ABOUT -> focusedIndex = 0
        }
    }

    fun onPhysicalRightClick() {
        triggerHaptic()
        when (currentScreen) {
            ScreenState.HOME -> focusedIndex = (focusedIndex + 1) % 2
            ScreenState.SPEEDOMETER -> focusedIndex = (focusedIndex + 1) % 2
            ScreenState.SETTINGS -> focusedIndex = (focusedIndex + 1) % 4
            ScreenState.THEMES -> {
                val total = ThemeStyle.entries.size + 1
                focusedIndex = (focusedIndex + 1) % total
            }
            ScreenState.ABOUT -> focusedIndex = 0
        }
    }

    fun onPhysicalCenterClick() {
        triggerHaptic()
        when (currentScreen) {
            ScreenState.HOME -> {
                when (focusedIndex) {
                    0 -> { currentScreen = ScreenState.SPEEDOMETER; focusedIndex = 0 }
                    1 -> { currentScreen = ScreenState.SETTINGS; focusedIndex = 0 }
                }
            }
            ScreenState.SPEEDOMETER -> {
                if (focusedIndex == 0) {
                    currentScreen = ScreenState.HOME
                    focusedIndex = 0
                } else {
                    distanceKm = 0f
                    movingTimeMillis = 0L
                    NormalWidgetProvider.updateAllWidgets(context, 0f, 0f)
                    CoverScreenWidgetProvider.updateAllCoverWidgets(context, 0f)
                }
            }
            ScreenState.SETTINGS -> {
                when (focusedIndex) {
                    0 -> { currentScreen = ScreenState.HOME; focusedIndex = 1 } // Voltar ao Menu Principal
                    1 -> { currentScreen = ScreenState.THEMES; focusedIndex = 0 } // Galeria de Molduras
                    2 -> { isRgbEnabled = !isRgbEnabled }                          // Efeito RGB
                    3 -> { currentScreen = ScreenState.ABOUT; focusedIndex = 0 }  // Sobre & Criador
                }
            }
            ScreenState.THEMES -> {
                if (focusedIndex == 0) {
                    currentScreen = ScreenState.SETTINGS
                    focusedIndex = 1
                } else {
                    val themeIdx = focusedIndex - 1
                    if (themeIdx in ThemeStyle.entries.indices) {
                        selectedTheme = ThemeStyle.entries[themeIdx]
                    }
                }
            }
            ScreenState.ABOUT -> { currentScreen = ScreenState.SETTINGS; focusedIndex = 3 }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = alphaAnim
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            val frameWidth = maxWidth
            val frameHeight = frameWidth / 1.227f

            Box(
                modifier = Modifier
                    .width(frameWidth)
                    .height(frameHeight),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = selectedTheme.imageResId),
                    contentDescription = selectedTheme.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Telinha do Aparelho com Moldura Néon Reativa RPM
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = frameHeight * 0.185f)
                        .width(frameWidth * 0.68f)
                        .height(frameHeight * 0.48f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0C121D).copy(alpha = 0.88f),
                                    Color(0xFF05080F).copy(alpha = 0.88f)
                                )
                            )
                        )
                        .border(
                            width = 3.5.dp,
                            color = activeAccentColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(16.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        activeAccentColor.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (currentScreen) {
                            ScreenState.HOME -> HomeScreen(
                                activeColor = activeAccentColor,
                                focusedIndex = focusedIndex,
                                onNavigate = { screen ->
                                    triggerHaptic()
                                    currentScreen = screen
                                    focusedIndex = 0
                                }
                            )

                            ScreenState.SPEEDOMETER -> SpeedometerScreen(
                                speedKmH = speedKmH,
                                distanceKm = distanceKm,
                                movingTimeMillis = movingTimeMillis,
                                activeColor = activeAccentColor,
                                focusedIndex = focusedIndex,
                                onNavigateBack = {
                                    triggerHaptic()
                                    currentScreen = ScreenState.HOME
                                    focusedIndex = 0
                                },
                                onResetDistance = {
                                    triggerHaptic()
                                    distanceKm = 0f
                                    movingTimeMillis = 0L
                                    NormalWidgetProvider.updateAllWidgets(context, 0f, 0f)
                                    CoverScreenWidgetProvider.updateAllCoverWidgets(context, 0f)
                                }
                            )

                            ScreenState.SETTINGS -> SettingsScreen(
                                isRgbEnabled = isRgbEnabled,
                                activeColor = activeAccentColor,
                                focusedIndex = focusedIndex,
                                onNavigateBack = {
                                    triggerHaptic()
                                    currentScreen = ScreenState.HOME
                                    focusedIndex = 1
                                },
                                onRgbToggle = {
                                    triggerHaptic()
                                    isRgbEnabled = !isRgbEnabled
                                },
                                onNavigate = {
                                    triggerHaptic()
                                    currentScreen = it
                                }
                            )

                            ScreenState.THEMES -> ThemesGalleryScreen(
                                selectedTheme = selectedTheme,
                                focusedIndex = focusedIndex,
                                onNavigateBack = {
                                    triggerHaptic()
                                    currentScreen = ScreenState.SETTINGS
                                    focusedIndex = 1
                                },
                                onThemeSelect = { theme ->
                                    triggerHaptic()
                                    selectedTheme = theme
                                }
                            )

                            ScreenState.ABOUT -> AboutScreen(
                                activeColor = activeAccentColor,
                                focusedIndex = focusedIndex,
                                onNavigateBack = {
                                    triggerHaptic()
                                    currentScreen = ScreenState.SETTINGS
                                    focusedIndex = 3
                                }
                            )
                        }
                    }
                }

                // Botões físicos na parte inferior do aparelho
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = frameHeight * 0.132f)
                        .width(frameWidth * 0.51f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(frameWidth * 0.145f)
                            .height(frameHeight * 0.10f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPhysicalLeftClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "◀",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAccentColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(frameWidth * 0.145f)
                            .height(frameHeight * 0.10f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPhysicalCenterClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF66)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(frameWidth * 0.145f)
                            .height(frameHeight * 0.10f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPhysicalRightClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▶",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAccentColor
                        )
                    }
                }
            }
        }
    }
}

// Menu Principal Enquadrado: 1. Velocímetro | 2. Configurações
@Composable
fun HomeScreen(
    activeColor: Color,
    focusedIndex: Int,
    onNavigate: (ScreenState) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuButton(
                title = "🏎️ Velocímetro",
                subtitle = "Medição de velocidade e distância",
                color = activeColor,
                isFocused = focusedIndex == 0,
                onClick = { onNavigate(ScreenState.SPEEDOMETER) }
            )

            MenuButton(
                title = "⚙️ Configurações", // TEXTO AJUSTADO: Apenas "Configurações"
                subtitle = "Molduras, efeito RGB e sobre o criador",
                color = Color(0xFF00F0FF),
                isFocused = focusedIndex == 1,
                onClick = { onNavigate(ScreenState.SETTINGS) }
            )
        }
    }
}

@Composable
fun MenuButton(
    title: String,
    subtitle: String,
    color: Color,
    isFocused: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isFocused) Color(0xFF1E2A3A) else Color(0xFF0F1623))
            .border(
                width = if (isFocused) 1.5.dp else 0.8.dp,
                color = if (isFocused) Color(0xFFFFD700) else color,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) Color(0xFFFFD700) else color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 7.5.sp,
            color = Color.LightGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CompactHeaderButton(
    title: String,
    color: Color,
    isFocused: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color(0xFF1C283A) else Color(0xFF0F1623))
            .border(
                width = if (isFocused) 1.2.dp else 0.8.dp,
                color = if (isFocused) Color(0xFFFFD700) else color,
                shape = RoundedCornerShape(3.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) Color(0xFFFFD700) else color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SpeedometerScreen(
    speedKmH: Float,
    distanceKm: Float,
    movingTimeMillis: Long,
    activeColor: Color,
    focusedIndex: Int,
    onNavigateBack: () -> Unit,
    onResetDistance: () -> Unit
) {
    val hours = (movingTimeMillis / 3600000L)
    val minutes = (movingTimeMillis % 3600000L) / 60000L
    val seconds = (movingTimeMillis % 60000L) / 1000L
    val formattedTime = "%02d:%02d:%02d".format(hours, minutes, seconds)
    val isMoving = speedKmH > 0.5f

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                CompactHeaderButton(
                    title = "◀ Voltar",
                    color = Color(0xFFFFD700),
                    isFocused = focusedIndex == 0,
                    onClick = onNavigateBack
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                CompactHeaderButton(
                    title = "🔄 Zerar KM",
                    color = Color(0xFFFF3333),
                    isFocused = focusedIndex == 1,
                    onClick = onResetDistance
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Painel do Velocímetro com Horas Dirigidas e Distância no Lado DIREITO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF080C14), RoundedCornerShape(6.dp))
                .border(1.5.dp, activeColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.weight(1.05f),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${speedKmH.toInt()}",
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Black,
                    color = activeColor,
                    letterSpacing = (-3).sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "km/h",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F1726))
                    .border(1.dp, activeColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column {
                    Text(
                        text = "DISTÂNCIA",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "%.2f km".format(distanceKm),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD700),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column {
                    Text(
                        text = if (isMoving) "▶ DIRIGINDO" else "⏸ PAUSADO",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMoving) Color(0xFF00FF66) else Color(0xFFFF8800),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00F0FF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Menu de Configurações com a Opção "Sobre & Criador" 100% Visível e Acessível
@Composable
fun SettingsScreen(
    isRgbEnabled: Boolean,
    activeColor: Color,
    focusedIndex: Int,
    onNavigateBack: () -> Unit,
    onRgbToggle: () -> Unit,
    onNavigate: (ScreenState) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CompactHeaderButton(
            title = "◀ Voltar ao Menu Principal",
            color = Color(0xFFFFD700),
            isFocused = focusedIndex == 0,
            onClick = onNavigateBack
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            MenuButton(
                title = "🎨 Galeria de Molduras",
                subtitle = "Escolha o estilo de borda do aparelho",
                color = activeColor,
                isFocused = focusedIndex == 1,
                onClick = { onNavigate(ScreenState.THEMES) }
            )

            MenuButton(
                title = if (isRgbEnabled) "🌈 Efeito RGB: [ATIVADO]" else "🌈 Efeito RGB: [DESATIVADO]",
                subtitle = "Alterna o ciclo de cores brilhantes",
                color = if (isRgbEnabled) Color(0xFF00FF66) else Color.Gray,
                isFocused = focusedIndex == 2,
                onClick = onRgbToggle
            )

            // SEÇÃO SOBRE & CRIADOR GARANTIDA DENTRO DAS CONFIGURAÇÕES
            MenuButton(
                title = "ℹ️ Sobre & Criador",
                subtitle = "Informações do desenvolvedor e doações",
                color = Color(0xFF00F0FF),
                isFocused = focusedIndex == 3,
                onClick = { onNavigate(ScreenState.ABOUT) }
            )
        }
    }
}

@Composable
fun ThemesGalleryScreen(
    selectedTheme: ThemeStyle,
    focusedIndex: Int,
    onNavigateBack: () -> Unit,
    onThemeSelect: (ThemeStyle) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CompactHeaderButton(
            title = "◀ Voltar às Configurações",
            color = Color(0xFFFFD700),
            isFocused = focusedIndex == 0,
            onClick = onNavigateBack
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ThemeStyle.entries.forEachIndexed { index, theme ->
                val isSelected = selectedTheme == theme
                val itemIndex = index + 1
                val isFocused = focusedIndex == itemIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFocused) Color(0xFF1E2A3A) else Color(0xFF0F1623))
                        .border(
                            width = if (isFocused) 1.5.dp else 0.8.dp,
                            color = if (isFocused) Color(0xFFFFD700) else if (isSelected) Color(0xFF00FF66) else Color.Gray,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onThemeSelect(theme) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = theme.title,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF00FF66) else Color.White
                    )

                    if (isSelected) {
                        Text(
                            text = "✓ ATIVO",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF66)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    activeColor: Color,
    focusedIndex: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val paypalUrl = "https://www.paypal.com/donate/?hosted_button_id=REPB3R2VW4AVA"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CompactHeaderButton(
            title = "◀ Voltar às Configurações",
            color = Color(0xFFFFD700),
            isFocused = focusedIndex == 0,
            onClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFF0F1623), RoundedCornerShape(6.dp))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.commando),
                contentDescription = "Commando Soldier Animation",
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Eagle Uno v1.4",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00F0FF)
            )

            Text(
                text = "Desenvolvido por Eagle Uno Team",
                fontSize = 8.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paypalUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC439)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                Text(
                    text = "Apoiar via PayPal",
                    color = Color(0xFF003087),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}
