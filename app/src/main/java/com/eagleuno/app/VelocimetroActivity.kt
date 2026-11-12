package com.eagleuno.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eagleuno.app.ui.theme.MyApplicationTheme
import com.eagleuno.app.ui.theme.*
import kotlinx.coroutines.delay

class VelocimetroActivity : ComponentActivity() {
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

    var speedKmH by remember { mutableFloatStateOf(0f) }
    var distanceKm by remember { mutableDoubleStateOf(0.000) }
    var movingTimeMillis by remember { mutableLongStateOf(0L) }
    var previousSpeed by remember { mutableFloatStateOf(0f) }
    var accelerationRate by remember { mutableFloatStateOf(0f) }
    var isAccelerating by remember { mutableStateOf(false) }
    var isDecelerating by remember { mutableStateOf(false) }
    var compassDirection by remember { mutableStateOf("N") }
    var showAboutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (speedKmH > 0.5f) {
                movingTimeMillis += 1000L
            }
        }
    }

    var lastSpeedTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(speedKmH) {
        val now = System.currentTimeMillis()
        val dt = ((now - lastSpeedTime) / 1000f).coerceAtLeast(0.05f)
        val diff = speedKmH - previousSpeed
        accelerationRate = diff / dt
        isAccelerating = diff > 0.2f
        isDecelerating = diff < -0.2f
        previousSpeed = speedKmH
        lastSpeedTime = now
    }

    val animatedSpeedKmH by animateFloatAsState(
        targetValue = speedKmH,
        animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing),
        label = "SpeedReadoutAnimation"
    )

    val currentSpeedColor by animateColorAsState(
        targetValue = getSpeedColor(speedKmH),
        animationSpec = tween(durationMillis = 200),
        label = "SpeedColorAnimation"
    )

    val maxSpeedForBar = 160f
    val targetRpmProgress = remember(speedKmH, isAccelerating, isDecelerating, accelerationRate) {
        val baseRatio = (speedKmH / maxSpeedForBar).coerceIn(0.04f, 1.0f)
        if (speedKmH <= 0.5f) {
            if (isAccelerating) 0.35f else 0.04f
        } else if (isAccelerating) {
            (baseRatio + 0.38f + (accelerationRate * 0.04f)).coerceIn(0.40f, 1.0f)
        } else if (isDecelerating) {
            (baseRatio - 0.08f).coerceIn(0.04f, 1.0f)
        } else {
            baseRatio
        }
    }

    val animatedRpmProgress by animateFloatAsState(
        targetValue = targetRpmProgress,
        animationSpec = tween(
            durationMillis = if (isAccelerating || isDecelerating) 90 else 150,
            easing = FastOutSlowInEasing
        ),
        label = "SnappyRpmBarFill"
    )

    val isPreview = LocalInspectionMode.current

    DisposableEffect(isPreview) {
        if (isPreview) {
            onDispose { }
        } else {
            var sensorManager: SensorManager? = null
            var sensorListener: SensorEventListener? = null
            try {
                sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                @Suppress("DEPRECATION")
                val orientationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

                sensorListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event != null) {
                            val azimuth = event.values[0]
                            compassDirection = getCardinalDirection(azimuth)
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                if (sensorManager != null && orientationSensor != null) {
                    sensorManager.registerListener(sensorListener, orientationSensor, SensorManager.SENSOR_DELAY_UI)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            onDispose {
                try {
                    if (sensorManager != null && sensorListener != null) {
                        sensorManager.unregisterListener(sensorListener)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    DisposableEffect(isPreview) {
        if (isPreview) {
            onDispose { }
        } else {
            var locationManager: LocationManager? = null
            var locationListener: LocationListener? = null
            try {
                locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                var lastLocation: Location? = null
                var lastLocationTimestamp = 0L

                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        try {
                            val now = System.currentTimeMillis()
                            var currentSpeed = 0f

                            if (location.hasSpeed() && location.speed >= 0f) {
                                currentSpeed = location.speed * 3.6f
                            } else if (lastLocation != null && lastLocationTimestamp > 0) {
                                val dtSec = (now - lastLocationTimestamp) / 1000f
                                if (dtSec in 0.2f..5.0f) {
                                    val meters = lastLocation!!.distanceTo(location)
                                    currentSpeed = (meters / dtSec) * 3.6f
                                }
                            }

                            if (currentSpeed < 0.8f && location.hasAccuracy() && location.accuracy > 25f) {
                                currentSpeed = 0f
                            }

                            speedKmH = currentSpeed.coerceIn(0f, 350f)

                            if (lastLocation != null && speedKmH > 0.5f) {
                                val deltaMeters = lastLocation!!.distanceTo(location)
                                val maxAllowedAccuracy = if (location.hasAccuracy()) location.accuracy else 35f
                                if (deltaMeters in 0.3..250.0 && maxAllowedAccuracy < 45f) {
                                    distanceKm += (deltaMeters / 1000.0)
                                }
                            }

                            lastLocation = location
                            lastLocationTimestamp = now
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    @Deprecated("Deprecated in API level 29")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }

                if (locationManager != null &&
                    (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                     context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
                ) {
                    try {
                        @Suppress("MissingPermission")
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            100L,
                            0.0f,
                            locationListener
                        )
                    } catch (_: Exception) {}

                    try {
                        @Suppress("MissingPermission")
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            400L,
                            0.0f,
                            locationListener
                        )
                    } catch (_: Exception) {}
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            onDispose {
                try {
                    if (locationManager != null && locationListener != null) {
                        locationManager.removeUpdates(locationListener)
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
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
        distanceKm = 0.0
        movingTimeMillis = 0L
    }

    val hours = (movingTimeMillis / 3600000L)
    val minutes = (movingTimeMillis % 3600000L) / 60000L
    val seconds = (movingTimeMillis % 60000L) / 1000L
    val formattedTime = "%02d:%02d:%02d".format(hours, minutes, seconds)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03060B))
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00FFCC),
                        Color(0xFFFF0055),
                        Color(0xFF00FFCC)
                    )
                ),
                shape = RectangleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        val isCompactDisplay = this.maxHeight < 450.dp || this.maxWidth < 400.dp
        val speedFontSize = if (isCompactDisplay) 72.sp else 86.sp
        val dividerHeight = if (isCompactDisplay) 76.dp else 92.dp
        val metricsSpacing = if (isCompactDisplay) 8.dp else 14.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(6.dp),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(6.dp),
                    start = 8.dp,
                    end = 8.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderBar(
                compassDirection = compassDirection,
                onBackClick = {
                    triggerHaptic()
                    (context as? Activity)?.finish()
                },
                onResetClick = { resetMetrics() },
                onAboutClick = {
                    triggerHaptic()
                    showAboutDialog = true
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1.15f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NeonSpeedText(
                        speed = "${animatedSpeedKmH.toInt()}",
                        fontSize = speedFontSize,
                        color = currentSpeedColor
                    )
                    Text(
                        text = "km/h",
                        fontSize = if (isCompactDisplay) 14.sp else 16.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace,
                        color = currentSpeedColor
                    )
                }

                DottedDivider(
                    modifier = Modifier
                        .height(dividerHeight)
                        .width(2.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .padding(start = if (isCompactDisplay) 8.dp else 12.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(metricsSpacing)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RoadIcon(
                            modifier = Modifier.size(if (isCompactDisplay) 24.dp else 28.dp),
                            color = Color(0xFF00FFCC)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%.2f km".format(distanceKm).replace('.', ','),
                            fontSize = if (isCompactDisplay) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FFCC)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        StopwatchIcon(
                            modifier = Modifier.size(if (isCompactDisplay) 24.dp else 28.dp),
                            color = Color(0xFF00FFCC)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedTime,
                            fontSize = if (isCompactDisplay) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FFCC)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            NeonRpmBar(
                progress = animatedRpmProgress,
                isAccelerating = isAccelerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCompactDisplay) 18.dp else 22.dp)
                    .padding(horizontal = 2.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))
        }

        if (showAboutDialog) {
            Dialog(onDismissRequest = { showAboutDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF080D18))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00FFCC),
                                    Color(0xFFFF0055)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Velocímetro Automotivo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00FFCC)
                        )

                        Text(
                            text = "v2.0.0 • Alta Precisão GPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Velocímetro digital HUD com moldura neon reativa à aceleração, cronômetro de viagem e suporte a telas dobráveis (Z Flip).",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                triggerHaptic()
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.paypal.com/donate/?hosted_button_id=REPB3R2VW4AVA")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0055)
                            )
                        ) {
                            Text(
                                text = "Apoiar Projeto no PayPal 💳",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        TextButton(
                            onClick = {
                                triggerHaptic()
                                showAboutDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Fechar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeonSpeedText(
    speed: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Text(
            text = speed,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Monospace,
            color = color.copy(alpha = 0.35f),
            letterSpacing = (-2).sp,
            modifier = Modifier.graphicsLayer {
                scaleX = 1.15f
                scaleY = 1.15f
                alpha = 0.45f
            }
        )

        val strokeColor = color.copy(alpha = 0.90f)
        val strokeOffsets = listOf(
            Offset(-2.5f, -2.5f), Offset(2.5f, -2.5f), Offset(-2.5f, 2.5f), Offset(2.5f, 2.5f),
            Offset(-3.5f, 0f), Offset(3.5f, 0f), Offset(0f, -3.5f), Offset(0f, 3.5f)
        )
        for (offset in strokeOffsets) {
            Text(
                text = speed,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Monospace,
                color = strokeColor,
                letterSpacing = (-2).sp,
                modifier = Modifier.offset(x = offset.x.dp, y = offset.y.dp)
            )
        }

        Text(
            text = speed,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            letterSpacing = (-2).sp
        )
    }
}

private fun getSpeedColor(speedKmH: Float): Color {
    return when {
        speedKmH < 30f -> Color(0xFF00FFCC)
        speedKmH < 60f -> Color(0xFF00FF66)
        speedKmH < 90f -> Color(0xFFFFCC00)
        speedKmH < 120f -> Color(0xFFFF6600)
        else -> Color(0xFFFF0055)
    }
}

@Composable
private fun HeaderBar(
    compassDirection: String,
    onBackClick: () -> Unit,
    onResetClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "<",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

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
                color = Color(0xFF00FFCC)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = compassDirection,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            Text(
                text = " ]",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onAboutClick() },
                contentAlignment = Alignment.Center
            ) {
                InfoIcon(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFFFF0055)
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
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
}

@Composable
private fun DottedDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeW = 2.dp.toPx()
        val dashH = 4.dp.toPx()
        val gapH = 4.dp.toPx()
        var currentY = 0f

        while (currentY < size.height) {
            drawLine(
                color = Color(0xFF00FFCC).copy(alpha = 0.6f),
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
private fun NeonRpmBar(
    progress: Float,
    modifier: Modifier = Modifier,
    isAccelerating: Boolean = false
) {
    Canvas(modifier = modifier) {
        val totalSegments = 16
        val gap = 3.dp.toPx()
        val totalWidth = size.width
        val barHeight = size.height
        val segmentWidth = (totalWidth - (gap * (totalSegments - 1))) / totalSegments
        val activeCount = (progress.coerceIn(0f, 1f) * totalSegments).toInt().coerceAtLeast(1)

        for (i in 0 until totalSegments) {
            val startX = i * (segmentWidth + gap)
            val segmentRect = Rect(
                left = startX,
                top = 0f,
                right = startX + segmentWidth,
                bottom = barHeight
            )

            val segmentColor = when {
                i < 5 -> Color(0xFF00FFCC)
                i < 9 -> Color(0xFF00FF66)
                i < 12 -> Color(0xFFFFCC00)
                i < 14 -> Color(0xFFFF6600)
                else -> Color(0xFFFF0055)
            }

            val isActive = i < activeCount

            if (isActive) {
                val alpha = if (isAccelerating) 1.0f else 0.88f

                drawRoundRect(
                    color = segmentColor.copy(alpha = alpha),
                    topLeft = Offset(segmentRect.left, segmentRect.top),
                    size = Size(segmentRect.width, segmentRect.height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                drawRoundRect(
                    color = Color.White.copy(alpha = if (isAccelerating) 0.75f else 0.45f),
                    topLeft = Offset(segmentRect.left + 1.dp.toPx(), segmentRect.top + 2.dp.toPx()),
                    size = Size(segmentRect.width - 2.dp.toPx(), segmentRect.height / 3f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(segmentRect.left, segmentRect.top),
                    size = Size(segmentRect.width, segmentRect.height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            } else {
                drawRoundRect(
                    color = Color(0xFF060D17),
                    topLeft = Offset(segmentRect.left, segmentRect.top),
                    size = Size(segmentRect.width, segmentRect.height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                drawRoundRect(
                    color = segmentColor.copy(alpha = 0.22f),
                    topLeft = Offset(segmentRect.left, segmentRect.top),
                    size = Size(segmentRect.width, segmentRect.height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun RoadIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF00FFCC)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.5.dp.toPx()

        drawLine(
            color = color,
            start = Offset(w * 0.42f, 0f),
            end = Offset(w * 0.15f, h),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(w * 0.58f, 0f),
            end = Offset(w * 0.85f, h),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

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
private fun StopwatchIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF00FFCC)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.2.dp.toPx()
        val center = Offset(w / 2f, h * 0.56f)
        val radius = minOf(w, h) * 0.36f

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

        drawLine(
            color = color,
            start = Offset(w * 0.72f, h * 0.22f),
            end = Offset(w * 0.82f, h * 0.3f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeW)
        )

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
private fun ResetIcon(modifier: Modifier = Modifier, color: Color = Color(0xFF00FFCC)) {
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
private fun CompassIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
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

        val needlePath = Path().apply {
            moveTo(center.x, center.y - radius * 0.75f)
            lineTo(center.x - radius * 0.3f, center.y + radius * 0.35f)
            lineTo(center.x + radius * 0.3f, center.y + radius * 0.35f)
            close()
        }
        drawPath(path = needlePath, color = color)
    }
}

@Composable
private fun InfoIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFFF0055)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.dp.toPx()
        val center = Offset(w / 2f, h / 2f)
        val radius = minOf(w, h) * 0.42f

        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeW)
        )

        drawCircle(
            color = color,
            radius = 1.8.dp.toPx(),
            center = Offset(w / 2f, h * 0.30f)
        )

        drawLine(
            color = color,
            start = Offset(w / 2f, h * 0.44f),
            end = Offset(w / 2f, h * 0.72f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun getCardinalDirection(azimuth: Float): String {
    val degrees = (azimuth % 360 + 360) % 360
    return when (degrees) {
        in 337.5..360.0, in 0.0..22.5 -> "N"
        in 22.5..67.5 -> "NE"
        in 67.5..112.5 -> "E"
        in 112.5..157.5 -> "SE"
        in 157.5..202.5 -> "S"
        in 202.5..247.5 -> "SW"
        in 247.5..292.5 -> "W"
        in 292.5..337.5 -> "NW"
        else -> "N"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun VelocimetroActivityPreview() {
    MyApplicationTheme {
        CyberpunkDashboardScreen()
    }
}
