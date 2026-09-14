package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.repository.AnimeRepository
import com.example.ui.SakuraMainApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    try {
      val database = AppDatabase.getDatabase(applicationContext)
      val repository = AnimeRepository(database.animeDao())

      setContent {
        SakuraMainApp(repository = repository)
      }
    } catch (e: Throwable) {
      Log.e("MainActivity", "Failed to initialize app gracefully", e)
      setContent {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090C14)),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "AniHub",
              color = Color(0xFFF5EEDC),
              fontWeight = FontWeight.Bold,
              fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { recreate() }) {
              Text("Restart App")
            }
          }
        }
      }
    }
  }
}

