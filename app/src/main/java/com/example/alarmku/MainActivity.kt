
package com.example.alarmku

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                AlarmKuScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmKuScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    var alarms by remember { mutableStateOf(AlarmData.load(context)) }
    var showPicker by remember { mutableStateOf(false) }

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
            Text("AlarmKu", style = MaterialTheme.typography.headlineLarge)

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
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tambah Alarm")
            }

            Text("Daftar Alarm (${alarms.size})",
                style = MaterialTheme.typography.titleLarge)

            if (alarms.isEmpty()) {
                Text("Belum ada alarm. Tekan Tambah Alarm.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        String.format(
                                            Locale.getDefault(),
                                            "%02d:%02d",
                                            alarm.hour,
                                            alarm.minute
                                        ),
                                        fontSize = 30.sp
                                    )
                                    Text(if (alarm.enabled) "Aktif" else "Nonaktif")
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
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

    if (showPicker) {
        val timeState = rememberTimePickerState(
            initialHour = LocalTime.now().hour,
            initialMinute = LocalTime.now().minute
        )

        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Pilih Waktu Alarm") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nextId = (alarms.maxOfOrNull { it.id } ?: 0) + 1
                        val alarm = AlarmItem(
                            id = nextId,
                            hour = timeState.hour,
                            minute = timeState.minute
                        )
                        val updated = alarms + alarm
                        updateAlarms(updated)

                        val manager = context.getSystemService(
                            Context.ALARM_SERVICE
                        ) as AlarmManager

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
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
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
