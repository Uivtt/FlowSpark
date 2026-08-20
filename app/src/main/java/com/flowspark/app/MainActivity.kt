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
import com.flowspark.app.ui.screens.OnboardingScreen
import com.flowspark.app.ui.screens.SettingsScreen
import com.flowspark.app.ui.theme.FlowSparkTheme

class MainActivity : ComponentActivity() {

    // 单张图片选择
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setInputImage(it) }
    }

    // 批量图片选择（Android 4.4+ 多选）
    private val pickBatchImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.setBatchImages(uris)
        }
    }

    // 导入工作流文件
    private val importWorkflowLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importWorkflow(it) }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Android 13+ 通知权限
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
            val showOnboarding by viewModel.showOnboarding.collectAsState()
            val aiProviderConfig by viewModel.aiProviderConfig.collectAsState()

            FlowSparkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when {
                        showOnboarding -> {
                            // 新手引导
                            OnboardingScreen(
                                onComplete = { viewModel.completeOnboarding() },
                            )
                        }
                        showSettings -> {
                            // 配置界面
                            SettingsScreen(
                                currentConfig = aiProviderConfig,
                                onSave = { config ->
                                    viewModel.updateProviderConfig(config)
                                    viewModel.closeSettings()
                                },
                                onBack = { viewModel.closeSettings() },
                            )
                        }
                        else -> {
                            // 主界面
                            HomeScreen(
                                viewModel = viewModel,
                                onPickImage = { pickImageLauncher.launch("image/*") },
                                onPickBatchImages = { pickBatchImagesLauncher.launch("image/*") },
                                onImportWorkflow = { importWorkflowLauncher.launch("application/json") },
                                onOpenSettings = { viewModel.openSettings() },
                            )
                        }
                    }
                }
            }
        }
    }
}
