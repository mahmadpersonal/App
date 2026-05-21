package com.clinixgo.app.data

import android.content.Context
import com.clinixgo.app.model.ClinixState
import com.clinixgo.app.model.DoseStatus
import com.clinixgo.app.model.DoseTime
import com.clinixgo.app.model.MealTiming
import com.clinixgo.app.model.MedicineInfo
import com.clinixgo.app.model.PatientProfile
import com.clinixgo.app.model.PrescriptionRecord
import com.clinixgo.app.model.ScheduledDose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ClinixRepository(context: Context) {
    private val prefs = context.getSharedPreferences("clinixgo_store", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<ClinixState> = _state

    fun completeOnboarding() = update { it.copy(onboardingComplete = true) }

    fun selectPatient(patientId: String) = update { it.copy(selectedPatientId = patientId) }

    fun upsertPatient(patient: PatientProfile) = update { current ->
        val patients = current.patients.filterNot { it.id == patient.id } + patient
        current.copy(patients = patients, selectedPatientId = patient.id)
    }

    fun savePrescription(record: PrescriptionRecord) = update { current ->
        val saved = current.prescriptions.filterNot { it.id == record.id } + record
        val oldDoses = current.scheduledDoses.filterNot { it.prescriptionId == record.id }
        current.copy(
            prescriptions = saved.sortedByDescending { it.createdAtMillis },
            scheduledDoses = oldDoses + record.toSchedule()
        )
    }

    fun toggleFavorite(prescriptionId: String) = update { current ->
        current.copy(
            prescriptions = current.prescriptions.map {
                if (it.id == prescriptionId) it.copy(favorite = !it.favorite) else it
            }
        )
    }

    fun restartSchedule(record: PrescriptionRecord) = update { current ->
        val fresh = record.toSchedule()
        current.copy(scheduledDoses = current.scheduledDoses + fresh)
    }

    fun setDoseStatus(doseId: String, status: DoseStatus) = update { current ->
        current.copy(scheduledDoses = current.scheduledDoses.map {
            if (it.id == doseId) it.copy(status = status) else it
        })
    }

    fun setDoseReminder(doseId: String, enabled: Boolean) = update { current ->
        current.copy(scheduledDoses = current.scheduledDoses.map {
            if (it.id == doseId) it.copy(reminderEnabled = enabled) else it
        })
    }

    fun setDemoMode(enabled: Boolean) = update { current ->
        if (!enabled) current.copy(demoMode = false)
        else {
            val demoPatient = PatientProfile(
                id = "demo",
                relation = "Demo",
                name = "Demo Patient",
                age = "36",
                gender = "Not specified",
                allergies = "Penicillin",
                chronicConditions = "Blood pressure"
            )
            val demoPrescription = DemoRecords.prescription
            val withPatient = if (current.patients.any { it.id == "demo" }) current.patients else current.patients + demoPatient
            val withPrescription = if (current.prescriptions.any { it.id == demoPrescription.id }) {
                current.prescriptions
            } else {
                listOf(demoPrescription) + current.prescriptions
            }
            val withDoses = if (current.scheduledDoses.any { it.prescriptionId == demoPrescription.id }) {
                current.scheduledDoses
            } else {
                current.scheduledDoses + demoPrescription.toSchedule()
            }
            current.copy(
                demoMode = true,
                selectedPatientId = "demo",
                patients = withPatient,
                prescriptions = withPrescription,
                scheduledDoses = withDoses
            )
        }
    }

    private fun PrescriptionRecord.toSchedule(): List<ScheduledDose> {
        return medicines
            .filter { it.name.isNotBlank() && !it.needsConfirmation }
            .flatMap { medicine -> medicine.timeSlots().map { slot -> medicine.toDose(this, slot) } }
    }

    private fun MedicineInfo.toDose(record: PrescriptionRecord, slot: DoseTime): ScheduledDose {
        return ScheduledDose(
            prescriptionId = record.id,
            patientId = record.patientId,
            medicineId = id,
            medicineName = name,
            strength = strength,
            timeOfDay = slot,
            mealTiming = mealTiming
        )
    }

    private fun MedicineInfo.timeSlots(): List<DoseTime> {
        val text = listOf(frequency, timing, instructions).joinToString(" ").lowercase()
        return when {
            "night" in text || "bed" in text -> listOf(DoseTime.Night)
            "morning" in text && "evening" in text -> listOf(DoseTime.Morning, DoseTime.Evening)
            "morning" in text -> listOf(DoseTime.Morning)
            "afternoon" in text -> listOf(DoseTime.Afternoon)
            "evening" in text -> listOf(DoseTime.Evening)
            "twice" in text || "2" in text || "bd" in text -> listOf(DoseTime.Morning, DoseTime.Evening)
            "three" in text || "3" in text || "tds" in text -> listOf(DoseTime.Morning, DoseTime.Afternoon, DoseTime.Night)
            "four" in text || "4" in text -> listOf(DoseTime.Morning, DoseTime.Afternoon, DoseTime.Evening, DoseTime.Night)
            else -> listOf(DoseTime.Morning)
        }
    }

    private fun update(transform: (ClinixState) -> ClinixState) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit().putString(KEY_STATE, json.encodeToString(next)).apply()
    }

    private fun loadState(): ClinixState {
        val saved = prefs.getString(KEY_STATE, null) ?: return ClinixState()
        return runCatching { json.decodeFromString<ClinixState>(saved) }.getOrDefault(ClinixState())
    }

    companion object {
        private const val KEY_STATE = "clinix_state"
    }
}

private object DemoRecords {
    val prescription = PrescriptionRecord(
        id = "demo-prescription",
        patientId = "demo",
        sourceType = com.clinixgo.app.model.SourceType.Demo,
        medicines = listOf(
            MedicineInfo(
                id = "demo-med-1",
                name = "Amlodipine",
                strength = "5 mg",
                dosage = "1 tablet",
                frequency = "Once daily",
                timing = "Morning",
                duration = "30 days",
                instructions = "Take at the same time each day.",
                confidence = 0.92f,
                needsConfirmation = false,
                mealTiming = MealTiming.AnyTime,
                generalPurpose = "Commonly used to help control blood pressure.",
                sideEffects = "May cause ankle swelling, flushing, headache, or dizziness.",
                warnings = "Stand up slowly if dizzy and verify use with your clinician."
            )
        ),
        tests = listOf(
            com.clinixgo.app.model.LabTestInfo(
                name = "Lipid Profile",
                explanation = "A blood test that checks cholesterol and related fats.",
                whyPrescribed = "Doctors often use it to understand heart health risk.",
                preparation = "Fasting may be requested depending on the lab and doctor advice.",
                sampleType = "Blood"
            )
        ),
        doctorName = "Dr. Demo",
        clinicName = "ClinixGo Demo Clinic",
        prescriptionDate = "2026-05-21",
        followUpDate = "2026-06-20",
        condition = "Blood pressure",
        notes = "Demo Mode data for testing only."
    )
}
