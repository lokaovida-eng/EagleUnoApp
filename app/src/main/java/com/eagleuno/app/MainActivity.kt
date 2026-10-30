package com.eagleuno.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

enum class ScreenState {
    HOME, SPEEDOMETER, ABOUT, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Deixa a janela do Android 100% transparente
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            MaterialTheme {
                EagleUnoApp()
            }
        }
    }
}

@Composable
fun EagleUnoApp() {
    val context = LocalContext.current
    var speedKmH by remember { mutableFloatStateOf(0f) }
    var distanceKm by remember { mutableFloatStateOf(0f) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var currentScreen by remember { mutableStateOf(ScreenState.SPEEDOMETER) }

    // Registra atualização de GPS e envia para o WIDGET em tempo real automaticamente
    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val currentSpeed = location.speed * 3.6f
                speedKmH = if (currentSpeed > 0.8f) currentSpeed else 0f

                lastLocation?.let { prev ->
                    val deltaMeters = prev.distanceTo(location)
                    if (deltaMeters in 2.0f..300.0f) {
                        distanceKm += deltaMeters / 1000f
                    }
                }
                lastLocation = location

                NormalWidgetProvider.updateAllWidgets(context, speedKmH, distanceKm)
                CoverScreenWidgetProvider.updateAllCoverWidgets(context, speedKmH)
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission && locationManager != null) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    1.0f,
                    locationListener
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        onDispose {
            locationManager?.removeUpdates(locationListener)
        }
    }

    // Moldura néon em volta da tela transparente
    DynamicNeonSpeedFrame(currentSpeedKmH = speedKmH) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                ScreenState.SPEEDOMETER -> {
                    SpeedometerScreen(
                        speedKmH = speedKmH,
                        distanceKm = distanceKm,
                        onResetDistance = {
                            distanceKm = 0f
                            lastLocation = null
                            NormalWidgetProvider.updateAllWidgets(context, 0f, 0f)
                            CoverScreenWidgetProvider.updateAllCoverWidgets(context, 0f)
                        },
                        onNavigateToAbout = { currentScreen = ScreenState.ABOUT }
                    )
                }
                ScreenState.ABOUT -> {
                    AboutScreen(
                        onNavigateBack = { currentScreen = ScreenState.SPEEDOMETER }
                    )
                }
                else -> {
                    SpeedometerScreen(
                        speedKmH = speedKmH,
                        distanceKm = distanceKm,
                        onResetDistance = { distanceKm = 0f },
                        onNavigateToAbout = { currentScreen = ScreenState.ABOUT }
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicNeonSpeedFrame(
    currentSpeedKmH: Float,
    content: @Composable () -> Unit
) {
    var previousSpeed by remember { mutableFloatStateOf(0f) }
    var accelerationRate by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentSpeedKmH) {
        accelerationRate = currentSpeedKmH - previousSpeed
        previousSpeed = currentSpeedKmH
    }

    val isExceedingLimit = currentSpeedKmH > 30f
    val isRapidAccelerating = isExceedingLimit && accelerationRate > 2.0f
    val isDecelerating = isExceedingLimit && accelerationRate < -1.0f

    val infiniteTransition = rememberInfiniteTransition(label = "NeonRGBAnimation")
    val rgbHue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RGBHue"
    )

    val babyBlueNeon = Color(0xFF00F0FF)
    val ambientNeon = Color(0xFF00FF88).copy(alpha = 0.6f)
    val rgbColor = Color.hsv(hue = rgbHue, saturation = 1f, value = 1f)

    val targetColor = when {
        !isExceedingLimit -> Color.Transparent
        isRapidAccelerating -> rgbColor
        isDecelerating -> babyBlueNeon
        else -> ambientNeon
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 250),
        label = "BorderColorTransition"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isExceedingLimit) {
                    Modifier.border(
                        width = 6.dp,
                        brush = if (isRapidAccelerating) {
                            Brush.sweepGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green,
                                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(animatedColor, animatedColor))
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            )
    ) {
        content()
    }
}

@Composable
fun SpeedometerScreen(
    speedKmH: Float,
    distanceKm: Float,
    onResetDistance: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(28.dp)
        ) {
            Text(
                text = "EAGLE UNO v1.4",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "${speedKmH.toInt()}",
                color = if (speedKmH > 30f) Color(0xFF00F0FF) else Color.White,
                fontSize = 96.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "KM/H",
                color = if (speedKmH > 30f) Color(0xFF00F0FF) else Color.LightGray,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = String.format("Distância: %.2f km", distanceKm),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onResetDistance,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Text(text = "Zerar", color = Color.White)
                }

                Button(
                    onClick = onNavigateToAbout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF).copy(alpha = 0.8f))
                ) {
                    Text(text = "Sobre / Criador", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val paypalUrl = "https://www.paypal.com/donate/?hosted_button_id=REPB3R2VW4AVA"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFF16161E), shape = RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "Página do Criador",
                color = Color(0xFF00F0FF),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Eagle Uno Speedometer",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Versão 1.4 - Edição Especial Néon & GPS",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Gostou do projeto e quer apoiar o desenvolvimento do Eagle Uno?",
                color = Color.LightGray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Apoiar via PayPal (Doação)",
                    color = Color(0xFF003087),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Voltar ao Velocímetro", color = Color.White)
            }
        }
    }
}
