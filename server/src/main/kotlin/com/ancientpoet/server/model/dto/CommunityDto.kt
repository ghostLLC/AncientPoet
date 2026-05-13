package com.ancientpoet.server.model.dto

import kotlinx.serialization.Serializable

@Serializable data class CreatePostRequest(val contentText: String? = null, val contentImageUrls: List<String>? = null, val sharedMessageIds: List<Long>? = null)

@Serializable data class PostResponse(
    val id: Long, val userId: Long, val userNickname: String?,
    val contentText: String?, val contentImageUrls: List<String>?,
    val sharedMessageIds: List<Long>?, val type: String,
    val repostOfId: Long?, val likeCount: Int, val commentCount: Int,
    val createdAt: String?, val isLiked: Boolean = false,
)

@Serializable data class PostListResponse(val posts: List<PostResponse>, val hasMore: Boolean = false)

@Serializable data class CreateCommentRequest(val content: String, val replyToCommentId: Long? = null)

@Serializable data class CommentResponse(
    val id: Long, val postId: Long, val userId: Long,
    val userNickname: String?, val content: String,
    val replyToCommentId: Long?, val createdAt: String?,
)

@Serializable data class LikeResponse(val liked: Boolean, val likeCount: Int)

@Serializable data class ShareImageRequest(val messageIds: List<Long>, val annotation: String? = null)
@Serializable data class ShareImageResponse(val imageUrl: String)

@Serializable data class FavoriteResponse(val messageId: Long, val favorited: Boolean)
@Serializable data class ProfileResponse(val id: Long, val nickname: String?, val bio: String?, val avatarUrl: String?, val postCount: Int)
