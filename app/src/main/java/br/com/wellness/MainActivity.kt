package br.com.wellness

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.remember
import br.com.wellness.ui.WellnessApp
import br.com.wellness.ui.theme.WellnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarColors()
        setContent {
            val factory = remember(application) { ViewModelProvider.AndroidViewModelFactory(application) }
            WellnessTheme { WellnessApp(viewModel(factory = factory)) }
        }
    }

    @Suppress("DEPRECATION") // On Android 15 the transparent bars also sit over the black background.
    private fun applySystemBarColors() {
        val background = getColor(R.color.wellness_background)
        window.decorView.setBackgroundColor(background)
        window.statusBarColor = background
        window.navigationBarColor = background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = background
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }
}
