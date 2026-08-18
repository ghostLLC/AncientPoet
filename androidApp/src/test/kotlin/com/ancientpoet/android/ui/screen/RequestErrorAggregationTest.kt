package com.ancientpoet.android.ui.screen

import com.ancientpoet.android.ui.screen.conversation.ConversationState
import com.ancientpoet.android.ui.screen.conversation.MsgItem
import com.ancientpoet.android.ui.screen.conversation.OperationError
import com.ancientpoet.android.ui.screen.poet.PoetOperationError
import com.ancientpoet.android.ui.screen.poet.PoetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestErrorAggregationTest {
    @Test
    fun conversationSuccessDoesNotClearTheOtherRequestError() {
        val state = ConversationState(
            conversationError = OperationError("故事线加载失败", retryable = true),
        )

        val afterMessagesSuccess = state.copy(
            messages = listOf(MsgItem(1, "assistant", "答曰", null)),
            messagesError = null,
        )

        assertEquals("故事线加载失败", afterMessagesSuccess.errorMessage)
        assertTrue(afterMessagesSuccess.canRetry)
    }

    @Test
    fun poetDetailSuccessDoesNotClearLifeEventsError() {
        val state = PoetState(
            lifeEventsError = PoetOperationError("生平加载失败", retryable = true),
        )

        val afterDetailSuccess = state.copy(detailError = null)

        assertEquals("生平加载失败", afterDetailSuccess.errorMessage)
        assertTrue(afterDetailSuccess.canRetry)
    }
}
