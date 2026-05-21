# ClinixGo

Tagline: **Your AI Prescription & Medicine Assistant**

ClinixGo is a native Android healthcare organizer built with Kotlin, Jetpack Compose, Material 3, and local JSON persistence. It starts clean with empty states and includes one optional Demo Mode for testing.

## Project Structure

- `app/src/main/java/com/clinixgo/app/MainActivity.kt` starts Compose and wires the repository plus AI service.
- `app/src/main/java/com/clinixgo/app/model/ClinixModels.kt` contains patient, prescription, medicine, lab test, schedule, and assistant models.
- `app/src/main/java/com/clinixgo/app/data/ClinixRepository.kt` owns local persistence, family profiles, saved prescriptions, favorites, and generated schedules.
- `app/src/main/java/com/clinixgo/app/ai/ClinixAiEngine.kt` contains the AI-ready interface, safe offline extractor, assistant responses, safety checker, patient education helpers, and a Gemini service placeholder.
- `app/src/main/java/com/clinixgo/app/ui/screens/ClinixGoApp.kt` contains the screen flow: onboarding, home, scan, extraction review, schedule, vault, profile, finder, assistant, comparison, and detail screens.
- `app/src/main/java/com/clinixgo/app/ui/components/ClinixComponents.kt` contains reusable cards, banners, chips, fields, metrics, empty states, and expandable panels.
- `app/src/main/java/com/clinixgo/app/ui/theme/ClinixTheme.kt` defines the premium healthcare theme.

## How To Run

1. Open this folder in Android Studio.
2. Let Android Studio sync Gradle dependencies.
3. Select an emulator or Android device.
4. Run the `app` configuration.

The shell on this machine does not currently expose Android SDK or Gradle commands, so Android Studio is the expected run environment.

## Cloud APK Build

This project includes `.github/workflows/build-apk.yml` for GitHub Actions.

1. Push this folder to a GitHub repository.
2. Open the repository on GitHub.
3. Go to **Actions**.
4. Run **Build ClinixGo APK** manually, or push to `main`.
5. Open the finished workflow run and download the **ClinixGo-debug-apk** artifact.

For a public release download link, push a tag such as `v1.0.0`. The workflow attaches `app-debug.apk` to the GitHub Release.

## Local Storage

ClinixGo stores app data in `SharedPreferences` as serialized JSON through `ClinixRepository`. The repository is intentionally small and can later be replaced with Room, encrypted storage, cloud sync, backup/restore, or account login without changing most UI screens.

## AI Extraction

The AI extraction boundary is:

```kotlin
interface PrescriptionAiService {
    suspend fun extractPrescription(sourceUri: String, sourceType: SourceType): ScanDraft
    suspend fun answerQuestion(question: String, context: List<PrescriptionRecord>): String
}
```

`SafeOfflineAiService` creates a safe editable draft and never guesses unclear prescription details. Users must confirm medicine details before schedules are generated.

## Connecting Gemini Later

Use `GeminiPrescriptionAiService` in `ClinixAiEngine.kt`:

1. Read the prescription image or PDF from `sourceUri`.
2. Send it to Gemini with a strict JSON schema for medicines, dosage, frequency, timing, duration, tests, doctor, clinic, dates, condition, and confidence.
3. Map Gemini JSON into `ScanDraft`.
4. Keep the safety rule: missing or unclear values must stay blank with `needsConfirmation = true`.
5. Swap `SafeOfflineAiService()` for `GeminiPrescriptionAiService(apiKey)` in `MainActivity.kt`.

## Future API Connection Points

- Pharmacy and lab APIs: `FinderScreen` and `ProviderSection`.
- Maps/distance: provider card placeholders in the finder UI.
- Calendar and reminders: `ScheduleScreen`, `ScheduledDose`, and reminder toggles.
- Notifications: add a `BroadcastReceiver` and scheduler around `ScheduledDose`.
- Cloud sync/encryption: replace or wrap `ClinixRepository`.
- OCR/Gemini: implement `GeminiPrescriptionAiService`.
