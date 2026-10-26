
package com.example.alarmku

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class RingingActivity : ComponentActivity() {


    private var alarmId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getIntExtra("alarm_id", 0)


        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("AlarmKu", fontSize = 32.sp)
                    Text("WAKTUNYA BANGUN!", fontSize = 24.sp)

                    Button(
                        onClick = { stopAlarm() }
                    ) {
                        Text("STOP")
                    }
                }
            }
        }
    }

    private fun stopAlarm() {
        val stopIntent = Intent(
            this,
            AlarmSoundService::class.java
        ).apply {
            action = AlarmSoundService.ACTION_STOP
        }

        startService(stopIntent)

        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.cancel(alarmId)
        manager.cancel(alarmId + 1000)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
