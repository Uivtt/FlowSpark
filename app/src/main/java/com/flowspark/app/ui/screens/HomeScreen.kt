package com.flowspark.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.flowspark.app.ui.ChatMessage
import com.flowspark.app.ui.MainViewModel
import com.flowspark.app.ui.screens.components.ChatInputBar
import com.flowspark.app.ui.screens.components.ImagePreview
import com.flowspark.app.ui.screens.components.StepCard
import com.flowspark.app.ui.theme.Indigo500
import com.flowspark.app.ui.theme.Rose500
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPickImage: () -> Unit,
    onPickBatchImages: () -> Unit = {},
    onImportWorkflow: () -> Unit = {},
    onOpenSettings: () -> Unit,
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val parsing by viewModel.parsing.collectAsState()
    val pendingWorkflow by viewModel.pendingWorkflow.collectAsState()
    val executionState by viewModel.executionState.collectAsState()
    val previewUri by viewModel.previewUri.collectAsState()
    val inputImageUri by viewModel.inputImageUri.collectAsState()
    val estimatedCost by viewModel.estimatedCost.collectAsState()
    val batchProgress by viewModel.batchProgress.collectAsState()
    val batchUris by viewModel.batchUris.collectAsState()

    // 拖拽排序状态
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FlowSpark") },
                actions = {
                    if (batchUris.isNotEmpty()) {
                        Text(
                            text = "${batchUris.size}张",
                            style = MaterialTheme.typography.labelMedium,
                            color = Indigo500,
                        )
                    }
                    IconButton(onClick = onImportWorkflow) {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = "导入工作流",
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "配置",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                enabled = !parsing && !executionState.running,
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = if (isLargeScreen) 48.dp else 12.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 顶部：图片预览 + 操作按钮
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (previewUri != null) {
                                ImagePreview(uri = previewUri)
                            } else {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    onClick = onPickImage,
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "选择图片",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 工具按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onPickImage,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("选图", style = MaterialTheme.typography.labelMedium)
                        }
                        if (previewUri != null) {
                            FilledTonalButton(
                                onClick = { viewModel.shareImage() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("分享", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        FilledTonalButton(
                            onClick = onPickBatchImages,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("批量", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 批量进度
            batchProgress?.let { (current, total) ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "批量处理: $current / $total",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (total > 0) current.toFloat() / total else 0f },
                                modifier = Modifier.fillMaxWidth(),
                                color = Indigo500,
                            )
                        }
                    }
                }
            }

            // 待确认工作流 + 成本预估 + 导出按钮
            pendingWorkflow?.let { workflow ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Indigo500.copy(alpha = 0.08f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = workflow.summary,
                                style = MaterialTheme.typography.titleMedium,
                            )

                            // 成本预估
                            if (estimatedCost.isNotBlank()) {
                                Text(
                                    text = "💰 预估费用: $estimatedCost",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            // 步骤卡片（可拖拽排序）
                            workflow.steps.forEachIndexed { index, step ->
                                StepCard(
                                    step = step,
                                    index = index,
                                    showDragHandle = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { draggedItemIndex = index },
                                                onDragEnd = {
                                                    if (draggedItemIndex >= 0) {
                                                        // 计算拖拽目标位置
                                                        val target = index
                                                        if (target != draggedItemIndex) {
                                                            viewModel.reorderSteps(draggedItemIndex, target)
                                                        }
                                                        draggedItemIndex = -1
                                                    }
                                                },
                                                onDragCancel = { draggedItemIndex = -1 },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                },
                                            )
                                        },
                                )
                            }

                            // 操作按钮
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (batchUris.isNotEmpty()) {
                                    Button(
                                        onClick = { viewModel.executeBatch() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                                    ) {
                                        Text("批量执行 (${batchUris.size})")
                                    }
                                }
                                Button(
                                    onClick = { viewModel.confirmExecution() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
                                ) {
                                    Text("确认执行")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.cancelWorkflow() },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("取消")
                                }
                                IconButton(onClick = { viewModel.exportWorkflow() }) {
                                    Icon(
                                        Icons.Filled.FileDownload,
                                        contentDescription = "导出工作流",
                                        tint = Indigo500,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 执行进度
            if (executionState.running) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = executionState.message,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (executionState.totalSteps > 0)
                                        (executionState.currentStepIndex + 1).toFloat() / executionState.totalSteps
                                    else 0f
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = Indigo500,
                            )
                        }
                    }
                }
            }

            // 执行错误
            executionState.error?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Rose500.copy(alpha = 0.1f),
                        ),
                    ) {
                        Text(
                            text = "❌ $error",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Rose500,
                        )
                    }
                }
            }

            // 对话消息流
            items(messages, key = { it.id }) { msg ->
                when (msg.role) {
                    ChatMessage.Role.USER -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Text(
                                text = msg.text,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    ChatMessage.Role.ASSISTANT -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = msg.text,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    ChatMessage.Role.SYSTEM -> {
                        Text(
                            text = msg.text,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 解析中加载指示器
            if (parsing) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Indigo500,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "正在理解你的需求…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 底部留白
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
