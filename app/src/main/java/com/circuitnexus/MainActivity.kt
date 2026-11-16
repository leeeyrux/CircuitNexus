package com.circuitnexus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import com.circuitnexus.di.AppContainer
import com.circuitnexus.navigation.AppNavHost
import com.circuitnexus.ui.theme.CircuitNexusTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy {
        (application as CircuitNexusApplication).container
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container.seedExamplesIfNeeded()
        setContent {
            CircuitNexusTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(container = container)
                }
            }
        }
    }
}