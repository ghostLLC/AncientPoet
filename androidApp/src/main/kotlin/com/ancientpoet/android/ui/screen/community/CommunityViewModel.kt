package com.ancientpoet.android.ui.screen.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ancientpoet.shared.data.api.AncientPoetApi
import com.ancientpoet.shared.data.api.ApiResult
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class CommunityViewModel(private val api: AncientPoetApi) : ViewModel() {
    private val _state = MutableStateFlow(CommunityState())
    val state: StateFlow<CommunityState> = _state

    fun loadPosts() {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<PostListRes>("community/posts")) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    posts = result.value.posts.map { it.toItem() },
                    isLoading = false,
                    errorMessage = null,
                )
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun toggleLike(postId: Long) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.post<MessageResponse>("community/posts/$postId/like")) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = null)
                    loadPosts()
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun loadComments(postId: Long) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<List<CommentRes>>("community/posts/$postId/comments")) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    comments = result.value.map { CommentItem(it.id, it.userId, it.userNickname, it.content) },
                    isLoading = false,
                    errorMessage = null,
                )
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun addComment(postId: Long, content: String) {
        if (content.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "评论内容不能为空", canRetry = false)
            return
        }
        viewModelScope.launch {
            setLoading()
            when (val result = api.post<MessageResponse>("community/posts/$postId/comments") { setBody(CommentReq(content)) }) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = null)
                    loadComments(postId)
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    fun loadProfile(userId: Long) {
        viewModelScope.launch {
            setLoading()
            when (val result = api.get<ProfileRes>("user/profile/$userId")) {
                is ApiResult.Success -> {
                    val profile = result.value
                    _state.value = _state.value.copy(
                        profileName = profile.nickname,
                        profileBio = profile.bio,
                        postCount = profile.postCount,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is ApiResult.Failure -> setFailure(result)
            }
        }
    }

    private fun setLoading() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null, canRetry = false)
    }

    private fun setFailure(result: ApiResult.Failure) {
        _state.value = _state.value.copy(isLoading = false, errorMessage = result.message, canRetry = result.retryable)
    }
}

data class CommunityState(
    val posts: List<PostItem> = emptyList(),
    val comments: List<CommentItem> = emptyList(),
    val profileName: String? = null,
    val profileBio: String? = null,
    val postCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
)

data class PostItem(val id: Long, val userId: Long, val userNickname: String?, val contentText: String?, val sharedMessageIds: List<Long>?, val likeCount: Int, val commentCount: Int, val isLiked: Boolean)
data class CommentItem(val id: Long, val userId: Long, val userNickname: String?, val content: String)

@Serializable data class PostListRes(val posts: List<PostRes>)
@Serializable data class PostRes(val id: Long, val userId: Long, val userNickname: String?, val contentText: String?, val sharedMessageIds: List<Long>? = null, val likeCount: Int, val commentCount: Int, val isLiked: Boolean = false)
@Serializable data class CommentRes(val id: Long, val userId: Long, val userNickname: String?, val content: String)
@Serializable data class CommentReq(val content: String)
@Serializable data class ProfileRes(val id: Long, val nickname: String?, val bio: String?, val avatarUrl: String?, val postCount: Int)
@Serializable data class MessageResponse(val message: String? = null)

private fun PostRes.toItem() = PostItem(id, userId, userNickname, contentText, sharedMessageIds, likeCount, commentCount, isLiked)
