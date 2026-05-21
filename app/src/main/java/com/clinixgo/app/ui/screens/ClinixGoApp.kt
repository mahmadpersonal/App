@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.clinixgo.app.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clinixgo.app.ai.PatientEducation
import com.clinixgo.app.ai.PrescriptionAiService
import com.clinixgo.app.ai.SafetyChecker
import com.clinixgo.app.data.ClinixRepository
import com.clinixgo.app.model.AssistantMessage
import com.clinixgo.app.model.ClinixState
import com.clinixgo.app.model.DoseStatus
import com.clinixgo.app.model.DoseTime
import com.clinixgo.app.model.LabTestInfo
import com.clinixgo.app.model.MealTiming
import com.clinixgo.app.model.MedicineInfo
import com.clinixgo.app.model.PatientProfile
import com.clinixgo.app.model.PrescriptionRecord
import com.clinixgo.app.model.ScanDraft
import com.clinixgo.app.model.ScheduledDose
import com.clinixgo.app.model.SourceType
import com.clinixgo.app.model.newId
import com.clinixgo.app.ui.components.ChipRow
import com.clinixgo.app.ui.components.ClinixTopBar
import com.clinixgo.app.ui.components.ConfidenceBadge
import com.clinixgo.app.ui.components.EmptyState
import com.clinixgo.app.ui.components.ExpandableCard
import com.clinixgo.app.ui.components.LabelValue
import com.clinixgo.app.ui.components.LoadingCard
import com.clinixgo.app.ui.components.MetricCard
import com.clinixgo.app.ui.components.QuickAction
import com.clinixgo.app.ui.components.SafetyBanner
import com.clinixgo.app.ui.components.SectionHeader
import com.clinixgo.app.ui.components.SimpleTextField
import com.clinixgo.app.ui.theme.Coral
import com.clinixgo.app.ui.theme.Gold
import com.clinixgo.app.ui.theme.Mint
import com.clinixgo.app.ui.theme.Sky
import com.clinixgo.app.ui.theme.Teal
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class MainTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Scan("Scan", Icons.Default.PhotoCamera),
    Schedule("Schedule", Icons.Default.CalendarMonth),
    Vault("Vault", Icons.Default.Description),
    Profile("Profile", Icons.Default.Person)
}

sealed interface OverlayScreen {
    data class Review(val draft: ScanDraft) : OverlayScreen
    data class VaultDetail(val recordId: String) : OverlayScreen
    data object Assistant : OverlayScreen
    data object Finder : OverlayScreen
    data object Compare : OverlayScreen
}

@Composable
fun ClinixGoApp(repository: ClinixRepository, aiService: PrescriptionAiService) {
    val state by repository.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var overlay by remember { mutableStateOf<OverlayScreen?>(null) }

    if (!state.onboardingComplete) {
        OnboardingScreen(onDone = repository::completeOnboarding)
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        bottomBar = {
            if (overlay == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.navigationBarsPadding()) {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            AnimatedContent(targetState = overlay, label = "clinix-navigation") { current ->
                when (current) {
                    is OverlayScreen.Review -> ReviewScreen(
                        state = state,
                        draft = current.draft,
                        onBack = { overlay = null },
                        onSave = {
                            repository.savePrescription(it)
                            overlay = OverlayScreen.VaultDetail(it.id)
                        }
                    )
                    is OverlayScreen.VaultDetail -> {
                        val record = state.prescriptions.firstOrNull { it.id == current.recordId }
                        if (record == null) {
                            EmptyStatePage("Prescription not found", "It may have been removed.")
                        } else {
                            VaultDetailScreen(
                                state = state,
                                record = record,
                                onBack = { overlay = null },
                                onFavorite = { repository.toggleFavorite(record.id) },
                                onRestart = { repository.restartSchedule(record) }
                            )
                        }
                    }
                    OverlayScreen.Assistant -> AssistantScreen(state, aiService, onBack = { overlay = null })
                    OverlayScreen.Finder -> FinderScreen(onBack = { overlay = null })
                    OverlayScreen.Compare -> CompareScreen(state, onBack = { overlay = null })
                    null -> when (selectedTab) {
                        MainTab.Home -> HomeScreen(
                            state = state,
                            onScan = { selectedTab = MainTab.Scan },
                            onOpenAssistant = { overlay = OverlayScreen.Assistant },
                            onOpenFinder = { overlay = OverlayScreen.Finder },
                            onOpenVault = { selectedTab = MainTab.Vault },
                            onDemoMode = repository::setDemoMode
                        )
                        MainTab.Scan -> ScanScreen(aiService = aiService, onReview = { overlay = OverlayScreen.Review(it) })
                        MainTab.Schedule -> ScheduleScreen(state = state, repository = repository)
                        MainTab.Vault -> VaultScreen(
                            state = state,
                            onOpen = { overlay = OverlayScreen.VaultDetail(it.id) },
                            onCompare = { overlay = OverlayScreen.Compare }
                        )
                        MainTab.Profile -> ProfileScreen(state = state, repository = repository)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        content = content
    )
}

@Composable
private fun EmptyStatePage(title: String, detail: String) {
    ScreenColumn {
        EmptyState(title, detail)
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        "Scan prescriptions" to "Capture doctor prescriptions, images, or PDFs and review every extracted field.",
        "Understand medicines" to "See simple medicine purpose, side effects, warnings, missed dose guidance, and storage advice.",
        "Organize family care" to "Keep prescriptions, lab tests, schedules, and history separate for yourself and family members."
    )
    var page by rememberSaveable { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("ClinixGo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Teal)
            Text("Your AI Prescription & Medicine Assistant", style = MaterialTheme.typography.titleMedium)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Mint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Teal, modifier = Modifier.size(34.dp))
                    }
                    Text(pages[page].first, style = MaterialTheme.typography.headlineSmall)
                    Text(pages[page].second, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SafetyBanner()
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach {
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (it == page) Teal else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
            Button(
                onClick = {
                    if (page == pages.lastIndex) onDone() else page += 1
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (page == pages.lastIndex) "Start clean" else "Continue")
            }
        }
    }
}

@Composable
fun HomeScreen(
    state: ClinixState,
    onScan: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenFinder: () -> Unit,
    onOpenVault: () -> Unit,
    onDemoMode: (Boolean) -> Unit
) {
    val patient = state.patients.firstOrNull { it.id == state.selectedPatientId } ?: state.patients.first()
    val patientDoses = state.scheduledDoses.filter { it.patientId == patient.id }
    val pendingDoses = patientDoses.filter { it.status == DoseStatus.Pending }
    val takenCount = patientDoses.count { it.status == DoseStatus.Taken }
    val adherence = if (patientDoses.isEmpty()) 0 else (takenCount * 100 / patientDoses.size)
    val patientPrescriptions = state.prescriptions.filter { it.patientId == patient.id }
    val pendingTests = patientPrescriptions.flatMap { it.tests }.filter { it.name.isNotBlank() }

    ScreenColumn {
        ClinixTopBar(
            title = "ClinixGo",
            subtitle = "Your AI Prescription & Medicine Assistant",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Demo", style = MaterialTheme.typography.labelSmall)
                    Switch(checked = state.demoMode, onCheckedChange = onDemoMode)
                }
            }
        )
        SafetyBanner()
        Spacer(Modifier.height(14.dp))
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Quick Scan Prescription")
        }
        SectionHeader("Today")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Today medicines", patientDoses.size.toString(), "Confirmed schedule items", Mint, Modifier.weight(1f))
            MetricCard("Active medicines", patientDoses.map { it.medicineId }.distinct().size.toString(), "For ${patient.name.ifBlank { patient.relation }}", Sky, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Adherence", "$adherence%", "Taken doses progress", MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
            MetricCard("Pending tests", pendingTests.size.toString(), "Detected from prescriptions", MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
        }
        SectionHeader("Next medicine dose")
        val nextDose = pendingDoses.firstOrNull()
        if (nextDose == null) EmptyState("No medicines scheduled", "Confirmed medicines will appear here.")
        else DoseCard(nextDose, onTaken = {}, onSkipped = {}, onReminder = {})

        SectionHeader("Recent prescriptions", action = "Vault", onAction = onOpenVault)
        if (patientPrescriptions.isEmpty()) EmptyState("No prescriptions saved yet", "Scan a prescription to build your permanent vault.", Icons.Default.Description)
        else patientPrescriptions.take(3).forEach { PrescriptionCard(it, onClick = onOpenVault) }

        SectionHeader("Care tools")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction("AI assistant", Icons.Default.Chat, onOpenAssistant)
            QuickAction("Find pharmacies and labs", Icons.Default.LocalPharmacy, onOpenFinder)
            QuickAction("Prescription vault", Icons.Default.Description, onOpenVault)
        }
    }
}

@Composable
fun ScanScreen(aiService: PrescriptionAiService, onReview: (ScanDraft) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedType by remember { mutableStateOf(SourceType.Image) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    fun createCameraUri(): Uri {
        val dir = File(context.filesDir, "prescriptions").also { it.mkdirs() }
        val file = File(dir, "prescription_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun persistPickedUri(uri: Uri, extension: String): Uri {
        val dir = File(context.filesDir, "prescriptions").also { it.mkdirs() }
        val file = File(dir, "prescription_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedUri = cameraUri
            selectedType = SourceType.Camera
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = persistPickedUri(it, "jpg")
            selectedType = SourceType.Image
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedUri = persistPickedUri(it, "pdf")
            selectedType = SourceType.Pdf
        }
    }

    ScreenColumn {
        ClinixTopBar("Scan prescription", "Capture, upload, preview, then review AI extraction.")
        SafetyBanner("Never guess unclear prescription information. Confirm or edit every uncertain field before generating a schedule.")
        SectionHeader("Source")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ElevatedButton(
                onClick = {
                    cameraUri = createCameraUri()
                    cameraLauncher.launch(cameraUri)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoCamera, null)
                Spacer(Modifier.width(6.dp))
                Text("Camera")
            }
            ElevatedButton(onClick = { imageLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Image, null)
                Spacer(Modifier.width(6.dp))
                Text("Image")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { pdfLauncher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileUpload, null)
            Spacer(Modifier.width(8.dp))
            Text("Upload PDF")
        }
        SectionHeader("Preview")
        if (selectedUri == null) {
            EmptyState("No prescription selected", "Capture a photo, upload an image, or choose a PDF.", Icons.Default.PhotoCamera)
        } else {
            PrescriptionPreview(uri = selectedUri.toString(), sourceType = selectedType)
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !isAnalyzing,
                onClick = {
                    scope.launch {
                        isAnalyzing = true
                        val draft = aiService.extractPrescription(selectedUri.toString(), selectedType)
                        isAnalyzing = false
                        onReview(draft)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.HealthAndSafety, null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze and review")
            }
        }
        if (isAnalyzing) {
            Spacer(Modifier.height(12.dp))
            LoadingCard("Preparing editable extraction review")
        }
    }
}

@Composable
fun ReviewScreen(
    state: ClinixState,
    draft: ScanDraft,
    onBack: () -> Unit,
    onSave: (PrescriptionRecord) -> Unit
) {
    var patientId by rememberSaveable { mutableStateOf(state.selectedPatientId) }
    var medicines by remember { mutableStateOf(draft.medicines.ifEmpty { listOf(MedicineInfo()) }) }
    var tests by remember { mutableStateOf(draft.tests) }
    var doctor by rememberSaveable { mutableStateOf(draft.doctorName) }
    var clinic by rememberSaveable { mutableStateOf(draft.clinicName) }
    var prescriptionDate by rememberSaveable { mutableStateOf(draft.prescriptionDate) }
    var followUpDate by rememberSaveable { mutableStateOf(draft.followUpDate) }
    var condition by rememberSaveable { mutableStateOf(draft.condition) }
    var notes by rememberSaveable { mutableStateOf(draft.notes) }

    ScreenColumn {
        ClinixTopBar("Review extraction", "Edit before saving. ClinixGo will not guess unclear fields.") {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        PrescriptionPreview(uri = draft.sourceUri, sourceType = draft.sourceType)
        SectionHeader("Family member")
        ChipRow(
            items = state.patients.map { it.id },
            selected = patientId,
            onSelect = { patientId = it }
        )
        Text(
            state.patients.firstOrNull { it.id == patientId }?.let { it.name.ifBlank { it.relation } }.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SectionHeader("Doctor details")
        SimpleTextField(doctor, "Doctor name", { doctor = it })
        SimpleTextField(clinic, "Clinic / hospital", { clinic = it })
        SimpleTextField(prescriptionDate, "Prescription date", { prescriptionDate = it })
        SimpleTextField(followUpDate, "Follow-up date", { followUpDate = it })
        SimpleTextField(condition, "Condition, only if clearly mentioned", { condition = it })

        SectionHeader("Medicines")
        medicines.forEachIndexed { index, medicine ->
            MedicineEditCard(
                medicine = medicine,
                onChange = { updated -> medicines = medicines.toMutableList().also { it[index] = updated } },
                onRemove = { medicines = medicines.filterIndexed { i, _ -> i != index } }
            )
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(onClick = { medicines = medicines + MedicineInfo() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add medicine")
        }

        SectionHeader("Lab tests")
        if (tests.isEmpty()) EmptyState("No tests pending", "Add tests only if they are clearly written.", Icons.Default.LocalHospital)
        tests.forEachIndexed { index, test ->
            TestEditCard(test, onChange = { updated -> tests = tests.toMutableList().also { it[index] = updated } })
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(onClick = { tests = tests + LabTestInfo() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add test")
        }
        SimpleTextField(notes, "Notes", { notes = it }, singleLine = false)

        val record = PrescriptionRecord(
            patientId = patientId,
            sourceUri = draft.sourceUri,
            sourceType = draft.sourceType,
            medicines = medicines.map { PatientEducation.enrichMedicine(it) },
            tests = tests.map { PatientEducation.enrichTest(it) },
            doctorName = doctor,
            clinicName = clinic,
            prescriptionDate = prescriptionDate,
            followUpDate = followUpDate,
            condition = condition,
            notes = notes
        )
        val patient = state.patients.firstOrNull { it.id == patientId }
        val warnings = SafetyChecker.warnings(record, patient?.allergies.orEmpty())
        SectionHeader("Safety checker")
        warnings.forEach {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Coral)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(it.title, style = MaterialTheme.typography.labelLarge)
                        Text(it.detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = { onSave(record) },
            modifier = Modifier.fillMaxWidth(),
            enabled = medicines.any { it.name.isNotBlank() } || tests.any { it.name.isNotBlank() } || draft.sourceUri.isNotBlank()
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Confirm and save to vault")
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
fun MedicineEditCard(medicine: MedicineInfo, onChange: (MedicineInfo) -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Medicine card", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                ConfidenceBadge(medicine.confidence, medicine.needsConfirmation)
                IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "Remove") }
            }
            SimpleTextField(medicine.name, "Medicine name", { onChange(medicine.copy(name = it, needsConfirmation = it.isBlank() || medicine.dosage.isBlank() || medicine.frequency.isBlank())) })
            SimpleTextField(medicine.strength, "Strength", { onChange(medicine.copy(strength = it)) })
            SimpleTextField(medicine.dosage, "Dosage", { onChange(medicine.copy(dosage = it, needsConfirmation = medicine.name.isBlank() || it.isBlank() || medicine.frequency.isBlank())) })
            SimpleTextField(medicine.frequency, "Frequency", { onChange(medicine.copy(frequency = it, needsConfirmation = medicine.name.isBlank() || medicine.dosage.isBlank() || it.isBlank())) })
            SimpleTextField(medicine.timing, "Timing", { onChange(medicine.copy(timing = it)) })
            SimpleTextField(medicine.duration, "Duration", { onChange(medicine.copy(duration = it)) })
            SimpleTextField(medicine.instructions, "Instructions", { onChange(medicine.copy(instructions = it)) }, singleLine = false)
            ChipRow(
                items = MealTiming.entries.map { it.label },
                selected = medicine.mealTiming.label,
                onSelect = { label -> onChange(medicine.copy(mealTiming = MealTiming.entries.first { it.label == label })) }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Needs confirmation", modifier = Modifier.weight(1f))
                Switch(checked = medicine.needsConfirmation, onCheckedChange = { onChange(medicine.copy(needsConfirmation = it)) })
            }
        }
    }
}

@Composable
fun TestEditCard(test: LabTestInfo, onChange: (LabTestInfo) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SimpleTextField(test.name, "Test name", { onChange(PatientEducation.enrichTest(test.copy(name = it))) })
            SimpleTextField(test.explanation, "Test explanation", { onChange(test.copy(explanation = it)) }, singleLine = false)
            SimpleTextField(test.preparation, "Preparation instructions", { onChange(test.copy(preparation = it)) }, singleLine = false)
            SimpleTextField(test.sampleType, "Sample type", { onChange(test.copy(sampleType = it)) })
        }
    }
}

@Composable
fun ScheduleScreen(state: ClinixState, repository: ClinixRepository) {
    var mode by rememberSaveable { mutableStateOf("Today") }
    val patientDoses = state.scheduledDoses.filter { it.patientId == state.selectedPatientId }
    val context = LocalContext.current

    ScreenColumn {
        ClinixTopBar("Medicine schedule", "Pill organizer, reminders, adherence, and calendar handoff.")
        SafetyBanner("Follow the confirmed prescription label. Please verify with your doctor or pharmacist.")
        TabRow(selectedTabIndex = if (mode == "Today") 0 else 1) {
            Tab(selected = mode == "Today", onClick = { mode = "Today" }, text = { Text("Today") })
            Tab(selected = mode == "Weekly", onClick = { mode = "Weekly" }, text = { Text("Weekly") })
        }
        Spacer(Modifier.height(14.dp))
        if (patientDoses.isEmpty()) {
            EmptyState("No medicines scheduled", "Confirmed medicines from saved prescriptions will appear here.", Icons.Default.Medication)
        } else if (mode == "Today") {
            DoseTime.entries.forEach { time ->
                SectionHeader(time.label)
                val doses = patientDoses.filter { it.timeOfDay == time }
                if (doses.isEmpty()) Text("No ${time.label.lowercase()} medicines.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                doses.forEach { dose ->
                    DoseCard(
                        dose = dose,
                        onTaken = { repository.setDoseStatus(dose.id, DoseStatus.Taken) },
                        onSkipped = { repository.setDoseStatus(dose.id, DoseStatus.Skipped) },
                        onReminder = { repository.setDoseReminder(dose.id, !dose.reminderEnabled) },
                        onCalendar = {
                            val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                                .putExtra(CalendarContract.Events.TITLE, "Take ${dose.medicineName}")
                                .putExtra(CalendarContract.Events.DESCRIPTION, "${dose.strength} • ${dose.mealTiming.label}")
                            context.startActivity(intent)
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
            repeat(7) { day ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Day ${day + 1}", style = MaterialTheme.typography.titleMedium)
                        Text("${patientDoses.size} scheduled doses copied from confirmed plan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DoseCard(
    dose: ScheduledDose,
    onTaken: () -> Unit,
    onSkipped: () -> Unit,
    onReminder: () -> Unit,
    onCalendar: (() -> Unit)? = null
) {
    val statusColor = when (dose.status) {
        DoseStatus.Taken -> Mint
        DoseStatus.Skipped -> MaterialTheme.colorScheme.secondaryContainer
        DoseStatus.Pending -> MaterialTheme.colorScheme.surface
    }
    Card(colors = CardDefaults.cardColors(containerColor = statusColor), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Medication, null, tint = Teal)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(dose.medicineName, style = MaterialTheme.typography.titleMedium)
                    Text("${dose.strength.ifBlank { "Strength not recorded" }} • ${dose.timeOfDay.label} • ${dose.mealTiming.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onReminder) {
                    Icon(Icons.Default.Notifications, contentDescription = "Toggle reminder", tint = if (dose.reminderEnabled) Teal else MaterialTheme.colorScheme.outline)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onTaken, modifier = Modifier.weight(1f)) { Text("Taken") }
                OutlinedButton(onClick = onSkipped, modifier = Modifier.weight(1f)) { Text("Skipped") }
                if (onCalendar != null) IconButton(onClick = onCalendar) { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") }
            }
        }
    }
}

@Composable
fun VaultScreen(state: ClinixState, onOpen: (PrescriptionRecord) -> Unit, onCompare: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    val patientIds = state.patients.map { it.id }
    val filtered = state.prescriptions
        .filter { filter == "All" || it.patientId == filter || (filter == "Favorites" && it.favorite) }
        .filter {
            val text = listOf(it.doctorName, it.clinicName, it.condition, it.medicines.joinToString { med -> med.name }, it.tests.joinToString { test -> test.name }).joinToString(" ").lowercase()
            search.isBlank() || search.lowercase() in text
        }

    ScreenColumn {
        ClinixTopBar("Prescription vault", "Timeline, search, filters, detail, export, and reuse.")
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            label = { Text("Search prescriptions") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        Spacer(Modifier.height(10.dp))
        ChipRow(listOf("All", "Favorites") + patientIds, filter) { filter = it }
        SectionHeader("Timeline", action = if (state.prescriptions.size >= 2) "Compare" else null, onAction = if (state.prescriptions.size >= 2) onCompare else null)
        if (filtered.isEmpty()) EmptyState("No prescriptions saved yet", "Saved prescriptions appear here permanently with original file and extracted details.", Icons.Default.Description)
        else filtered.forEach { record ->
            PrescriptionCard(record, onClick = { onOpen(record) })
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun PrescriptionCard(record: PrescriptionRecord, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Mint),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = Teal)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.doctorName.ifBlank { "Doctor not recorded" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(record.condition.ifBlank { "Condition not recorded" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${record.medicines.size} medicines • ${record.tests.size} tests", style = MaterialTheme.typography.bodySmall)
            }
            Icon(if (record.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (record.favorite) Coral else MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun VaultDetailScreen(
    state: ClinixState,
    record: PrescriptionRecord,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    val patient = state.patients.firstOrNull { it.id == record.patientId }
    val warnings = SafetyChecker.warnings(record, patient?.allergies.orEmpty())
    val shareText = remember(record) { record.toShareSummary() }
    ScreenColumn {
        ClinixTopBar("Prescription detail", patient?.let { p -> p.name.ifBlank { p.relation } }) {
            Row {
                IconButton(onClick = onFavorite) { Icon(if (record.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite") }
                IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Close") }
            }
        }
        PrescriptionPreview(record.sourceUri, record.sourceType)
        SectionHeader("Prescription")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabelValue("Doctor", record.doctorName, Modifier.weight(1f))
                    LabelValue("Clinic", record.clinicName, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabelValue("Date", record.prescriptionDate, Modifier.weight(1f))
                    LabelValue("Follow-up", record.followUpDate, Modifier.weight(1f))
                }
                LabelValue("Condition", record.condition)
                LabelValue("Notes", record.notes)
            }
        }
        SectionHeader("Medicines")
        if (record.medicines.isEmpty()) EmptyState("No medicines saved", "This prescription has no confirmed medicine cards.", Icons.Default.Medication)
        record.medicines.forEach { medicine ->
            ExpandableCard(
                title = medicine.name.ifBlank { "Unnamed medicine" },
                subtitle = "${medicine.dosage.ifBlank { "Dosage missing" }} • ${medicine.frequency.ifBlank { "Frequency missing" }}"
            ) {
                LabelValue("Purpose", medicine.generalPurpose)
                LabelValue("Side effects", medicine.sideEffects)
                LabelValue("Warnings", medicine.warnings)
                LabelValue("Missed dose guidance", medicine.missedDoseGuidance)
                LabelValue("Storage", medicine.storageAdvice)
                SafetyBanner("Consult your doctor/pharmacist before changing or stopping any medicine.")
            }
            Spacer(Modifier.height(8.dp))
        }
        SectionHeader("Tests")
        if (record.tests.isEmpty()) EmptyState("No tests pending", "Detected tests will appear with explanations and reminders.", Icons.Default.LocalHospital)
        record.tests.forEach { test ->
            ExpandableCard(test.name.ifBlank { "Unnamed test" }, test.sampleType.ifBlank { "Sample type not recorded" }) {
                LabelValue("Explanation", test.explanation)
                LabelValue("Why doctors prescribe it", test.whyPrescribed)
                LabelValue("Preparation", test.preparation)
                FilledTonalButton(onClick = {}) { Text("Reminder") }
                TextButton(onClick = {}) { Text("Find nearby labs") }
            }
            Spacer(Modifier.height(8.dp))
        }
        SectionHeader("Safety checker")
        warnings.forEach { LabelValue(it.title, it.detail) }
        val lifestyle = PatientEducation.lifestyleSuggestions(record.condition)
        if (lifestyle.isNotEmpty()) {
            SectionHeader("Lifestyle suggestions")
            lifestyle.forEach { LabelValue("General education", it) }
            SafetyBanner("These are general educational suggestions only. ClinixGo does not diagnose or change treatment.")
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.RestartAlt, null)
                Spacer(Modifier.width(6.dp))
                Text("Restart")
            }
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, shareText)
                    context.startActivity(Intent.createChooser(intent, "Share prescription summary"))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(6.dp))
                Text("Export")
            }
        }
    }
}

@Composable
fun ProfileScreen(state: ClinixState, repository: ClinixRepository) {
    var selectedId by rememberSaveable(state.selectedPatientId) { mutableStateOf(state.selectedPatientId) }
    val profile = state.patients.firstOrNull { it.id == selectedId } ?: PatientProfile(id = selectedId)
    var draft by remember(profile) { mutableStateOf(profile) }

    ScreenColumn {
        ClinixTopBar("Profile & family", "Privacy, reminders, allergies, conditions, and separate records.")
        SectionHeader("Family members")
        ChipRow(state.patients.map { it.id } + "new", selectedId) {
            if (it == "new") {
                val member = PatientProfile(id = newId(), relation = "Other")
                repository.upsertPatient(member)
                selectedId = member.id
                draft = member
            } else {
                selectedId = it
                repository.selectPatient(it)
            }
        }
        SectionHeader("Details")
        SimpleTextField(draft.name, "Name", { draft = draft.copy(name = it) })
        SimpleTextField(draft.relation, "Relation", { draft = draft.copy(relation = it) })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SimpleTextField(draft.age, "Age", { draft = draft.copy(age = it) }, Modifier.weight(1f))
            SimpleTextField(draft.gender, "Gender", { draft = draft.copy(gender = it) }, Modifier.weight(1f))
        }
        SimpleTextField(draft.allergies, "Allergies", { draft = draft.copy(allergies = it) }, singleLine = false)
        SimpleTextField(draft.chronicConditions, "Chronic conditions", { draft = draft.copy(chronicConditions = it) }, singleLine = false)
        SimpleTextField(draft.emergencyContact, "Emergency contact", { draft = draft.copy(emergencyContact = it) })
        SimpleTextField(draft.preferredPharmacy, "Preferred pharmacy", { draft = draft.copy(preferredPharmacy = it) })
        SimpleTextField(draft.reminderPreference, "Reminder preferences", { draft = draft.copy(reminderPreference = it) })
        SettingSwitch("PIN lock concept", draft.privacyPinEnabled) { draft = draft.copy(privacyPinEnabled = it) }
        SettingSwitch("Biometric lock concept", draft.biometricLockEnabled) { draft = draft.copy(biometricLockEnabled = it) }
        Button(onClick = { repository.upsertPatient(draft) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Check, null)
            Spacer(Modifier.width(8.dp))
            Text("Save profile")
        }
        SafetyBanner("Local storage is designed for future encrypted storage, account login, cloud sync, backup, and restore.")
    }
}

@Composable
fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun AssistantScreen(state: ClinixState, aiService: PrescriptionAiService, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(
            listOf(
                AssistantMessage(false, "Ask about medicine purpose, timing labels, CBC, or a saved prescription summary. I cannot diagnose or change treatment.")
            )
        )
    }
    ScreenColumn {
        ClinixTopBar("AI healthcare assistant", "Safe explanations for saved prescription context.") {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Close") }
        }
        SafetyBanner("The assistant avoids diagnosis and treatment decisions. Please verify with your doctor or pharmacist.")
        messages.forEach { message ->
            Card(
                colors = CardDefaults.cardColors(containerColor = if (message.fromUser) Mint else MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Text(message.text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
        SectionHeader("Try asking")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("What is this medicine for?", "When should I take this?", "What does CBC mean?", "Summarize my prescription").forEach { prompt ->
                QuickAction(prompt, Icons.Default.ContentCopy, onClick = { input = prompt })
            }
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Ask a safe health question") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        Button(
            onClick = {
                val question = input.trim()
                if (question.isNotBlank()) {
                    messages = messages + AssistantMessage(true, question)
                    input = ""
                    scope.launch {
                        val answer = aiService.answerQuestion(question, state.prescriptions)
                        messages = messages + AssistantMessage(false, answer)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Chat, null)
            Spacer(Modifier.width(8.dp))
            Text("Send")
        }
    }
}

@Composable
fun FinderScreen(onBack: () -> Unit) {
    ScreenColumn {
        ClinixTopBar("Pharmacies & labs", "API-ready nearby care finder.") {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Close") }
        }
        SafetyBanner("Call to confirm availability before traveling.")
        ProviderSection(
            title = "Nearby pharmacies",
            emptyTitle = "No nearby pharmacies loaded",
            icon = Icons.Default.LocalPharmacy
        )
        ProviderSection(
            title = "Nearby labs",
            emptyTitle = "No nearby labs loaded",
            icon = Icons.Default.LocalHospital
        )
    }
}

@Composable
fun ProviderSection(title: String, emptyTitle: String, icon: ImageVector) {
    SectionHeader(title)
    EmptyState(emptyTitle, "Connect a Maps, pharmacy, or lab availability API to populate distance and availability cards.", icon)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Provider card template", style = MaterialTheme.typography.titleMedium)
            Text("Distance placeholder • Availability placeholder", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Call, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Call")
                }
                OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text("WhatsApp")
                }
            }
            Text("Call to confirm availability", style = MaterialTheme.typography.bodySmall, color = Coral)
        }
    }
}

@Composable
fun CompareScreen(state: ClinixState, onBack: () -> Unit) {
    val prescriptions = state.prescriptions.take(2)
    val notes = if (prescriptions.size >= 2) SafetyChecker.compare(prescriptions[1], prescriptions[0]) else emptyList()
    ScreenColumn {
        ClinixTopBar("Compare prescriptions", "Added, removed, changed medicines, and repeated tests.") {
            IconButton(onClick = onBack) { Icon(Icons.Default.Close, "Close") }
        }
        if (prescriptions.size < 2) EmptyState("Need two prescriptions", "Save another prescription to compare changes.", Icons.Default.Description)
        else notes.forEach { note ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                Text(note, modifier = Modifier.padding(14.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun PrescriptionPreview(uri: String, sourceType: SourceType) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (sourceType == SourceType.Pdf || uri.isBlank() || sourceType == SourceType.Demo) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.7f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Sky),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (sourceType == SourceType.Pdf) Icons.Default.Description else Icons.Default.Image, null, tint = Teal, modifier = Modifier.size(54.dp))
                }
            } else {
                val bitmap = remember(uri) {
                    runCatching {
                        context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }
                    }.getOrNull()
                }
                if (bitmap != null) {
                    ComposeImage(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Prescription preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.25f)
                            .clip(MaterialTheme.shapes.medium)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.7f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Sky),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, null, tint = Teal, modifier = Modifier.size(54.dp))
                    }
                }
            }
            Text(sourceType.name, style = MaterialTheme.typography.labelLarge)
            Text(uri.ifBlank { "Preview placeholder" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun PrescriptionRecord.toShareSummary(): String {
    val meds = medicines.joinToString("\n") { "- ${it.name}: ${it.dosage}, ${it.frequency}, ${it.timing}" }.ifBlank { "No medicines recorded" }
    val labs = tests.joinToString("\n") { "- ${it.name}: ${it.preparation}" }.ifBlank { "No tests recorded" }
    return """
        ClinixGo prescription summary
        Doctor: ${doctorName.ifBlank { "Not recorded" }}
        Clinic: ${clinicName.ifBlank { "Not recorded" }}
        Date: ${prescriptionDate.ifBlank { "Not recorded" }}
        Follow-up: ${followUpDate.ifBlank { "Not recorded" }}
        Condition: ${condition.ifBlank { "Not recorded" }}

        Medicines:
        $meds

        Tests:
        $labs

        Please verify with your doctor or pharmacist.
    """.trimIndent()
}
