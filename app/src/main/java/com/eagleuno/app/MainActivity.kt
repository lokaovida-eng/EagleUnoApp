package com.eagleuno.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.eagleuno.app.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

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
    SCREENSAVER,
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
                    color = Color(0xFF05080F)
                ) {
                    EagleUnoApp()
                }
            }
        }
    }
}

// Componente para Reprodução Suave de GIFs (Totalmente seguro contra falhas)
@Composable
fun AnimatedGifImage(
    gifResId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }

    if (hasError) {
        Box(
            modifier = modifier.background(Color(0xFF0F1724)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contentDescription ?: "",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    } else {
        val imageLoader = remember(context) {
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }

        val model = remember(gifResId, context) {
            ImageRequest.Builder(context)
                .data("android.resource://${context.packageName}/$gifResId")
                .build()
        }

        AsyncImage(
            model = model,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            onError = { e ->
                e.result.throwable.printStackTrace()
                hasError = true
            }
        )
    }
}

// Renderização limpa e segura da moldura do dispositivo
@Composable
fun EagleUnoApp() {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(ThemeStyle.VERMELHO_METAL) }
    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
    var isRgbEnabled by remember { mutableStateOf(false) }

    // Animação de Transição Mágica (Alinhamento perfeito com o Widget)
    var isAppVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isAppVisible = true
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isAppVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "SeamlessAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (isAppVisible) 1f else 0.97f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "SeamlessScale"
    )

    // Estado dos Botões Físicos
    var focusedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentScreen) {
        focusedIndex = 0
    }

    // Estados do Velocímetro e Distância
    var speedKmH by remember { mutableFloatStateOf(0f) }
    var distanceKm by remember { mutableFloatStateOf(0.00f) }
    var movingTimeMillis by remember { mutableLongStateOf(0L) }

    // Cronômetro que RODA em Segundo Plano APENAS com Velocidade > 0.5 km/h
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            if (speedKmH > 0.5f) {
                movingTimeMillis += 1000L
            }
        }
    }

    val isAbove35 = speedKmH > 35f
    val isMoving = speedKmH > 0.5f

    // Animações de Cores
    val infiniteTransition = rememberInfiniteTransition(label = "NeonGlow")

    val breathingColor by infiniteTransition.animateColor(
        initialValue = selectedTheme.primaryColor,
        targetValue = Color(0xFF00E5FF),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingAnimation"
    )

    val rainbowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF0033),
        targetValue = Color(0xFF00E5FF),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RainbowAnimation"
    )

    val redNeonColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF0033),
        targetValue = Color(0xFFFF5566),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RedNeonAnimation"
    )

    val activeAccentColor = when {
        isAbove35 -> redNeonColor
        isMoving || isRgbEnabled -> rainbowColor
        else -> breathingColor
    }

    // GPS Sensor Listener Seguro
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
                    1000L,
                    1f,
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

    fun onPhysicalLeftClick() {
        when (currentScreen) {
            ScreenState.HOME -> focusedIndex = (focusedIndex - 1 + 3) % 3
            ScreenState.SPEEDOMETER -> focusedIndex = (focusedIndex - 1 + 2) % 2
            ScreenState.SCREENSAVER -> { currentScreen = ScreenState.HOME; focusedIndex = 1 }
            ScreenState.SETTINGS -> focusedIndex = (focusedIndex - 1 + 4) % 4
            ScreenState.THEMES -> {
                val total = ThemeStyle.entries.size + 1
                focusedIndex = (focusedIndex - 1 + total) % total
            }
            ScreenState.ABOUT -> { currentScreen = ScreenState.SETTINGS; focusedIndex = 3 }
        }
    }

    fun onPhysicalRightClick() {
        when (currentScreen) {
            ScreenState.HOME -> focusedIndex = (focusedIndex + 1) % 3
            ScreenState.SPEEDOMETER -> focusedIndex = (focusedIndex + 1) % 2
            ScreenState.SCREENSAVER -> { currentScreen = ScreenState.HOME; focusedIndex = 1 }
            ScreenState.SETTINGS -> focusedIndex = (focusedIndex + 1) % 4
            ScreenState.THEMES -> {
                val total = ThemeStyle.entries.size + 1
                focusedIndex = (focusedIndex + 1) % total
            }
            ScreenState.ABOUT -> { currentScreen = ScreenState.SETTINGS; focusedIndex = 3 }
        }
    }

    fun onPhysicalCenterClick() {
        when (currentScreen) {
            ScreenState.HOME -> {
                when (focusedIndex) {
                    0 -> { currentScreen = ScreenState.SPEEDOMETER; focusedIndex = 0 }
                    1 -> { currentScreen = ScreenState.SCREENSAVER; focusedIndex = 0 }
                    2 -> { currentScreen = ScreenState.SETTINGS; focusedIndex = 0 }
                }
            }
            ScreenState.SPEEDOMETER -> {
                if (focusedIndex == 0) {
                    currentScreen = ScreenState.HOME
                    focusedIndex = 0
                } else {
                    distanceKm = 0f
                    movingTimeMillis = 0L
                }
            }
            ScreenState.SCREENSAVER -> { currentScreen = ScreenState.HOME; focusedIndex = 1 }
            ScreenState.SETTINGS -> {
                when (focusedIndex) {
                    0 -> { currentScreen = ScreenState.HOME; focusedIndex = 2 }
                    1 -> { currentScreen = ScreenState.THEMES; focusedIndex = 0 }
                    2 -> isRgbEnabled = !isRgbEnabled
                    3 -> { currentScreen = ScreenState.ABOUT; focusedIndex = 0 }
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
                                    Color(0xFF0C121D),
                                    Color(0xFF05080F)
                                )
                            )
                        )
                        .border(
                            width = 2.2.dp,
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
                                        activeAccentColor.copy(alpha = 0.45f),
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
                                    when (screen) {
                                        ScreenState.SPEEDOMETER -> focusedIndex = 0
                                        ScreenState.SCREENSAVER -> focusedIndex = 0
                                        ScreenState.SETTINGS -> focusedIndex = 0
                                        else -> focusedIndex = 0
                                    }
                                    currentScreen = screen
                                }
                            )

                            ScreenState.SPEEDOMETER -> SpeedometerScreen(
                                speedKmH = speedKmH,
                                distanceKm = distanceKm,
                                movingTimeMillis = movingTimeMillis,
                                activeColor = activeAccentColor,
                                focusedIndex = focusedIndex,
                                onNavigateBack = {
                                    currentScreen = ScreenState.HOME
                                    focusedIndex = 0
                                },
                                onResetDistance = {
                                    distanceKm = 0f
                                    movingTimeMillis = 0L
                                }
                            )

                            ScreenState.SCREENSAVER -> ScreensaverScreen(
                                activeColor = activeAccentColor,
                                focusedIndex = focusedIndex,
                                onNavigateBack = {
                                    currentScreen = ScreenState.HOME
                                    focusedIndex = 1
                                }
                            )

                            ScreenState.SETTINGS -> SettingsScreen(
                                isRgbEnabled = isRgbEnabled,
                                activeColor = activeAccentColor,
                                focusedIndex = focusedIndex,
                                onNavigateBack = {
                                    currentScreen = ScreenState.HOME
                                    focusedIndex = 2
                                },
                                onRgbToggle = { isRgbEnabled = !isRgbEnabled },
                                onNavigate = { currentScreen = it }
                            )

                            ScreenState.THEMES -> ThemesGalleryScreen(
                                selectedTheme = selectedTheme,
                                focusedIndex = focusedIndex,
                                onNavigateBack = {
                                    currentScreen = ScreenState.SETTINGS
                                    focusedIndex = 1
                                },
                                onThemeSelect = { theme -> selectedTheme = theme }
                            )

                            ScreenState.ABOUT -> AboutScreen(
                                activeColor = activeAccentColor,
                                onNavigateBack = {
                                    currentScreen = ScreenState.SETTINGS
                                    focusedIndex = 3
                                }
                            )
                        }
                    }
                }

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
                            fontSize = 8.sp,
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
                            fontSize = 8.sp,
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
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeAccentColor
                        )
                    }
                }
            }
        }
    }
}

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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MenuButton(
                title = "🏎️ Velocímetro",
                subtitle = "Medição de velocidade e distância percorrida",
                color = activeColor,
                isFocused = focusedIndex == 0,
                onClick = { onNavigate(ScreenState.SPEEDOMETER) }
            )

            MenuButton(
                title = "🎬 Tela de Descanso (GIF)",
                subtitle = "Animação tática do personagem",
                color = Color(0xFF00F0FF),
                isFocused = focusedIndex == 1,
                onClick = { onNavigate(ScreenState.SCREENSAVER) }
            )

            MenuButton(
                title = "⚙️ Configurações",
                subtitle = "Mudar moldura, efeito RGB & sobre",
                color = activeColor,
                isFocused = focusedIndex == 2,
                onClick = { onNavigate(ScreenState.SETTINGS) }
            )
        }
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
            .height(20.dp)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF080C14), RoundedCornerShape(4.dp))
                .border(1.2.dp, activeColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${speedKmH.toInt()}",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = activeColor,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "km/h",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF0F1726))
                    .border(0.8.dp, activeColor.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "DISTÂNCIA",
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = "%.2f km".format(distanceKm),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700)
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (isMoving) "▶ TEMPO" else "⏸ PAUSADO",
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMoving) Color(0xFF00FF66) else Color(0xFFFF8800)
                )
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}

@Composable
fun ScreensaverScreen(
    activeColor: Color,
    focusedIndex: Int,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MenuButton(
            title = "◀ Voltar ao Menu Principal",
            subtitle = "Sair da Tela de Descanso",
            color = Color(0xFFFFD700),
            isFocused = focusedIndex == 0,
            onClick = onNavigateBack
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF05080E))
                .border(1.2.dp, activeColor, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedGifImage(
                gifResId = R.drawable.commando,
                contentDescription = "Tela de Descanso GIF Animado",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun SettingsScreen(
    isRgbEnabled: Boolean,
    activeColor: Color,
    focusedIndex: Int,
    onNavigateBack: () -> Unit,
    onRgbToggle: () -> Unit,
    onNavigate: (ScreenState) -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(focusedIndex) {
        scrollState.animateScrollTo((focusedIndex * 32).coerceAtLeast(0))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        MenuButton(
            title = "◀ Voltar ao Menu Principal",
            subtitle = "Retornar à tela inicial",
            color = Color(0xFFFFD700),
            isFocused = focusedIndex == 0,
            onClick = onNavigateBack
        )

        MenuButton(
            title = "🎨 Mudar Moldura do Aparelho",
            subtitle = "Escolha a cor do corpo do dispositivo",
            color = activeColor,
            isFocused = focusedIndex == 1,
            onClick = { onNavigate(ScreenState.THEMES) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(25.dp)
                .background(
                    if (isRgbEnabled) activeColor.copy(alpha = 0.2f)
                    else if (focusedIndex == 2) Color(0xFF182436)
                    else Color(0xFF101826),
                    RoundedCornerShape(3.dp)
                )
                .border(
                    width = if (isRgbEnabled || focusedIndex == 2) 1.2.dp else 0.8.dp,
                    color = if (focusedIndex == 2) Color(0xFFFFD700) else if (isRgbEnabled) activeColor else Color(0xFF202E42),
                    shape = RoundedCornerShape(3.dp)
                )
                .clickable { onRgbToggle() }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🌈 Efeito de Luzes RGB",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRgbEnabled) activeColor else Color.White
                )
                Text(
                    text = if (isRgbEnabled) "✨ Spectrum Neon Ativo" else "Animação de cores dinâmicas no painel",
                    fontSize = 6.5.sp,
                    color = if (isRgbEnabled) Color.LightGray else Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        if (isRgbEnabled) activeColor else Color(0xFF223044),
                        RoundedCornerShape(2.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (isRgbEnabled) "LIGADO" else "DESLIGADO",
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRgbEnabled) Color.Black else Color.Gray
                )
            }
        }

        MenuButton(
            title = "ℹ️ Sobre o Aplicativo",
            subtitle = "Informações do sistema e apoio PayPal",
            color = Color(0xFFA0B0C0),
            isFocused = focusedIndex == 3,
            onClick = { onNavigate(ScreenState.ABOUT) }
        )
    }
}

@Composable
fun ThemesGalleryScreen(
    selectedTheme: ThemeStyle,
    focusedIndex: Int,
    onNavigateBack: () -> Unit,
    onThemeSelect: (ThemeStyle) -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(focusedIndex) {
        scrollState.animateScrollTo((focusedIndex * 32).coerceAtLeast(0))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        MenuButton(
            title = "◀ Voltar para Configurações",
            subtitle = "Retornar ao menu anterior",
            color = Color(0xFFFFD700),
            isFocused = focusedIndex == 0,
            onClick = onNavigateBack
        )

        ThemeStyle.entries.forEachIndexed { index, theme ->
            val itemIndex = index + 1
            val isSelected = selectedTheme == theme
            val isFocused = focusedIndex == itemIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isSelected) theme.primaryColor.copy(alpha = 0.25f)
                        else if (isFocused) Color(0xFF1A2638)
                        else Color(0xFF101724)
                    )
                    .border(
                        width = if (isFocused || isSelected) 1.2.dp else 0.5.dp,
                        color = if (isFocused) Color(0xFFFFD700) else if (isSelected) theme.primaryColor else Color(0xFF202C3D),
                        shape = RoundedCornerShape(3.dp)
                    )
                    .clickable { onThemeSelect(theme) }
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp, 24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, theme.primaryColor, RoundedCornerShape(2.dp))
                ) {
                    Image(
                        painter = painterResource(id = theme.imageResId),
                        contentDescription = theme.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = theme.title,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) theme.primaryColor else Color(0xFF1E2B3C),
                            RoundedCornerShape(2.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isSelected) "✔ Selecionado" else "Usar",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    activeColor: Color,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    fun openPayPal() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/eagleuno")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MenuButton(
            title = "◀ Voltar para Configurações",
            subtitle = "Retornar ao menu anterior",
            color = Color(0xFFFFD700),
            isFocused = true,
            onClick = onNavigateBack
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1724), RoundedCornerShape(4.dp))
                .border(0.5.dp, activeColor, RoundedCornerShape(4.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp, 50.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .border(0.5.dp, activeColor, RoundedCornerShape(3.dp))
            ) {
                AnimatedGifImage(
                    gifResId = R.drawable.commando,
                    contentDescription = "Eagle Uno Commando GIF",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Eagle Uno v1.3",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColor
                )
                Text(
                    text = "Velocímetro GPS & Descanso",
                    fontSize = 6.5.sp,
                    color = Color.Gray
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A1608))
                .border(0.8.dp, Color(0xFFFFD700), RoundedCornerShape(4.dp))
                .clickable { openPayPal() }
                .padding(5.dp)
        ) {
            Text(
                text = "💛 APOIE O PROJETO VIA PAYPAL (CLIQUE AQUI):",
                fontSize = 6.5.sp,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "paypal.me/eagleuno 🔗",
                fontSize = 9.sp,
                color = Color(0xFF00F0FF),
                fontWeight = FontWeight.ExtraBold
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(25.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isFocused) Color(0xFF1A2638) else Color(0xFF0E1522))
            .border(
                width = if (isFocused) 1.2.dp else 0.8.dp,
                color = if (isFocused) Color(0xFFFFD700) else color,
                shape = RoundedCornerShape(3.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 6.5.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = if (isFocused) "👈 OK" else "▶",
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) Color(0xFFFFD700) else color
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun EagleUnoPreview() {
    MyApplicationTheme {
        EagleUnoApp()
    }
}
