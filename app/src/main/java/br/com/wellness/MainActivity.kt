package br.com.wellness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.wellness.ui.WellnessApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF266859), onPrimary = Color.White,
                primaryContainer = Color(0xFFD5EAE0), onPrimaryContainer = Color(0xFF123B30),
                secondary = Color(0xFF716042), secondaryContainer = Color(0xFFF1E6D2),
                background = Color(0xFFF6F7F2), surface = Color(0xFFF6F7F2),
                surfaceVariant = Color(0xFFE6EAE2), onSurface = Color(0xFF202D28)
            )) { WellnessApp(viewModel()) }
        }
    }
}
