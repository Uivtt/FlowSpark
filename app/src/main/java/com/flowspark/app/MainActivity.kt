package com.flowspark.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowspark.app.ui.MainViewModel
import com.flowspark.app.ui.screens.HomeScreen
import com.flowspark.app.ui.screens.SettingsScreen
import com.flowspark.app.ui.theme.FlowSparkTheme

class MainActivity : ComponentActivity() {

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setInputImage(it) }
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            viewModel = viewModel()
            val showSettings by viewModel.showSettings.collectAsState()
            val aiProviderConfig by viewModel.aiProviderConfig.collectAsState()

            FlowSparkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (showSettings) {
                        // 配置界面
                        SettingsScreen(
                            currentConfig = aiProviderConfig,
                            onSave = { config ->
                                viewModel.updateProviderConfig(config)
                                viewModel.closeSettings()
                            },
                            onBack = { viewModel.closeSettings() },
                        )
                    } else {
                        // 主界面
                        HomeScreen(
                            viewModel = viewModel,
                            onPickImage = { pickImageLauncher.launch("image/*") },
                            onOpenSettings = { viewModel.openSettings() },
                        )
                    }
                }
            }
        }
    }
}
