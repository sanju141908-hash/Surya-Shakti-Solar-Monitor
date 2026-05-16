package com.mindmatrix.suryashakti

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindmatrix.suryashakti.data.AppDatabase
import com.mindmatrix.suryashakti.data.EnergyLogEntity
import com.mindmatrix.suryashakti.data.EnergyRepository
import com.mindmatrix.suryashakti.ui.EnergyUiState
import com.mindmatrix.suryashakti.ui.EnergyViewModel
import com.mindmatrix.suryashakti.ui.EnergyViewModelFactory
import com.mindmatrix.suryashakti.ui.WeatherCondition
import java.util.Locale
import kotlin.math.max

private const val CHANNEL_ID = "peak_sun_channel"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = EnergyRepository(database.energyLogDao())
        val factory = EnergyViewModelFactory(repository)

        setContent {
            SuryaShaktiTheme {
                val viewModel: EnergyViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                SuryaShaktiApp(
                    state = state,
                    onGenerationChange = viewModel::updateGeneration,
                    onConsumptionChange = viewModel::updateConsumption,
                    onBatteryChange = viewModel::updateBattery,
                    onRateChange = viewModel::updateRate,
                    onWeatherChange = viewModel::updateWeather,
                    onSimulate = {
                        viewModel.simulateGeneration()
                        if (state.input.weather == WeatherCondition.Sunny) {
                            showPeakSuggestionNotification(context)
                        }
                    },
                    onSave = {
                        viewModel.saveToday()
                        if (state.input.weather == WeatherCondition.Sunny) {
                            showPeakSuggestionNotification(context)
                        }
                    },
                    onClearLogs = viewModel::clearLogs
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Peak Sun Suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Solar usage suggestions when sunlight is strong"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

@Composable
private fun SuryaShaktiTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = SolarYellow,
        secondary = SolarAmber,
        background = SolarBlack,
        surface = PanelBlack,
        onPrimary = SolarBlack,
        onSecondary = SolarBlack,
        onBackground = Color.White,
        onSurface = Color.White
    )
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun SuryaShaktiApp(
    state: EnergyUiState,
    onGenerationChange: (String) -> Unit,
    onConsumptionChange: (String) -> Unit,
    onBatteryChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onWeatherChange: (WeatherCondition) -> Unit,
    onSimulate: () -> Unit,
    onSave: () -> Unit,
    onClearLogs: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarBlack),
        color = SolarBlack
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item { Header() }
            item { ScorePanel(state) }
            item {
                InputPanel(
                    state = state,
                    onGenerationChange = onGenerationChange,
                    onConsumptionChange = onConsumptionChange,
                    onBatteryChange = onBatteryChange,
                    onRateChange = onRateChange,
                    onWeatherChange = onWeatherChange,
                    onSimulate = onSimulate,
                    onSave = onSave
                )
            }
            item { SavingsPanel(state) }
            item {
                LogsHeader(
                    count = state.logs.size,
                    onClearLogs = onClearLogs
                )
            }
            if (state.logs.isEmpty()) {
                item {
                    Text(
                        text = "No 30-day savings logs yet. Save today's reading to start tracking.",
                        color = MutedText,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            } else {
                items(state.logs, key = { it.id }) { log ->
                    LogRow(log)
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Surya-Shakti",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = SolarYellow
        )
        Text(
            text = "Personal solar dashboard for green energy independence",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

@Composable
private fun ScorePanel(state: EnergyUiState) {
    DashboardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SolarProgress(
                progress = (state.independenceScore / 100.0).toFloat(),
                label = "${state.independenceScore.format(0)}%"
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Green Energy Independence", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "Solar ${state.generation.format()} kWh vs Grid import ${state.gridImport.format()} kWh",
                    color = MutedText
                )
                LinearProgressIndicator(
                    progress = { (state.battery / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = SolarYellow,
                    trackColor = BorderYellow
                )
                Text("Battery level ${state.battery}%", color = SolarYellow, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SolarProgress(progress: Float, label: String) {
    Box(
        modifier = Modifier
            .size(132.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = BorderYellow,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = SolarYellow,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = SolarYellow, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
            Text("solar", color = MutedText, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun InputPanel(
    state: EnergyUiState,
    onGenerationChange: (String) -> Unit,
    onConsumptionChange: (String) -> Unit,
    onBatteryChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onWeatherChange: (WeatherCondition) -> Unit,
    onSimulate: () -> Unit,
    onSave: () -> Unit
) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Daily Energy Log", color = SolarYellow, fontWeight = FontWeight.Bold)
            NumberField("Generation today (kWh)", state.input.generationKwh, onGenerationChange)
            NumberField("Consumption / meter usage (kWh)", state.input.consumptionKwh, onConsumptionChange)
            NumberField("Battery level (%)", state.input.batteryPercent, onBatteryChange)
            NumberField("Electricity rate per unit", state.input.ratePerKwh, onRateChange)
            WeatherPicker(selected = state.input.weather, onSelected = onWeatherChange)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSimulate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber, contentColor = SolarBlack)
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = null)
                    Text("Simulate", modifier = Modifier.padding(start = 6.dp))
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SolarYellow, contentColor = SolarBlack)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text("Save", modifier = Modifier.padding(start = 6.dp))
                }
            }

            if (state.input.message != null) {
                Text(state.input.message, color = SolarYellow, fontWeight = FontWeight.Medium)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = SolarYellow)
                Text("Peak Suggestion: High Sun: Ideal time for heavy appliances.", color = Color.White)
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SolarYellow,
            unfocusedBorderColor = BorderYellow,
            focusedLabelColor = SolarYellow,
            cursorColor = SolarYellow,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherPicker(selected: WeatherCondition, onSelected: (WeatherCondition) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Mock weather condition") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SolarYellow,
                unfocusedBorderColor = BorderYellow,
                focusedLabelColor = SolarYellow,
                cursorColor = SolarYellow,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WeatherCondition.entries.forEach { weather ->
                DropdownMenuItem(
                    text = { Text(weather.label) },
                    onClick = {
                        onSelected(weather)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SavingsPanel(state: EnergyUiState) {
    DashboardCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Savings Report", color = SolarYellow, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Metric("Today's savings", "Rs ${state.todaySavings.format()}", Modifier.weight(1f))
                Metric("30-day savings", "Rs ${state.monthlySavings.format()}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Metric("Grid import", "${state.gridImport.format()} kWh", Modifier.weight(1f))
                Metric("Export to grid", "${state.exportToGrid.format()} kWh", Modifier.weight(1f))
            }
            Text(
                text = if (state.exportToGrid > 0.0) {
                    "Over-generation active: extra solar energy is available for grid export."
                } else {
                    "Use pumps, charging, or appliances during peak sun to reduce grid use."
                },
                color = Color.White
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF111111), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelMedium)
        Text(value, color = SolarYellow, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LogsHeader(count: Int, onClearLogs: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("30-Day Logs ($count)", color = Color.White, fontWeight = FontWeight.Bold)
        TextButton(onClick = onClearLogs) {
            Icon(Icons.Default.Clear, contentDescription = null, tint = SolarYellow)
            Text("Clear", color = SolarYellow)
        }
    }
}

@Composable
private fun LogRow(log: EnergyLogEntity) {
    val saved = minOf(log.generationKwh, log.consumptionKwh) * log.ratePerKwh
    val export = max(log.generationKwh - log.consumptionKwh, 0.0)
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(log.dateLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(log.weather, color = SolarYellow, fontWeight = FontWeight.Bold)
            }
            Text(
                "Generated ${log.generationKwh.format()} kWh, consumed ${log.consumptionKwh.format()} kWh, saved Rs ${saved.format()}",
                color = MutedText
            )
            if (export > 0.0) {
                Text("Exported ${export.format()} kWh to grid", color = SolarYellow)
            }
        }
    }
}

@Composable
private fun DashboardCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.padding(16.dp)) {
            content()
        }
    }
}

private fun showPeakSuggestionNotification(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_sun)
        .setContentTitle("High Sun")
        .setContentText("Ideal time for heavy appliances.")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(87, notification)
}

private fun Double.format(digits: Int = 2): String = "%.${digits}f".format(Locale.US, this)

private val SolarBlack = Color(0xFF050505)
private val PanelBlack = Color(0xFF1A1A1A)
private val SolarYellow = Color(0xFFFFD600)
private val SolarAmber = Color(0xFFFFAB00)
private val BorderYellow = Color(0x55FFD600)
private val MutedText = Color(0xFFD7D7D7)
