package com.example.alarmku

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {
            MaterialTheme {
                AlarmKuScreen()
            }
        }
    }
}

private val dayOptions = listOf(
    Calendar.MONDAY to "Sen",
    Calendar.TUESDAY to "Sel",
    Calendar.WEDNESDAY to "Rab",
    Calendar.THURSDAY to "Kam",
    Calendar.FRIDAY to "Jum",
    Calendar.SATURDAY to "Sab",
    Calendar.SUNDAY to "Min"
)

private fun dayDescription(days: Set<Int>): String {
    if (days.isEmpty()) return "Tidak ada hari dipilih"

    return dayOptions
        .filter { it.first in days }
        .joinToString(", ") { it.second }
}

private fun getMusicDisplayName(context: Context, musicUri: String?): String? {
    if (musicUri.isNullOrBlank()) return null

    return try {
        val uri = Uri.parse(musicUri)
        var displayName: String? = null

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex)
                }
            }
        }

        displayName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment
            ?: "File musik terpilih"
    } catch (_: Exception) {
        "File musik terpilih"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmKuScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var alarms by remember { mutableStateOf(AlarmData.load(context)) }
    var showPicker by remember { mutableStateOf(false) }
    var selectedDays by remember {
        mutableStateOf(
            setOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )
        )
    }
    var selectedMusicUri by remember { mutableStateOf<String?>(null) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var isPreviewLoading by remember { mutableStateOf(false) }

    fun stopMusicPreview() {
        val player = previewPlayer
        previewPlayer = null
        isPreviewPlaying = false
        isPreviewLoading = false

        if (player != null) {
            try {
                player.setOnPreparedListener(null)
                player.setOnCompletionListener(null)
                player.setOnErrorListener(null)
                if (player.isPlaying) player.stop()
            } catch (_: Exception) {
                // Pemutar mungkin sudah berhenti atau belum siap.
            }
            try {
                player.release()
            } catch (_: Exception) {
                // Tidak ada tindakan jika pemutar sudah dilepas.
            }
        }
    }

    val musicPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            stopMusicPreview()
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Beberapa penyedia file tidak mendukung izin persisten.
            }
            selectedMusicUri = uri.toString()
        }
    }

    val selectedMusicName = remember(selectedMusicUri) {
        getMusicDisplayName(context, selectedMusicUri)
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val dateFormatter = DateTimeFormatter.ofPattern(
        "EEEE, dd MMMM yyyy",
        Locale("id", "ID")
    )

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000)
        }
    }

    fun updateAlarms(updated: List<AlarmItem>) {
        alarms = updated
        AlarmData.save(context, updated)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "AlarmKu",
                style = MaterialTheme.typography.headlineLarge
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Waktu Sekarang")
                    Text(currentTime.format(timeFormatter), fontSize = 38.sp)
                    Text(LocalDate.now().format(dateFormatter))
                }
            }

            Button(
                onClick = {
                    stopMusicPreview()
                    selectedDays = setOf(
                        Calendar.MONDAY,
                        Calendar.TUESDAY,
                        Calendar.WEDNESDAY,
                        Calendar.THURSDAY,
                        Calendar.FRIDAY
                    )
                    selectedMusicUri = null
                    showPicker = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tambah Alarm")
            }

            Text(
                "Daftar Alarm (${alarms.size})",
                style = MaterialTheme.typography.titleLarge
            )

            if (alarms.isEmpty()) {
                Text("Belum ada alarm. Tekan Tambah Alarm.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            String.format(
                                                Locale.getDefault(),
                                                "%02d:%02d",
                                                alarm.hour,
                                                alarm.minute
                                            ),
                                            fontSize = 30.sp
                                        )
                                        Text(dayDescription(alarm.daysOfWeek))
                                        Text(
                                            getMusicDisplayName(context, alarm.musicUri)
                                                ?: "Nada alarm bawaan",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            if (alarm.enabled) "Aktif" else "Nonaktif"
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Switch(
                                            checked = alarm.enabled,
                                            onCheckedChange = { enabled ->
                                                val updated = alarm.copy(enabled = enabled)

                                                if (enabled) {
                                                    AlarmScheduler.schedule(context, updated)
                                                } else {
                                                    AlarmScheduler.cancel(context, updated)
                                                }

                                                updateAlarms(
                                                    alarms.map {
                                                        if (it.id == alarm.id) updated else it
                                                    }
                                                )
                                            }
                                        )

                                        TextButton(
                                            onClick = {
                                                AlarmScheduler.cancel(context, alarm)
                                                updateAlarms(
                                                    alarms.filterNot { it.id == alarm.id }
                                                )
                                            }
                                        ) {
                                            Text("Hapus")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        val timeState = rememberTimePickerState(
            initialHour = LocalTime.now().hour,
            initialMinute = LocalTime.now().minute
        )

        AlertDialog(
            onDismissRequest = {
                stopMusicPreview()
                showPicker = false
            },
            title = { Text("Tambah Alarm") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Pilih waktu")
                    TimePicker(state = timeState)

                    Text(
                        "Ulangi pada hari",
                        style = MaterialTheme.typography.titleMedium
                    )

                    dayOptions.chunked(4).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowDays.forEach { (day, label) ->
                                FilterChip(
                                    selected = day in selectedDays,
                                    onClick = {
                                        selectedDays = if (day in selectedDays) {
                                            selectedDays - day
                                        } else {
                                            selectedDays + day
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            selectedDays = dayOptions.map { it.first }.toSet()
                        }
                    ) {
                        Text("Pilih setiap hari")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Musik alarm",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        selectedMusicName ?: "Nada alarm bawaan",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            musicPicker.launch(arrayOf("audio/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pilih Musik dari HP")
                    }

                    if (selectedMusicUri != null) {
                        Button(
                            onClick = {
                                if (previewPlayer != null || isPreviewLoading || isPreviewPlaying) {
                                    stopMusicPreview()
                                } else {
                                    try {
                                        isPreviewLoading = true
                                        val player = MediaPlayer()
                                        previewPlayer = player
                                        player.setDataSource(
                                            context,
                                            Uri.parse(selectedMusicUri)
                                        )
                                        player.setOnPreparedListener { preparedPlayer ->
                                            isPreviewLoading = false
                                            isPreviewPlaying = true
                                            preparedPlayer.start()
                                        }
                                        player.setOnCompletionListener {
                                            stopMusicPreview()
                                        }
                                        player.setOnErrorListener { _, _, _ ->
                                            stopMusicPreview()
                                            true
                                        }
                                        player.prepareAsync()
                                    } catch (_: Exception) {
                                        stopMusicPreview()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when {
                                    isPreviewLoading -> "Memuat musik... (tekan untuk berhenti)"
                                    isPreviewPlaying -> "Hentikan Preview"
                                    else -> "Putar Preview"
                                }
                            )
                        }

                        TextButton(
                            onClick = {
                                stopMusicPreview()
                                selectedMusicUri = null
                            }
                        ) {
                            Text("Gunakan Nada Bawaan")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedDays.isNotEmpty()) {
                            stopMusicPreview()
                            val nextId = (alarms.maxOfOrNull { it.id } ?: 0) + 1

                            val alarm = AlarmItem(
                                id = nextId,
                                hour = timeState.hour,
                                minute = timeState.minute,
                                enabled = true,
                                daysOfWeek = selectedDays,
                                musicUri = selectedMusicUri
                            )

                            val updated = alarms + alarm
                            updateAlarms(updated)

                            val manager = context.getSystemService(
                                Context.ALARM_SERVICE
                            ) as AlarmManager

                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                !manager.canScheduleExactAlarms()
                            ) {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                AlarmScheduler.schedule(context, alarm)
                            }

                            showPicker = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        stopMusicPreview()
                        showPicker = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}
