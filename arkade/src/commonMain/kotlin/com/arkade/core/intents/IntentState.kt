package com.arkade.core.intents

enum class IntentState {
    WAITING_TO_SUBMIT,
    WAITING_FOR_BATCH,
    BATCH_IN_PROGRESS,
    BATCH_FAILED,
    BATCH_SUCCEEDED,
    CANCELLED,
}
