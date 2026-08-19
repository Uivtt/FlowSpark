package com.flowspark.app.domain.executor

import android.graphics.Bitmap
import com.flowspark.app.domain.image.ImageTool
import com.flowspark.app.domain.image.ImageToolRegistry
import com.flowspark.app.domain.model.ExecutionState
import com.flowspark.app.domain.model.Step
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 有序流水线执行器。
 *
 * 基于 [Flow]，每一步执行前调用 [kotlinx.coroutines.ensureActive]
 * 保证取消能立即传播（弥补 flatMapConcat 取消传播不及时的问题）。
 *
 * @param localTools 本地图像工具注册表
 * @param cloudHandler 云端步骤处理器（文生图/图生图），可为空（离线模式）
 */
class LinearExecutor(
    private val localTools: ImageToolRegistry,
    private val cloudHandler: CloudStepHandler? = null,
) {
    /**
     * 执行步骤序列，发出进度状态。
     *
     * @param steps 待执行步骤
     * @param inputImage 输入图片（第一个需要输入图片的步骤开始使用）
     */
    fun execute(steps: List<Step>, inputImage: Bitmap?): Flow<ExecutionState> = flow {
        var current: Bitmap? = inputImage
        val total = steps.size
        var index = 0

        for (step in steps) {
            currentCoroutineContext().ensureActive()
            emit(
                ExecutionState(
                    running = true,
                    currentStepIndex = index,
                    totalSteps = total,
                    currentStepDescription = step.description.ifBlank { step.type.name },
                    message = "步骤 ${index + 1}/$total",
                )
            )

            current = when {
                step.type.isCloud -> {
                    requireNotNull(cloudHandler) { "云端步骤需要 CloudStepHandler，当前为离线模式" }
                    val result = cloudHandler.handle(step, current)
                    result
                }
                else -> {
                    val tool: ImageTool = localTools.resolve(step.type)
                        ?: throw IllegalArgumentException("未注册的工具: ${step.type}")
                    requireNotNull(current) { "本地工具 ${step.type} 需要输入图片" }
                    tool.apply(current, step.params)
                }
            }

            emit(
                ExecutionState(
                    running = index < total - 1,
                    currentStepIndex = index,
                    totalSteps = total,
                    currentStepDescription = step.description,
                    message = "步骤 ${index + 1}/$total 完成",
                )
            )
            index++
        }

        emit(
            ExecutionState(
                running = false,
                currentStepIndex = total - 1,
                totalSteps = total,
                message = "全部完成",
            )
        )
    }
}

/** 云端步骤处理器：接收当前位图（可为 null），返回处理后的位图 */
fun interface CloudStepHandler {
    suspend fun handle(step: Step, current: Bitmap?): Bitmap
}
