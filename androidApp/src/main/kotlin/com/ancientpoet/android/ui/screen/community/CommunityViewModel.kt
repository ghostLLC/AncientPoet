package com.ancientpoet.android.ui.screen.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class CommunityViewModel(private val client: HttpClient) : ViewModel() {
    private val _state = MutableStateFlow(CommunityState())
    val state: StateFlow<CommunityState> = _state
    private val base = "http://10.0.2.2:8080/api/v1"

    fun loadPosts() {
        viewModelScope.launch {
            try {
                val res: PostListRes = client.get("$base/community/posts").body()
                _state.value = _state.value.copy(posts = res.posts.map { PostItem(it.id, it.userId, it.userNickname, it.contentText, it.sharedMessageIds, it.likeCount, it.commentCount, it.isLiked) })
            } catch (_: Exception) {}
        }
    }

    fun toggleLike(postId: Long) {
        viewModelScope.launch {
            try {
                client.post("$base/community/posts/$postId/like")
                loadPosts()
            } catch (_: Exception) {}
        }
    }

    fun loadComments(postId: Long) {
        viewModelScope.launch {
            try {
                val comments: List<CommentRes> = client.get("$base/community/posts/$postId/comments").body()
                _state.value = _state.value.copy(comments = comments.map { CommentItem(it.id, it.userId, it.userNickname, it.content) })
            } catch (_: Exception) {}
        }
    }

    fun addComment(postId: Long, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                client.post("$base/community/posts/$postId/comments") { contentType(ContentType.Application.Json); setBody(CommentReq(content)) }
                loadComments(postId)
            } catch (_: Exception) {}
        }
    }
    fun loadProfile(userId: Long) {
        viewModelScope.launch {
            try {
                val p: ProfileRes = client.get("$base/user/profile/$userId").body()
                _state.value = _state.value.copy(profileName = p.nickname, profileBio = p.bio, postCount = p.postCount)
            } catch (_: Exception) {}
        }
    }
}

data class CommunityState(
    val posts: List<PostItem> = emptyList(),
    val comments: List<CommentItem> = emptyList(),
    val profileName: String? = null,
    val profileBio: String? = null,
    val postCount: Int = 0,
)
data class PostItem(val id: Long, val userId: Long, val userNickname: String?, val contentText: String?, val sharedMessageIds: List<Long>?, val likeCount: Int, val commentCount: Int, val isLiked: Boolean)
data class CommentItem(val id: Long, val userId: Long, val userNickname: String?, val content: String)

@Serializable data class PostListRes(val posts: List<PostRes>)
@Serializable data class PostRes(val id: Long, val userId: Long, val userNickname: String?, val contentText: String?, val sharedMessageIds: List<Long>? = null, val likeCount: Int, val commentCount: Int, val isLiked: Boolean = false)
@Serializable data class CommentRes(val id: Long, val userId: Long, val userNickname: String?, val content: String)
@Serializable data class CommentReq(val content: String)
@Serializable data class ProfileRes(val id: Long, val nickname: String?, val bio: String?, val avatarUrl: String?, val postCount: Int)
