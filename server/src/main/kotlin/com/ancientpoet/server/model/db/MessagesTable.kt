package com.ancientpoet.server.model.db

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamptz
import org.jetbrains.exposed.sql.json.jsonb

object MessagesTable : Table("messages") {
    val id = long("id").autoIncrement()
    val conversationId = long("conversation_id").references(ConversationsTable.id)
    val senderType = varchar("sender_type", 5)
    val contentText = text("content_text").nullable()
    val contentImageUrl = text("content_image_url").nullable()
    val translation = text("translation").nullable()
    val isDelivered = bool("is_delivered").default(false)
    val scheduledDeliveryAt = timestamptz("scheduled_delivery_at").nullable()
    val deliveredAt = timestamptz("delivered_at").nullable()
    val delaySeconds = integer("delay_seconds").nullable()
    val delayFactors = jsonb("delay_factors", MapSerializer(String.serializer(), String.serializer())).nullable()
    val createdAt = timestamptz("created_at")
    override val primaryKey = PrimaryKey(id)
}

object ConversationSummariesTable : Table("conversation_summaries") {
    val id = long("id").autoIncrement()
    val conversationId = long("conversation_id").references(ConversationsTable.id)
    val summaryText = text("summary_text")
    val coversUpTo = long("covers_up_to")
    val createdAt = timestamptz("created_at")
    override val primaryKey = PrimaryKey(id)
}
