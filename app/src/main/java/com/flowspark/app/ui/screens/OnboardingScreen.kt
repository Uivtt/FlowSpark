package com.flowspark.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowspark.app.ui.theme.Indigo500

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(0) }
    val steps = listOf(
        StepData(
            "👋",
            "欢迎使用 FlowSpark",
            "零门槛对话式 AI 视觉工作流工具\n\n说句话就能编辑图片，就像聊天一样简单。",
            "开始体验",
        ),
        StepData(
            "💬",
            "第一步：说话",
            "在底部输入框说出你想做的事，例如：\n\n「调亮一点，然后弄成黑白」\n「把背景变模糊」\n「画一只猫在草地上」",
            "下一步",
        ),
        StepData(
            "✅",
            "第二步：确认",
            "AI 会理解你的需求并生成工作流卡片。\n确认无误后点击「确认执行」。\n\n🔹 蓝色 = 云端处理（需网络）\n📱 灰色 = 本地处理（无需网络）",
            "下一步",
        ),
        StepData(
            "🎉",
            "第三步：出图",
            "执行完成后图片自动显示在预览区。\n\n你可以继续对话调整，或点击 ⚙️ 切换 AI 供应商。\n\n现在开始试试吧！",
            "开始使用",
        ),
    )

    val current = steps[step]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = current.emoji,
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = current.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = current.body,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "${step + 1} / ${steps.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (step < steps.size - 1) step++
                    else onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500),
            ) {
                Text(
                    text = current.buttonText,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private data class StepData(
    val emoji: String,
    val title: String,
    val body: String,
    val buttonText: String,
)
