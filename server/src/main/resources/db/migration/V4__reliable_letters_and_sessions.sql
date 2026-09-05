-- Forward-only repair: retain existing letters and make their lifecycle explicit.
ALTER TABLE messages ADD COLUMN client_message_id VARCHAR(80);
ALTER TABLE messages ADD COLUMN reply_to_message_id BIGINT REFERENCES messages(id) ON DELETE CASCADE;
ALTER TABLE messages ADD COLUMN read_at TIMESTAMPTZ;
CREATE UNIQUE INDEX uq_message_submission ON messages(conversation_id, client_message_id) WHERE client_message_id IS NOT NULL;
CREATE UNIQUE INDEX uq_message_reply ON messages(reply_to_message_id) WHERE reply_to_message_id IS NOT NULL;
CREATE INDEX idx_messages_cursor ON messages(conversation_id, id DESC);
UPDATE messages SET is_delivered=true, delivered_at=COALESCE(delivered_at, created_at, now())
WHERE sender_type='user' AND is_delivered IS NOT TRUE;

ALTER TABLE messages DROP CONSTRAINT messages_conversation_id_fkey;
ALTER TABLE messages ADD CONSTRAINT messages_conversation_id_fkey FOREIGN KEY(conversation_id) REFERENCES conversations(id) ON DELETE CASCADE;
ALTER TABLE conversation_summaries DROP CONSTRAINT conversation_summaries_conversation_id_fkey;
ALTER TABLE conversation_summaries ADD CONSTRAINT conversation_summaries_conversation_id_fkey FOREIGN KEY(conversation_id) REFERENCES conversations(id) ON DELETE CASCADE;
ALTER TABLE conversations DROP CONSTRAINT conversations_user_id_fkey;
ALTER TABLE conversations ADD CONSTRAINT conversations_user_id_fkey FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_locations DROP CONSTRAINT user_locations_user_id_fkey;
ALTER TABLE user_locations ADD CONSTRAINT user_locations_user_id_fkey FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE conversations ADD COLUMN archived BOOLEAN NOT NULL DEFAULT false;
UPDATE conversations c SET storyline_current_year=LEAST(p.birth_year+42,p.death_year)
FROM poets p WHERE c.poet_id=p.id AND (c.storyline_current_year IS NULL OR c.storyline_current_year NOT BETWEEN p.birth_year AND p.death_year);

CREATE TABLE message_jobs (
 id BIGSERIAL PRIMARY KEY,
 message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
 kind VARCHAR(16) NOT NULL CHECK(kind IN ('generate','translate','notify','summarize')),
 state VARCHAR(12) NOT NULL DEFAULT 'queued' CHECK(state IN ('queued','running','retrying','done','failed')),
 payload JSONB NOT NULL DEFAULT '{}',
 attempts INT NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 lease_until TIMESTAMPTZ,
 lease_token VARCHAR(36),
 last_error_code VARCHAR(50),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(message_id,kind)
);
CREATE INDEX idx_jobs_ready ON message_jobs(next_attempt_at,id) WHERE state IN ('queued','retrying','running');

CREATE TABLE auth_sessions (
 id VARCHAR(36) PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 refresh_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auth_sessions_user ON auth_sessions(user_id);
CREATE TABLE used_refresh_tokens (
 token_hash VARCHAR(64) PRIMARY KEY,
 session_id VARCHAR(36) NOT NULL REFERENCES auth_sessions(id) ON DELETE CASCADE,
 expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE sms_challenges (
 phone VARCHAR(20) PRIMARY KEY,
 code_hash VARCHAR(64) NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 sent_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 attempts INT NOT NULL DEFAULT 0,
 consumed BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE community_posts (
 id BIGSERIAL PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 content_text TEXT,
 content_image_urls TEXT,
 shared_message_ids TEXT,
 type VARCHAR(10) NOT NULL DEFAULT 'original',
 repost_of_id BIGINT REFERENCES community_posts(id) ON DELETE SET NULL,
 like_count INT NOT NULL DEFAULT 0 CHECK(like_count>=0),
 comment_count INT NOT NULL DEFAULT 0 CHECK(comment_count>=0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_posts_page ON community_posts(id DESC);
CREATE TABLE community_comments (
 id BIGSERIAL PRIMARY KEY,
 post_id BIGINT NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 content TEXT NOT NULL,
 reply_to_comment_id BIGINT REFERENCES community_comments(id) ON DELETE SET NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_post ON community_comments(post_id,id);
CREATE TABLE community_likes (
 post_id BIGINT NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY(post_id,user_id)
);
CREATE TABLE user_favorites (
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY(user_id,message_id)
);
CREATE TABLE media_assets (
 id VARCHAR(36) PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 object_key TEXT NOT NULL UNIQUE,
 content_type VARCHAR(40) NOT NULL,
 byte_count BIGINT NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
