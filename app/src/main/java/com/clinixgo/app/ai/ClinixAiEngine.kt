package com.clinixgo.app.ai

import com.clinixgo.app.model.LabTestInfo
import com.clinixgo.app.model.MealTiming
import com.clinixgo.app.model.MedicineInfo
import com.clinixgo.app.model.PrescriptionRecord
import com.clinixgo.app.model.SafetyWarning
import com.clinixgo.app.model.ScanDraft
import com.clinixgo.app.model.SourceType

interface PrescriptionAiService {
    suspend fun extractPrescription(sourceUri: String, sourceType: SourceType): ScanDraft
    suspend fun answerQuestion(question: String, context: List<PrescriptionRecord>): String
}

class SafeOfflineAiService : PrescriptionAiService {
    override suspend fun extractPrescription(sourceUri: String, sourceType: SourceType): ScanDraft {
        return ScanDraft(
            sourceUri = sourceUri,
            sourceType = sourceType,
            medicines = listOf(
                MedicineInfo(
                    name = "",
                    dosage = "",
                    frequency = "",
                    timing = "",
                    duration = "",
                    instructions = "Fill only what you can verify from the prescription.",
                    confidence = 0f,
                    needsConfirmation = true,
                    mealTiming = MealTiming.Unknown
                )
            ),
            notes = "Offline extractor created an editable review draft. Connect Gemini later for OCR and structured extraction."
        )
    }

    override suspend fun answerQuestion(question: String, context: List<PrescriptionRecord>): String {
        val lower = question.lowercase()
        val safety = "Please verify with your doctor or pharmacist. I can help explain and organize information, but I cannot diagnose or change treatment."
        return when {
            "cbc" in lower -> "CBC means Complete Blood Count. It checks blood cells and can help doctors evaluate infection, anemia, and other general health clues. $safety"
            "summarize" in lower || "summary" in lower -> summarizeLatest(context, safety)
            "when" in lower || "take" in lower -> "Use the confirmed schedule in the Schedule tab for timing. If the prescription is unclear, do not guess the dose or timing. $safety"
            "medicine" in lower || "for" in lower -> "Open the medicine card in your prescription review or vault to see simple purpose, side effects, warnings, missed dose guidance, and storage advice. $safety"
            else -> "I can explain prescription terms, lab tests, medicine schedule labels, and saved prescription summaries in simple language. $safety"
        }
    }

    private fun summarizeLatest(context: List<PrescriptionRecord>, safety: String): String {
        val latest = context.maxByOrNull { it.createdAtMillis }
            ?: return "No saved prescriptions are available yet. Scan or upload one first. $safety"
        val medicineText = latest.medicines.filter { it.name.isNotBlank() }.joinToString { it.name }
            .ifBlank { "No confirmed medicines saved" }
        val testText = latest.tests.filter { it.name.isNotBlank() }.joinToString { it.name }
            .ifBlank { "No tests saved" }
        return "Latest prescription: medicines: $medicineText. Tests: $testText. Doctor: ${latest.doctorName.ifBlank { "not recorded" }}. Follow-up: ${latest.followUpDate.ifBlank { "not recorded" }}. $safety"
    }
}

object SafetyChecker {
    fun warnings(record: PrescriptionRecord, allergyText: String): List<SafetyWarning> {
        val warnings = mutableListOf<SafetyWarning>()
        record.medicines.forEach { medicine ->
            if (medicine.name.isBlank()) {
                warnings += SafetyWarning("Unclear medicine", "A medicine name is missing. Confirm it before saving the schedule.")
            }
            if (medicine.dosage.isBlank() || medicine.frequency.isBlank()) {
                warnings += SafetyWarning("Missing dosage or frequency", "${medicine.name.ifBlank { "A medicine" }} is missing dosage or frequency. Never guess unclear prescription details.")
            }
            if (medicine.needsConfirmation) {
                warnings += SafetyWarning("Needs confirmation", "${medicine.name.ifBlank { "This medicine" }} should be confirmed with a doctor or pharmacist.")
            }
        }
        val duplicateMedicines = record.medicines.map { it.name.trim().lowercase() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
        duplicateMedicines.keys.forEach { warnings += SafetyWarning("Duplicate medicine", "$it appears more than once. Confirm whether this is intentional.") }

        val repeatedTests = record.tests.map { it.name.trim().lowercase() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
        repeatedTests.keys.forEach { warnings += SafetyWarning("Repeated test", "$it appears more than once. Confirm whether it is a duplicate.") }

        val allergies = allergyText.split(",", ";").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        record.medicines.forEach { medicine ->
            if (allergies.any { allergy -> medicine.name.lowercase().contains(allergy) }) {
                warnings += SafetyWarning("Allergy warning", "${medicine.name} may match a saved allergy. Please verify before taking it.")
            }
        }
        if (warnings.isEmpty()) {
            warnings += SafetyWarning("Verification reminder", "Please verify with your doctor or pharmacist.")
        }
        return warnings
    }

    fun compare(old: PrescriptionRecord, newer: PrescriptionRecord): List<String> {
        val oldMeds = old.medicines.map { it.name.trim() }.filter { it.isNotBlank() }.toSet()
        val newMeds = newer.medicines.map { it.name.trim() }.filter { it.isNotBlank() }.toSet()
        val added = newMeds - oldMeds
        val removed = oldMeds - newMeds
        val notes = mutableListOf<String>()
        if (added.isNotEmpty()) notes += "Added medicines: ${added.joinToString()}"
        if (removed.isNotEmpty()) notes += "Removed medicines: ${removed.joinToString()}"
        newer.medicines.forEach { current ->
            val previous = old.medicines.firstOrNull { it.name.equals(current.name, ignoreCase = true) }
            if (previous != null && (previous.dosage != current.dosage || previous.frequency != current.frequency)) {
                notes += "Dosage or frequency changed for ${current.name}."
            }
        }
        val repeatedTests = newer.tests.map { it.name.trim() }.filter { it.isNotBlank() }
            .groupingBy { it.lowercase() }
            .eachCount()
            .filterValues { it > 1 }
        if (repeatedTests.isNotEmpty()) notes += "Repeated tests: ${repeatedTests.keys.joinToString()}"
        return notes.ifEmpty { listOf("No clear changes detected between these prescriptions.") }
    }
}

object PatientEducation {
    fun enrichMedicine(medicine: MedicineInfo): MedicineInfo {
        val name = medicine.name.lowercase()
        return when {
            "paracetamol" in name || "acetaminophen" in name -> medicine.copy(
                generalPurpose = "Often used for fever or mild pain relief.",
                sideEffects = "Usually tolerated, but high doses can harm the liver.",
                warnings = "Avoid taking multiple products that contain the same ingredient."
            )
            "amoxicillin" in name || "azithromycin" in name || "antibiotic" in name -> medicine.copy(
                generalPurpose = "Antibiotics are used for certain bacterial infections when prescribed.",
                sideEffects = "May cause stomach upset, loose stools, rash, or allergy symptoms.",
                warnings = "Take exactly as prescribed and report allergy signs urgently."
            )
            "omeprazole" in name || "pantoprazole" in name -> medicine.copy(
                generalPurpose = "Commonly used to reduce stomach acid.",
                sideEffects = "May cause headache, nausea, constipation, or diarrhea.",
                warnings = "Confirm duration with your clinician, especially for long-term use."
            )
            else -> medicine.copy(
                generalPurpose = medicine.generalPurpose.ifBlank { "General purpose depends on the exact medicine and condition." },
                sideEffects = medicine.sideEffects.ifBlank { "Side effects vary by medicine. Check the label and ask a pharmacist." },
                warnings = medicine.warnings.ifBlank { "Confirm dose, timing, interactions, pregnancy safety, and allergies with a professional." }
            )
        }
    }

    fun enrichTest(test: LabTestInfo): LabTestInfo {
        val name = test.name.lowercase()
        return when {
            "cbc" in name -> test.copy(
                explanation = "Complete Blood Count checks red cells, white cells, and platelets.",
                whyPrescribed = "Doctors use it to look for infection, anemia, inflammation, and general blood health clues.",
                preparation = "Usually no fasting is needed unless your doctor combines it with other tests.",
                sampleType = "Blood"
            )
            "lipid" in name || "cholesterol" in name -> test.copy(
                explanation = "Lipid profile checks cholesterol and related fats.",
                whyPrescribed = "It helps estimate heart and blood vessel risk.",
                preparation = "Some doctors or labs request fasting. Confirm before the test.",
                sampleType = "Blood"
            )
            "hba1c" in name || "a1c" in name -> test.copy(
                explanation = "HbA1c estimates average blood sugar over the past few months.",
                whyPrescribed = "It is commonly used to monitor diabetes risk or diabetes control.",
                preparation = "Usually no fasting is needed.",
                sampleType = "Blood"
            )
            else -> test.copy(
                explanation = test.explanation.ifBlank { "This lab test needs confirmation from your doctor or lab." },
                whyPrescribed = test.whyPrescribed.ifBlank { "Doctors prescribe tests to support clinical decisions, not as a diagnosis by themselves." },
                preparation = test.preparation.ifBlank { "Ask the lab whether fasting or special preparation is needed." },
                sampleType = test.sampleType.ifBlank { "Confirm with the lab" }
            )
        }
    }

    fun lifestyleSuggestions(condition: String): List<String> {
        val text = condition.lowercase()
        return when {
            "diabetes" in text || "sugar" in text -> listOf(
                "Keep regular meal timing and follow the diet plan given by your clinician.",
                "Track blood sugar as advised and bring readings to follow-up visits.",
                "Choose daily movement that your doctor says is safe for you."
            )
            "blood pressure" in text || "hypertension" in text || "bp" in text -> listOf(
                "Limit excess salt if your clinician has advised it.",
                "Measure blood pressure at consistent times and record readings.",
                "Avoid stopping blood pressure medicine without medical advice."
            )
            "acidity" in text || "reflux" in text || "gastritis" in text -> listOf(
                "Notice foods or late meals that worsen symptoms.",
                "Avoid lying down soon after meals if reflux is a problem.",
                "Ask your doctor how long acid-reducing medicine should continue."
            )
            "infection" in text || "fever" in text -> listOf(
                "Rest, fluids, and follow-up matter when symptoms persist or worsen.",
                "Take prescribed antibiotics exactly as written if one was prescribed.",
                "Seek urgent care for breathing trouble, severe weakness, confusion, or worsening symptoms."
            )
            else -> emptyList()
        }
    }
}

class GeminiPrescriptionAiService(
    private val apiKey: String
) : PrescriptionAiService {
    override suspend fun extractPrescription(sourceUri: String, sourceType: SourceType): ScanDraft {
        error("Gemini is not connected yet. Use this class to send OCR/image/PDF content to Gemini and map the JSON response into ScanDraft.")
    }

    override suspend fun answerQuestion(question: String, context: List<PrescriptionRecord>): String {
        error("Gemini is not connected yet. Send the user question plus redacted prescription context, then enforce ClinixGo medical safety rules on the response.")
    }
}
