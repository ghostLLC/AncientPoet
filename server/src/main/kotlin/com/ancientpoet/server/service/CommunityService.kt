package com.ancientpoet.server.service

import com.ancientpoet.server.model.dto.*
import com.ancientpoet.server.repository.*

class CommunityService(
    private val communityRepo: CommunityRepository,
    private val userRepo: UserRepository,
) {
    suspend fun createPost(userId: Long, request: CreatePostRequest): PostResponse {
        val postId = communityRepo.createPost(userId, request.contentText, request.contentImageUrls, request.sharedMessageIds)
        val user = userRepo.findById(userId)
        return PostResponse(id = postId, userId = userId, userNickname = user?.nickname, contentText = request.contentText, contentImageUrls = request.contentImageUrls, sharedMessageIds = request.sharedMessageIds, type = "original", likeCount = 0, commentCount = 0)
    }

    suspend fun getPosts(page: Int): PostListResponse {
        val rows = communityRepo.getPosts(page)
        val responses = rows.map { PostResponse(id = it.id, userId = it.userId, userNickname = it.userNickname.ifBlank { null }, contentText = it.contentText, type = it.type, likeCount = it.likeCount, commentCount = it.commentCount) }
        return PostListResponse(posts = responses, hasMore = rows.size >= 20)
    }

    suspend fun toggleLike(postId: Long, userId: Long): LikeResponse {
        val liked = communityRepo.toggleLike(postId, userId)
        return LikeResponse(liked = liked, likeCount = 0)
    }

    suspend fun getComments(postId: Long): List<CommentResponse> {
        return communityRepo.getComments(postId).map { CommentResponse(id = it.id, postId = postId, userId = it.userId, userNickname = it.userNickname.ifBlank { null }, content = it.content, replyToCommentId = it.replyToCommentId) }
    }

    suspend fun createComment(postId: Long, userId: Long, request: CreateCommentRequest): CommentResponse {
        val id = communityRepo.createComment(postId, userId, request.content, request.replyToCommentId)
        val user = userRepo.findById(userId)
        return CommentResponse(id = id, postId = postId, userId = userId, userNickname = user?.nickname, content = request.content, replyToCommentId = request.replyToCommentId)
    }

    suspend fun repost(userId: Long, postId: Long): PostResponse {
        val newId = communityRepo.createPost(userId, null, null, null, "repost", postId)
        val user = userRepo.findById(userId)
        return PostResponse(id = newId, userId = userId, userNickname = user?.nickname, contentText = null, type = "repost", repostOfId = postId, likeCount = 0, commentCount = 0)
    }

    suspend fun toggleFavorite(userId: Long, messageId: Long): FavoriteResponse {
        val fav = communityRepo.toggleFavorite(userId, messageId)
        return FavoriteResponse(messageId = messageId, favorited = fav)
    }

    suspend fun getFavorites(userId: Long): List<Long> = communityRepo.getFavorites(userId)

    suspend fun getProfile(userId: Long): ProfileResponse {
        val user = userRepo.findById(userId)
        val postCount = communityRepo.getPostCount(userId)
        return ProfileResponse(id = userId, nickname = user?.nickname, bio = user?.bio, avatarUrl = user?.avatarUrl, postCount = postCount)
    }
}
