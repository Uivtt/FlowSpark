package com.flowspark.app.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.flowspark.app.domain.model.Step
import com.flowspark.app.domain.model.StepType
import com.flowspark.app.ui.theme.Indigo100
import com.flowspark.app.ui.theme.Indigo500

/** 工作流步骤卡片（垂直流水线中的单个卡片） */
@Composable
fun StepCard(
    step: Step,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 步骤序号 + 图标
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Indigo100),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = step.type.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Indigo500,
                    )
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Indigo500,
                    )
                }
            }

            // 步骤描述
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.type.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (step.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (step.params.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = step.params.entries.joinToString(" · ") { "${it.key}=${it.value}" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 云端/本地标记
            Text(
                text = if (step.type.isCloud) "☁️" else "📱",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

// ========== 扩展：StepType 的显示属性 ==========

private val StepType.displayName: String
    get() = when (this) {
        StepType.GRAYSCALE -> "灰度"
        StepType.CROP -> "裁剪"
        StepType.BRIGHTNESS -> "亮度"
        StepType.CONTRAST -> "对比度"
        StepType.SCALE -> "缩放"
        StepType.TEXT_TO_IMAGE -> "文生图"
        StepType.IMAGE_TO_IMAGE -> "图生图"
    }

private val StepType.icon: ImageVector
    get() = when (this) {
        StepType.TEXT_TO_IMAGE, StepType.IMAGE_TO_IMAGE -> Icons.Default.Image
        else -> Icons.Default.Tune
    }
