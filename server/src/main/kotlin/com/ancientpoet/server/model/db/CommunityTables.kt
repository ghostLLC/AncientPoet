package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamptz
import org.jetbrains.exposed.sql.json.jsonb

object CommunityPostsTable : Table("community_posts") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(UsersTable.id)
    val contentText = text("content_text").nullable()
    val contentImageUrls = jsonb("content_image_urls", /* List<String> serializer */).nullable()
    val sharedMessageIds = jsonb("shared_message_ids", /* List<Long> serializer */).nullable()
    val type = varchar("type", 10).default("original")
    val repostOfId = long("repost_of_id").nullable().references(this.id)
    val likeCount = integer("like_count").default(0)
    val commentCount = integer("comment_count").default(0)
    val createdAt = timestamptz("created_at")
    override val primaryKey = PrimaryKey(id)
}

object CommunityCommentsTable : Table("community_comments") {
    val id = long("id").autoIncrement()
    val postId = long("post_id").references(CommunityPostsTable.id)
    val userId = long("user_id").references(UsersTable.id)
    val content = text("content")
    val replyToCommentId = long("reply_to_comment_id").nullable().references(this.id)
    val createdAt = timestamptz("created_at")
    override val primaryKey = PrimaryKey(id)
}

object CommunityLikesTable : Table("community_likes") {
    val postId = long("post_id").references(CommunityPostsTable.id)
    val userId = long("user_id").references(UsersTable.id)
    val createdAt = timestamptz("created_at")
    override val primaryKey = PrimaryKey(postId, userId)
}

object UserFavoritesTable : Table("user_favorites") {
    val userId = long("user_id").references(UsersTable.id)
    val messageId = long("message_id").references(MessagesTable.id)
    val createdAt = timestamptz("created_at")
    override val primaryKey = PrimaryKey(userId, messageId)
}
