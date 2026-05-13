package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table

// FK constraints enforced at DB level via Flyway V1 schema
object CommunityPostsTable : Table("community_posts") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val contentText = text("content_text").nullable()
    val contentImageUrls = text("content_image_urls").nullable()
    val sharedMessageIds = text("shared_message_ids").nullable()
    val type = varchar("type", 10).default("original")
    val repostOfId = long("repost_of_id").nullable()
    val likeCount = integer("like_count").default(0)
    val commentCount = integer("comment_count").default(0)
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(id)
}

object CommunityCommentsTable : Table("community_comments") {
    val id = long("id").autoIncrement()
    val postId = long("post_id")
    val userId = long("user_id")
    val content = text("content")
    val replyToCommentId = long("reply_to_comment_id").nullable()
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(id)
}

object CommunityLikesTable : Table("community_likes") {
    val postId = long("post_id")
    val userId = long("user_id")
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(postId, userId)
}

object UserFavoritesTable : Table("user_favorites") {
    val userId = long("user_id")
    val messageId = long("message_id")
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(userId, messageId)
}
