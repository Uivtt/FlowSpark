package com.flowspark.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
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

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户选择后无需额外操作 */ }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // targetSdk 35 默认启用 edge-to-edge,让 Compose Scaffold 接管 inset 管理
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Android 13+ 请求通知权限(前台服务需要通知显示,否则用户看不到执行进度)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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
