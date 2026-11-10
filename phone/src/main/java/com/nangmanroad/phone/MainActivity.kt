package com.nangmanroad.phone

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nangmanroad.core.ui.Palette
import com.nangmanroad.phone.ui.PhoneApp
import com.nangmanroad.phone.ui.theme.PhoneTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PhoneViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Palette.followNightMode(resources.configuration)
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            PhoneTheme {
                PhoneApp(state = state, actions = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setForeground(true)
    }

    override fun onStop() {
        viewModel.setForeground(false)
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Palette.followNightMode(newConfig)
    }
}
