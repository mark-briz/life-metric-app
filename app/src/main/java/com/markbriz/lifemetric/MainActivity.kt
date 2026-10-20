package com.markbriz.lifemetric

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.markbriz.lifemetric.ui.AppViewModel
import com.markbriz.lifemetric.ui.LifeMetricApp
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import com.markbriz.lifemetric.ui.theme.materialColorScheme
import com.markbriz.lifemetric.ui.theme.themeById

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val colors = themeById(state.themeId)
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = colors.lightStatusBar
            }
            CompositionLocalProvider(LocalAppColors provides colors) {
                MaterialTheme(colorScheme = colors.materialColorScheme()) {
                    LifeMetricApp(viewModel)
                }
            }
        }
    }
}
