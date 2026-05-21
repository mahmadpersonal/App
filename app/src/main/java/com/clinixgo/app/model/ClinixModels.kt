package com.clinixgo.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
data class ClinixState(
    val onboardingComplete: Boolean = false,
    val demoMode: Boolean = false,
    val selectedPatientId: String = "self",
    val patients: List<PatientProfile> = listOf(PatientProfile()),
    val prescriptions: List<PrescriptionRecord> = emptyList(),
    val scheduledDoses: List<ScheduledDose> = emptyList()
)

@Serializable
data class PatientProfile(
    val id: String = "self",
    val relation: String = "Self",
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val allergies: String = "",
    val chronicConditions: String = "",
    val emergencyContact: String = "",
    val preferredPharmacy: String = "",
    val reminderPreference: String = "Smart reminders",
    val privacyPinEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false
)

@Serializable
data class PrescriptionRecord(
    val id: String = newId(),
    val patientId: String = "self",
    val sourceUri: String = "",
    val sourceType: SourceType = SourceType.Image,
    val medicines: List<MedicineInfo> = emptyList(),
    val tests: List<LabTestInfo> = emptyList(),
    val doctorName: String = "",
    val clinicName: String = "",
    val prescriptionDate: String = "",
    val followUpDate: String = "",
    val condition: String = "",
    val notes: String = "",
    val favorite: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Serializable
enum class SourceType { Camera, Image, Pdf, Demo }

@Serializable
data class MedicineInfo(
    val id: String = newId(),
    val name: String = "",
    val strength: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val timing: String = "",
    val duration: String = "",
    val instructions: String = "",
    val confidence: Float = 0f,
    val needsConfirmation: Boolean = true,
    val mealTiming: MealTiming = MealTiming.Unknown,
    val generalPurpose: String = "",
    val sideEffects: String = "",
    val warnings: String = "",
    val missedDoseGuidance: String = "If you miss a dose, ask your doctor or pharmacist what to do. Do not double doses unless they advise it.",
    val storageAdvice: String = "Store as written on the medicine label, away from heat, moisture, and children."
)

@Serializable
enum class MealTiming(val label: String) {
    BeforeFood("Before food"),
    AfterFood("After food"),
    WithFood("With food"),
    AnyTime("Any time"),
    Unknown("Confirm timing")
}

@Serializable
data class LabTestInfo(
    val id: String = newId(),
    val name: String = "",
    val explanation: String = "",
    val whyPrescribed: String = "",
    val preparation: String = "",
    val sampleType: String = "",
    val reminderEnabled: Boolean = false
)

@Serializable
data class ScheduledDose(
    val id: String = newId(),
    val prescriptionId: String,
    val patientId: String,
    val medicineId: String,
    val medicineName: String,
    val strength: String = "",
    val timeOfDay: DoseTime,
    val mealTiming: MealTiming,
    val reminderEnabled: Boolean = true,
    val status: DoseStatus = DoseStatus.Pending
)

@Serializable
enum class DoseTime(val label: String) {
    Morning("Morning"),
    Afternoon("Afternoon"),
    Evening("Evening"),
    Night("Night")
}

@Serializable
enum class DoseStatus { Pending, Taken, Skipped }

data class SafetyWarning(
    val title: String,
    val detail: String
)

data class ScanDraft(
    val sourceUri: String = "",
    val sourceType: SourceType = SourceType.Image,
    val medicines: List<MedicineInfo> = emptyList(),
    val tests: List<LabTestInfo> = emptyList(),
    val doctorName: String = "",
    val clinicName: String = "",
    val prescriptionDate: String = "",
    val followUpDate: String = "",
    val condition: String = "",
    val notes: String = ""
)

data class AssistantMessage(
    val fromUser: Boolean,
    val text: String
)
