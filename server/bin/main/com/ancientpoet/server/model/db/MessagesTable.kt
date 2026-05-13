package com.ancientpoet.server.model.db

import org.jetbrains.exposed.sql.Table


// FK constraints are enforced at DB level via Flyway V1 schema
object MessagesTable : Table("messages") {
    val id = long("id").autoIncrement()
    val conversationId = long("conversation_id")
    val senderType = varchar("sender_type", 5)
    val contentText = text("content_text").nullable()
    val contentImageUrl = text("content_image_url").nullable()
    val translation = text("translation").nullable()
    val isDelivered = bool("is_delivered").default(false)
    val scheduledDeliveryAt = text("scheduled_delivery_at").nullable()
    val deliveredAt = text("delivered_at").nullable()
    val delaySeconds = integer("delay_seconds").nullable()
    val delayFactors = text("delay_factors").nullable()
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(id)
}

object ConversationSummariesTable : Table("conversation_summaries") {
    val id = long("id").autoIncrement()
    val conversationId = long("conversation_id")
    val summaryText = text("summary_text")
    val coversUpTo = long("covers_up_to")
    val createdAt = text("created_at")
    override val primaryKey = PrimaryKey(id)
}
