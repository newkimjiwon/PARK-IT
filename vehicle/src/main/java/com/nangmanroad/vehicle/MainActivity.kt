package com.nangmanroad.vehicle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nangmanroad.vehicle.ui.NangmanRoadApp
import com.nangmanroad.vehicle.ui.theme.NangmanRoadTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            NangmanRoadTheme {
                NangmanRoadApp(state = state, actions = viewModel)
            }
        }
    }
}
