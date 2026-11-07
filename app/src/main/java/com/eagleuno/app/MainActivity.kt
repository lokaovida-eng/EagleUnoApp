package com.eagleuno.app

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eagleuno.app.ui.theme.MyApplicationTheme
import com.eagleuno.app.ui.theme.*
import kotlinx.coroutines.delay

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
                    color = Color.Black
                ) {
                    CyberpunkDashboardScreen()
                }
            }
        }
    }
}

@Composable
fun CyberpunkDashboardScreen() {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    // Metric States
    var speedKmH by remember { mutableFloatStateOf(0f) }
    var distanceKm by remember { mutableFloatStateOf(0.00f) }
    var movingTimeMillis by remember { mutableLongStateOf(0L) }
    var previousSpeed by remember { mutableFloatStateOf(0f) }
    var accelerationRate by remember { mutableFloatStateOf(0f) }
    var isAccelerating by remember { mutableStateOf(false) }
    var isDecelerating by remember { mutableStateOf(false) }
    var compassDirection by remember { mutableStateOf("N") }

    // Driving Timer: Runs when moving (> 0.5 km/h)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            if (speedKmH > 0.5f) {
                movingTimeMillis += 1000L
            }
        }
    }

    // Calculate Acceleration Rate & Speed Change Direction
    LaunchedEffect(speedKmH) {
        val diff = speedKmH - previousSpeed
        accelerationRate = diff
        isAccelerating = diff > 0.3f
        isDecelerating = diff < -0.3f
        previousSpeed = speedKmH
    }

    // Dynamic Speed Color based on Speed Range (Cyan -> Green -> Yellow -> Orange -> Red)
    val currentSpeedColor by animateColorAsState(
        targetValue = getSpeedColor(speedKmH),
        animationSpec = tween(durationMillis = 400),
        label = "SpeedColorAnimation"
    )

    // Target RPM / Speed Domino Bar Progress:
    // Direct speed mapping (0 to 160 km/h).
    // Advances left-to-right when accelerating (domino effect),
    // recedes right-to-left when decelerating,
    // and stays locked at current position when cruising/steady speed.
    val maxSpeedForBar = 160f
    val targetRpmProgress = remember(speedKmH) {
        if (speedKmH <= 0.5f) {
            0.03f // Slight idle light indicator
        } else {
            (speedKmH / maxSpeedForBar).coerceIn(0.04f, 1.0f)
        }
    }

    // Smooth domino-style transition for light bar fill
    val animatedRpmProgress by animateFloatAsState(
        targetValue = targetRpmProgress,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "RpmBarFill"
    )

    // Sensor Listener for Compass Heading
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val orientationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    val azimuth = event.values[0]
                    compassDirection = getCardinalDirection(azimuth)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && orientationSensor != null) {
            sensorManager.registerListener(sensorListener, orientationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(sensorListener)
        }
    }

    // GPS Sensor Listener
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        var lastLocation: Location? = null

        val locationListener = object : LocationListener {
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

    fun resetMetrics() {
        triggerHaptic()
        distanceKm = 0f
        movingTimeMillis = 0L
        NormalWidgetProvider.updateAllWidgets(context, 0f, 0f)
        CoverScreenWidgetProvider.updateAllCoverWidgets(context, 0f)
    }

    // Format Trip Time as HH:MM:SS
    val hours = (movingTimeMillis / 3600000L)
    val minutes = (movingTimeMillis % 3600000L) / 60000L
    val seconds = (movingTimeMillis % 60000L) / 1000L
    val formattedTime = "%02d:%02d:%02d".format(hours, minutes, seconds)

    // Fullscreen Cyberpunk Outer Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main Dashboard Card (Optimized for Flip Cover Screen & Compact Displays)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF03060B))
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00FFCC).copy(alpha = 0.5f),
                            Color(0xFFFF0055).copy(alpha = 0.3f),
                            Color(0xFF00FFCC).copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. HEADER MINIMALISTA
                HeaderBar(
                    compassDirection = compassDirection,
                    onBackClick = {
                        triggerHaptic()
                        (context as? Activity)?.finish()
                    },
                    onResetClick = { resetMetrics() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. CENTRAL SECTION (DIVISÃO EM DOIS MÓDULOS)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // MÓDULO ESQUERDO (VELOCÍMETRO GIGANTE COM COR DINÂMICA)
                    Column(
                        modifier = Modifier.weight(1.1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${speedKmH.toInt()}",
                            fontSize = 78.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = currentSpeedColor,
                            letterSpacing = (-2).sp,
                            modifier = Modifier.graphicsLayer {
                                shadowElevation = 10.dp.toPx()
                            }
                        )
                        Text(
                            text = "km/h",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = currentSpeedColor
                        )
                    }

                    // LINHA VERTICAL NEON PONTILLADA
                    DottedDivider(
                        modifier = Modifier
                            .height(88.dp)
                            .width(2.dp)
                    )

                    // MÓDULO DIREITO (MÉTRICAS DA ESTRADA)
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .padding(start = 12.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Linha 1: Distância
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            RoadIcon(
                                modifier = Modifier.size(28.dp),
                                color = Color(0xFF00FFCC)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "%.2f km".format(distanceKm).replace('.', ','),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC)
                            )
                        }

                        // Linha 2: Duração
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            StopwatchIcon(
                                modifier = Modifier.size(28.dp),
                                color = Color(0xFF00FFCC)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedTime,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFCC)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. BARRA DE PERFORMANCE INFERIOR (RPM SPEED DOMINO BAR)
                NeonRpmBar(
                    progress = animatedRpmProgress,
                    isAccelerating = isAccelerating,
                    isDecelerating = isDecelerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// COMPONENTES VETORIAIS E GRÁFICOS PERSONALIZADOS
// ---------------------------------------------------------------------------

fun getSpeedColor(speedKmH: Float): Color {
    return when {
        speedKmH < 30f -> Color(0xFF00FFCC) // Ciano Neon (Baixa velocidade / Urbano)
        speedKmH < 60f -> Color(0xFF00FF66) // Verde Neon (Velocidade média)
        speedKmH < 90f -> Color(0xFFFFCC00) // Amarelo Neon (Velocidade cruzeiro)
        speedKmH < 120f -> Color(0xFFFF6600) // Laranja Neon (Rodovia)
        else -> Color(0xFFFF0055)            // Vermelho Elétrico / Rosa (Alta velocidade)
    }
}

@Composable
fun HeaderBar(
    compassDirection: String,
    onBackClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Canto Esquerdo: Botão chevron '<'
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "<",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        // Centro: Bússola HUD [ 🧭 N ]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "[ ",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
            CompassIcon(
                modifier = Modifier.size(16.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = compassDirection,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = " ]",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Canto Direito: Botão Zerar KM (Circular Loop Arrow)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable { onResetClick() },
            contentAlignment = Alignment.Center
        ) {
            ResetIcon(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF00FFCC)
            )
        }
    }
}

@Composable
fun DottedDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeW = 1.8.dp.toPx()
        val dashH = 4.dp.toPx()
        val gapH = 4.dp.toPx()
        var currentY = 0f

        while (currentY < size.height) {
            drawLine(
                color = Color(0xFF00FFCC).copy(alpha = 0.5f),
                start = Offset(size.width / 2f, currentY),
                end = Offset(size.width / 2f, (currentY + dashH).coerceAtMost(size.height)),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            currentY += dashH + gapH
        }
    }
}

@Composable
fun NeonRpmBar(
    progress: Float,
    isAccelerating: Boolean = false,
    isDecelerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val totalSegments = 24
        val gap = 3.dp.toPx()
        val slant = h * 0.45f // Corte chanfrado diagonal cyberpunk

        val availableWidth = w - (totalSegments - 1) * gap
        val segmentWidth = availableWidth / totalSegments
        val currentActiveFraction = progress.coerceIn(0f, 1f) * totalSegments

        // Trilha de fundo escura
        val trackPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w - slant, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = trackPath,
            color = Color(0xFF040810)
        )
        drawPath(
            path = trackPath,
            color = Color(0xFF00FFCC).copy(alpha = 0.2f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Desenho dos blocos de LED estilo Dominó (Esquerda para Direita)
        for (i in 0 until totalSegments) {
            val startX = i * (segmentWidth + gap)
            val endX = startX + segmentWidth

            // Cor dinâmica do segmento (Ciano -> Verde -> Amarelo -> Laranja -> Vermelho Rosa Neon)
            val t = i.toFloat() / (totalSegments - 1).coerceAtLeast(1)
            val segmentColor = when {
                t < 0.25f -> Color(0xFF00FFCC) // Ciano Neon
                t < 0.50f -> Color(0xFF00FF66) // Verde Neon
                t < 0.72f -> Color(0xFFFFCC00) // Amarelo Neon
                t < 0.88f -> Color(0xFFFF6600) // Laranja Neon
                else -> Color(0xFFFF0055)      // Rosa Vermelho Elétrico
            }

            val activeSegmentIndex = currentActiveFraction.toInt()
            val isLit = i < activeSegmentIndex
            val isPartial = i == activeSegmentIndex
            val partialAlpha = if (isPartial) (currentActiveFraction - activeSegmentIndex) else 0f

            val segSlantLeft = if (i == 0) 0f else slant * 0.25f
            val segSlantRight = if (i == totalSegments - 1) slant else slant * 0.25f

            val segPath = Path().apply {
                moveTo(startX + segSlantLeft, 0f)
                lineTo(endX, 0f)
                lineTo(endX + segSlantRight, h)
                lineTo(startX, h)
                close()
            }

            if (isLit) {
                // Dominó aceso (Aceleração / Velocidade subindo)
                val isLeadingEdge = (i == activeSegmentIndex - 1)
                val strokeAlpha = if (isLeadingEdge && isAccelerating) 0.9f else 0.4f

                drawPath(path = segPath, color = segmentColor)
                drawPath(
                    path = segPath,
                    color = Color.White.copy(alpha = strokeAlpha),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            } else if (isPartial && partialAlpha > 0.05f) {
                // Transição suave do dominó acendendo/apagando
                drawPath(path = segPath, color = segmentColor.copy(alpha = partialAlpha))
                if (isAccelerating) {
                    drawPath(
                        path = segPath,
                        color = Color.White.copy(alpha = partialAlpha * 0.8f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            } else {
                // Dominó apagado (Trilha translúcida)
                drawPath(path = segPath, color = segmentColor.copy(alpha = 0.12f))
                drawPath(
                    path = segPath,
                    color = Color.White.copy(alpha = 0.22f),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun RoadIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF00FFCC)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.5.dp.toPx()

        // Linha esquerda convergente
        drawLine(
            color = color,
            start = Offset(w * 0.42f, 0f),
            end = Offset(w * 0.15f, h),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Linha direita convergente
        drawLine(
            color = color,
            start = Offset(w * 0.58f, 0f),
            end = Offset(w * 0.85f, h),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Tracejado central
        val dashH = h * 0.2f
        val gapH = h * 0.12f
        var currentY = h * 0.08f
        while (currentY + dashH <= h * 0.95f) {
            drawLine(
                color = color,
                start = Offset(w * 0.5f, currentY),
                end = Offset(w * 0.5f, currentY + dashH),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            currentY += dashH + gapH
        }
    }
}

@Composable
fun StopwatchIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF00FFCC)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.2.dp.toPx()
        val center = Offset(w / 2f, h * 0.56f)
        val radius = minOf(w, h) * 0.36f

        // Botão superior do cronômetro
        drawLine(
            color = color,
            start = Offset(w / 2f, h * 0.04f),
            end = Offset(w / 2f, h * 0.18f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.35f, h * 0.04f),
            end = Offset(w * 0.65f, h * 0.04f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Botão lateral
        drawLine(
            color = color,
            start = Offset(w * 0.72f, h * 0.22f),
            end = Offset(w * 0.82f, h * 0.3f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Círculo principal
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeW)
        )

        // Ponteiros de hora e minuto
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + radius * 0.42f, center.y + radius * 0.22f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = 2.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun ResetIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF00FFCC)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.2.dp.toPx()
        val center = Offset(w / 2f, h / 2f)
        val radius = minOf(w, h) * 0.38f

        drawArc(
            color = color,
            startAngle = 40f,
            sweepAngle = 280f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Seta na ponta do arco
        val arrowPath = Path().apply {
            moveTo(w * 0.76f, h * 0.12f)
            lineTo(w * 0.88f, h * 0.32f)
            lineTo(w * 0.62f, h * 0.32f)
            close()
        }
        drawPath(path = arrowPath, color = color)
    }
}

@Composable
fun CompassIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8.dp.toPx()
        val center = Offset(w / 2f, h / 2f)
        val radius = minOf(w, h) * 0.42f

        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeW)
        )

        // Agulha apontando para o Norte
        val needlePath = Path().apply {
            moveTo(center.x, center.y - radius * 0.75f)
            lineTo(center.x - radius * 0.3f, center.y + radius * 0.35f)
            lineTo(center.x + radius * 0.3f, center.y + radius * 0.35f)
            close()
        }
        drawPath(path = needlePath, color = color)
    }
}

fun getCardinalDirection(azimuth: Float): String {
    val degrees = (azimuth % 360 + 360) % 360
    return when {
        degrees >= 337.5 || degrees < 22.5 -> "N"
        degrees >= 22.5 && degrees < 67.5 -> "NE"
        degrees >= 67.5 && degrees < 112.5 -> "E"
        degrees >= 112.5 && degrees < 157.5 -> "SE"
        degrees >= 157.5 && degrees < 202.5 -> "S"
        degrees >= 202.5 && degrees < 247.5 -> "SW"
        degrees >= 247.5 && degrees < 292.5 -> "W"
        degrees >= 292.5 && degrees < 337.5 -> "NW"
        else -> "N"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun CyberpunkDashboardScreenPreview() {
    MyApplicationTheme {
        CyberpunkDashboardScreen()
    }
}

