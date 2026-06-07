package com.example.telesurgvr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleSurgVRApp()
        }
    }
}

@Composable
fun TeleSurgVRApp() {
    var latency by remember { mutableDoubleStateOf(10.0) }
    var latencyThreshold by remember { mutableDoubleStateOf(50.0) }
    var hudFocused by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    val heartRatePoints = remember { mutableStateListOf<Float>() }

    val isUnsafe = latency > latencyThreshold

    LaunchedEffect(Unit) {
        while (true) {
            latency = Random.nextDouble(5.0, 150.0)
            delay(1500.milliseconds)
        }
    }

    LaunchedEffect(Unit) {
        val baseHr = 72f
        while (true) {
            val nextHr = baseHr + (Random.nextFloat() * 10) - 5
            heartRatePoints.add(nextHr)
            if (heartRatePoints.size > 50) heartRatePoints.removeAt(0)
            delay(200.milliseconds)
        }
    }

    val borderColor by animateColorAsState(
        targetValue = if (isUnsafe) Color.Red else Color.Cyan.copy(alpha = 0.3f),
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "BorderAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .border(if (isUnsafe) 4.dp else 1.dp, borderColor)
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("TELESURG-VR MONITOR", color = Color.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("LATENCY: ${"%.1f".format(latency)}ms",
                    color = if (isUnsafe) Color.Red else Color.Green,
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(if (hudFocused) 1.0f else 0.4f)
                .clickable { hudFocused = !hudFocused }
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .background(Color(0xFF10141D), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Cyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PATIENT BIOMETRICS", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        (heartRatePoints.lastOrNull()?.toInt() ?: "--").toString(),
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(" BPM", color = Color.Red, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
                }

                Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    val path = Path()
                    val stepX = size.width / 50
                    heartRatePoints.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - ((v - 50f) / 50f * size.height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, Color.Red, style = Stroke(2.dp.toPx()))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    VitalItem("SpO2", "98%", Color.Green)
                    VitalItem("TEMP", "36.5°C", Color.Cyan)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (hudFocused) "[FOCUS: ACTIVE]" else "[CLICK TO FOCUS]",
                    color = Color.Cyan.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }

        if (isUnsafe) {
            DangerBanner()
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("System Configuration") },
                text = {
                    Column {
                        Text("Latency Threshold: ${latencyThreshold.toInt()}ms")
                        Slider(
                            value = latencyThreshold.toFloat(),
                            onValueChange = { latencyThreshold = it.toDouble() },
                            valueRange = 20f..200f
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showSettings = false }) { Text("Done") }
                }
            )
        }
    }
}

@Composable
fun VitalItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BoxScope.DangerBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "Alpha"
    )
    Surface(
        color = Color.Red,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 40.dp)
            .alpha(alpha)
    ) {
        Text(
            "DANGER: NETWORK LATENCY CRITICAL",
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.ExtraBold
        )
    }
}