package com.ancientpoet.server.repository

import com.ancientpoet.server.model.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class CommunityRepository {
    suspend fun createPost(userId: Long, contentText: String?, contentImageUrls: List<String>?, sharedMessageIds: List<Long>?, type: String = "original", repostOfId: Long? = null): Long = withContext(Dispatchers.IO) {
        transaction {
            CommunityPostsTable.insertAndGetId {
                it[CommunityPostsTable.userId] = userId
                it[CommunityPostsTable.contentText] = contentText
                it[CommunityPostsTable.type] = type
                it[CommunityPostsTable.repostOfId] = repostOfId
            }.value
        }
    }

    suspend fun getPosts(page: Int = 0, pageSize: Int = 20): List<Triple<Long, Long, String>> = withContext(Dispatchers.IO) {
        transaction {
            CommunityPostsTable
                .innerJoin(UsersTable)
                .selectAll()
                .orderBy(CommunityPostsTable.createdAt, SortOrder.DESC)
                .limit(pageSize).offset((page * pageSize).toLong())
                .map { Triple(it[CommunityPostsTable.id].value, it[CommunityPostsTable.userId].value, it[UsersTable.nickname] ?: "") }
        }
    }

    suspend fun getPostById(postId: Long): ResultRow? = withContext(Dispatchers.IO) {
        transaction {
            CommunityPostsTable.select { CommunityPostsTable.id eq postId }.singleOrNull()
        }
    }

    suspend fun isLiked(postId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        transaction {
            CommunityLikesTable.select { (CommunityLikesTable.postId eq postId) and (CommunityLikesTable.userId eq userId) }.count() > 0
        }
    }

    suspend fun toggleLike(postId: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        transaction {
            val existing = CommunityLikesTable.select { (CommunityLikesTable.postId eq postId) and (CommunityLikesTable.userId eq userId) }.singleOrNull()
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

    suspend fun getComments(postId: Long): List<Triple<Long, Long, String>> = withContext(Dispatchers.IO) {
        transaction {
            CommunityCommentsTable
                .innerJoin(UsersTable)
                .select { CommunityCommentsTable.postId eq postId }
                .orderBy(CommunityCommentsTable.createdAt)
                .map { Triple(it[CommunityCommentsTable.id].value, it[CommunityCommentsTable.userId].value, it[UsersTable.nickname] ?: "") }
        }
    }

    suspend fun createComment(postId: Long, userId: Long, content: String, replyToId: Long? = null): Long = withContext(Dispatchers.IO) {
        transaction {
            val id = CommunityCommentsTable.insertAndGetId {
                it[CommunityCommentsTable.postId] = postId
                it[CommunityCommentsTable.userId] = userId
                it[CommunityCommentsTable.content] = content
                it[CommunityCommentsTable.replyToCommentId] = replyToId
            }
            CommunityPostsTable.update({ CommunityPostsTable.id eq postId }) { with(SqlExpressionBuilder) { it[commentCount] = commentCount + 1 } }
            id.value
        }
    }

    // Favorites
    suspend fun toggleFavorite(userId: Long, messageId: Long): Boolean = withContext(Dispatchers.IO) {
        transaction {
            val existing = UserFavoritesTable.select { (UserFavoritesTable.userId eq userId) and (UserFavoritesTable.messageId eq messageId) }.singleOrNull()
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
        transaction {
            UserFavoritesTable.select { UserFavoritesTable.userId eq userId }
                .orderBy(UserFavoritesTable.createdAt, SortOrder.DESC)
                .map { it[UserFavoritesTable.messageId].value }
        }
    }

    suspend fun getPostCount(userId: Long): Int = withContext(Dispatchers.IO) {
        transaction {
            CommunityPostsTable.select { CommunityPostsTable.userId eq userId }.count().toInt()
        }
    }
}
