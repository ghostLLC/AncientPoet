package com.ancientpoet.server.repository

import com.ancientpoet.server.model.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CommunityRepository {
    suspend fun createPost(userId: Long, contentText: String?, contentImageUrls: List<String>?, sharedMessageIds: List<Long>?, type: String = "original", repostOfId: Long? = null): Long = withContext(Dispatchers.IO) {
        transaction {
            val result = CommunityPostsTable.insert {
                it[CommunityPostsTable.userId] = userId
                it[CommunityPostsTable.contentText] = contentText
                it[CommunityPostsTable.type] = type
                if (repostOfId != null) it[CommunityPostsTable.repostOfId] = repostOfId
            }
            result[CommunityPostsTable.id]
        }
    }

    suspend fun getPosts(page: Int = 0, pageSize: Int = 20): List<PostRow> = withContext(Dispatchers.IO) {
        transaction {
            CommunityPostsTable.join(
                UsersTable,
                JoinType.INNER,
                additionalConstraint = { CommunityPostsTable.userId eq UsersTable.id },
            ).selectAll()
                .orderBy(CommunityPostsTable.createdAt, SortOrder.DESC)
                .limit(pageSize).offset((page * pageSize).toLong())
                .map { PostRow(it[CommunityPostsTable.id], it[CommunityPostsTable.userId], it[UsersTable.nickname] ?: "", it[CommunityPostsTable.contentText], it[CommunityPostsTable.type], it[CommunityPostsTable.likeCount], it[CommunityPostsTable.commentCount]) }
        }
    }

    suspend fun isLiked(postId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        transaction { CommunityLikesTable.selectAll().where { (CommunityLikesTable.postId eq postId) and (CommunityLikesTable.userId eq userId) }.count() > 0 }
    }

    suspend fun toggleLike(postId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        transaction {
            val existing = CommunityLikesTable.selectAll().where { (CommunityLikesTable.postId eq postId) and (CommunityLikesTable.userId eq userId) }.singleOrNull()
            if (existing != null) {
                CommunityLikesTable.deleteWhere { (CommunityLikesTable.postId eq postId) and (CommunityLikesTable.userId eq userId) }
                CommunityPostsTable.update({ CommunityPostsTable.id eq postId }) { with(SqlExpressionBuilder) { it[likeCount] = likeCount - 1 } }
                false
            } else {
                CommunityLikesTable.insert { it[CommunityLikesTable.postId] = postId; it[CommunityLikesTable.userId] = userId }
                CommunityPostsTable.update({ CommunityPostsTable.id eq postId }) { with(SqlExpressionBuilder) { it[likeCount] = likeCount + 1 } }
                true
            }
        }
    }

    suspend fun getComments(postId: Long): List<CommentRow> = withContext(Dispatchers.IO) {
        transaction {
            CommunityCommentsTable.join(
                UsersTable,
                JoinType.INNER,
                additionalConstraint = { CommunityCommentsTable.userId eq UsersTable.id },
            ).selectAll()
                .where { CommunityCommentsTable.postId eq postId }
                .orderBy(CommunityCommentsTable.createdAt)
                .map { CommentRow(it[CommunityCommentsTable.id], it[CommunityCommentsTable.userId], it[UsersTable.nickname] ?: "", it[CommunityCommentsTable.content], it[CommunityCommentsTable.replyToCommentId]) }
        }
    }

    suspend fun createComment(postId: Long, userId: Long, content: String, replyToId: Long? = null): Long = withContext(Dispatchers.IO) {
        transaction {
            val result = CommunityCommentsTable.insert {
                it[CommunityCommentsTable.postId] = postId
                it[CommunityCommentsTable.userId] = userId
                it[CommunityCommentsTable.content] = content
                if (replyToId != null) it[CommunityCommentsTable.replyToCommentId] = replyToId
            }
            CommunityPostsTable.update({ CommunityPostsTable.id eq postId }) { with(SqlExpressionBuilder) { it[commentCount] = commentCount + 1 } }
            result[CommunityCommentsTable.id]
        }
    }

    suspend fun toggleFavorite(userId: Long, messageId: Long): Boolean = withContext(Dispatchers.IO) {
        transaction {
            val existing = UserFavoritesTable.selectAll().where { (UserFavoritesTable.userId eq userId) and (UserFavoritesTable.messageId eq messageId) }.singleOrNull()
            if (existing != null) {
                UserFavoritesTable.deleteWhere { (UserFavoritesTable.userId eq userId) and (UserFavoritesTable.messageId eq messageId) }
                false
            } else {
                UserFavoritesTable.insert { it[UserFavoritesTable.userId] = userId; it[UserFavoritesTable.messageId] = messageId }
                true
            }
        }
    }

    suspend fun getFavorites(userId: Long): List<Long> = withContext(Dispatchers.IO) {
        transaction { UserFavoritesTable.selectAll().where { UserFavoritesTable.userId eq userId }.orderBy(UserFavoritesTable.createdAt, SortOrder.DESC).map { it[UserFavoritesTable.messageId] } }
    }

    suspend fun getPostCount(userId: Long): Int = withContext(Dispatchers.IO) {
        transaction { CommunityPostsTable.selectAll().where { CommunityPostsTable.userId eq userId }.count().toInt() }
    }
}

data class PostRow(val id: Long, val userId: Long, val userNickname: String, val contentText: String?, val type: String, val likeCount: Int, val commentCount: Int)
data class CommentRow(val id: Long, val userId: Long, val userNickname: String, val content: String, val replyToCommentId: Long?)
