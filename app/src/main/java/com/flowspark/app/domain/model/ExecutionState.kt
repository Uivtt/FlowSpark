package com.flowspark.app.domain.model

/** 工作流执行状态（驱动 UI 进度） */
data class ExecutionState(
    val running: Boolean = false,
    val currentStepIndex: Int = -1,
    val totalSteps: Int = 0,
    val currentStepDescription: String = "",
    val message: String = "",
    val error: String? = null,
)
