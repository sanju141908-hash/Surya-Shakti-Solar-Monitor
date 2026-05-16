package com.mindmatrix.suryashakti.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindmatrix.suryashakti.data.EnergyLogEntity
import com.mindmatrix.suryashakti.data.EnergyRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EnergyInputState(
    val generationKwh: String = "6.5",
    val consumptionKwh: String = "5.2",
    val batteryPercent: String = "72",
    val ratePerKwh: String = "8.00",
    val weather: WeatherCondition = WeatherCondition.Sunny,
    val message: String? = null
)

data class EnergyUiState(
    val input: EnergyInputState = EnergyInputState(),
    val logs: List<EnergyLogEntity> = emptyList()
) {
    val generation = input.generationKwh.toDoubleOrNull() ?: 0.0
    val consumption = input.consumptionKwh.toDoubleOrNull() ?: 0.0
    val battery = input.batteryPercent.toIntOrNull()?.coerceIn(0, 100) ?: 0
    val rate = input.ratePerKwh.toDoubleOrNull() ?: 0.0
    val solarUsed = minOf(generation, consumption)
    val gridImport = max(consumption - generation, 0.0)
    val exportToGrid = max(generation - consumption, 0.0)
    val independenceScore = if (consumption > 0.0) {
        ((solarUsed / consumption) * 100.0).coerceIn(0.0, 100.0)
    } else {
        0.0
    }
    val todaySavings = solarUsed * rate
    val monthlySavings = logs.sumOf { minOf(it.generationKwh, it.consumptionKwh) * it.ratePerKwh }
    val monthlyExport = logs.sumOf { max(it.generationKwh - it.consumptionKwh, 0.0) }
}

enum class WeatherCondition(val label: String, val multiplier: Double) {
    Sunny("Sunny", 1.0),
    Cloudy("Cloudy", 0.55),
    Rainy("Rainy", 0.28)
}

class EnergyViewModel(private val repository: EnergyRepository) : ViewModel() {
    private val inputState = MutableStateFlow(EnergyInputState())

    val uiState: StateFlow<EnergyUiState> = combine(
        inputState,
        repository.recentLogs
    ) { input, logs ->
        EnergyUiState(input = input, logs = logs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EnergyUiState()
    )

    fun updateGeneration(value: String) = updateInput { copy(generationKwh = value, message = null) }
    fun updateConsumption(value: String) = updateInput { copy(consumptionKwh = value, message = null) }
    fun updateBattery(value: String) = updateInput { copy(batteryPercent = value, message = null) }
    fun updateRate(value: String) = updateInput { copy(ratePerKwh = value, message = null) }
    fun updateWeather(value: WeatherCondition) = updateInput { copy(weather = value, message = null) }

    fun simulateGeneration() {
        val weather = inputState.value.weather
        val basePeak = Random.nextDouble(7.0, 10.5)
        val simulated = basePeak * weather.multiplier
        updateInput {
            copy(
                generationKwh = "%.2f".format(Locale.US, simulated),
                message = "${weather.label} simulation updated today's solar generation."
            )
        }
    }

    fun saveToday() {
        val input = inputState.value
        val generation = input.generationKwh.toDoubleOrNull()
        val consumption = input.consumptionKwh.toDoubleOrNull()
        val battery = input.batteryPercent.toIntOrNull()
        val rate = input.ratePerKwh.toDoubleOrNull()

        if (generation == null || consumption == null || battery == null || rate == null) {
            updateInput { copy(message = "Please enter valid numbers before saving.") }
            return
        }

        viewModelScope.launch {
            repository.save(
                EnergyLogEntity(
                    dateLabel = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()),
                    generationKwh = generation.coerceAtLeast(0.0),
                    consumptionKwh = consumption.coerceAtLeast(0.0),
                    batteryPercent = battery.coerceIn(0, 100),
                    ratePerKwh = rate.coerceAtLeast(0.0),
                    weather = input.weather.label
                )
            )
            val export = max(generation - consumption, 0.0)
            updateInput {
                copy(
                    batteryPercent = battery.coerceIn(0, 100).toString(),
                    message = if (export > 0.0) {
                        "Saved. Over-generation detected: %.2f kWh can be exported to grid.".format(Locale.US, export)
                    } else {
                        "Saved. Net savings calculated for today's solar usage."
                    }
                )
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAll()
            updateInput { copy(message = "All saved energy logs cleared.") }
        }
    }

    private fun updateInput(block: EnergyInputState.() -> EnergyInputState) {
        inputState.update(block)
    }
}

class EnergyViewModelFactory(private val repository: EnergyRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnergyViewModel::class.java)) {
            return EnergyViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
