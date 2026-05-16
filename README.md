# Surya-Shakti Solar Monitor

Native Android app in Kotlin for the MindMatrix VTU Internship project:
**Android App Development using GenAI - Surya-Shakti Solar Monitor (Energy)**.

## Features Implemented

- Daily solar generation log.
- Consumption tracker using kWh input.
- Battery percentage input.
- Mock weather simulation for Sunny, Cloudy, and Rainy days.
- Circular progress indicator for Green Energy Independence.
- Net savings calculation using a configurable per-unit electricity rate.
- Over-generation handling with export-to-grid display.
- 30-day savings report from Room database logs.
- Peak sun notification: "High Sun: Ideal time for heavy appliances."
- High-contrast yellow and black UI for outdoor readability.

## Tech Stack

- Kotlin
- Jetpack Compose
- Room Database
- ViewModel and Kotlin Flow
- Android notifications

## Open and Run in Android Studio

1. Open Android Studio.
2. Select **File > Open**.
3. Choose this project folder:

   `C:\Users\sanju\Documents\Codex\2026-05-15\files-mentioned-by-the-user-whatsapp`

4. Wait for Gradle Sync to finish.
5. If Android Studio asks to trust the project, click **Trust Project**.
6. Create or select a virtual device:
   - **Tools > Device Manager**
   - Create a Pixel device if you do not already have one.
   - Use Android API 35 or another installed recent API.
7. Select the emulator from the device dropdown.
8. Click **Run**.

## Important Build Notes

- Use **JDK 17** in Android Studio:
  - **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**
  - Set **Gradle JDK** to Android Studio's bundled JDK or JDK 17.
- The first Gradle sync needs internet access because Android Studio downloads AndroidX, Compose, Room, Kotlin, and Gradle plugin dependencies.
- If your Android SDK does not include API 35:
  - Open **Tools > SDK Manager**.
  - Install **Android 15.0 / API 35**.
  - Sync again.

## Main Source Files

- `app/src/main/java/com/mindmatrix/suryashakti/MainActivity.kt`
- `app/src/main/java/com/mindmatrix/suryashakti/ui/EnergyViewModel.kt`
- `app/src/main/java/com/mindmatrix/suryashakti/data/AppDatabase.kt`
- `app/src/main/java/com/mindmatrix/suryashakti/data/EnergyLogDao.kt`
- `app/src/main/java/com/mindmatrix/suryashakti/data/EnergyLogEntity.kt`

## Calculation Logic

- Solar used: `min(generation, consumption)`
- Grid import: `max(consumption - generation, 0)`
- Export to grid: `max(generation - consumption, 0)`
- Today's savings: `solarUsed * ratePerKwh`
- Independence score: `(solarUsed / consumption) * 100`

## Common Fixes

If Gradle sync fails because a dependency cannot be downloaded, check that Android Studio has internet access and retry **File > Sync Project with Gradle Files**.

If the emulator does not show notification permission automatically, run the app on Android 13 or newer and allow notifications when prompted.
