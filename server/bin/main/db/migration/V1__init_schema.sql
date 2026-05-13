-- ============================================
-- AncientPoet V1: Initial Schema
-- Phase 1 tables: dynasties, dynasty_cities,
--   poets, poet_movements, poems,
--   users, user_locations,
--   conversations, messages, conversation_summaries
-- ============================================

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- ========== 基础数据 ==========

-- 朝代表
CREATE TABLE dynasties (
    id          VARCHAR(20) PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    start_year  INT NOT NULL,
    end_year    INT NOT NULL,
    map_url     TEXT,
    description TEXT
);

-- 朝代城市坐标
CREATE TABLE dynasty_cities (
    id           BIGSERIAL PRIMARY KEY,
    dynasty_id   VARCHAR(20) NOT NULL REFERENCES dynasties(id),
    name         VARCHAR(100) NOT NULL,
    modern_name  VARCHAR(100),
    province     VARCHAR(100),
    lat          DOUBLE PRECISION NOT NULL,
    lng          DOUBLE PRECISION NOT NULL,
    is_capital   BOOLEAN DEFAULT false,
    geom         GEOMETRY(Point, 4326),
    UNIQUE(dynasty_id, name)
);
CREATE INDEX idx_cities_dynasty ON dynasty_cities(dynasty_id);
CREATE INDEX idx_cities_geom ON dynasty_cities USING GIST(geom);

-- ========== 诗人数据 ==========

-- 诗人表
CREATE TABLE poets (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(50) NOT NULL,
    courtesy_name       VARCHAR(50),
    art_name            VARCHAR(50),
    dynasty_id          VARCHAR(20) NOT NULL REFERENCES dynasties(id),
    birth_year          INT NOT NULL,
    death_year          INT NOT NULL,
    personality_profile JSONB NOT NULL,
    writing_style       TEXT NOT NULL,
    system_prompt       TEXT NOT NULL,
    biography_summary   TEXT,
    portrait_url        TEXT,
    is_free             BOOLEAN DEFAULT true,
    created_at          TIMESTAMPTZ DEFAULT now()
);

-- 诗人历史轨迹
CREATE TABLE poet_movements (
    id                BIGSERIAL PRIMARY KEY,
    poet_id           BIGINT NOT NULL REFERENCES poets(id),
    year_start        INT NOT NULL,
    year_end          INT NOT NULL,
    location_name     VARCHAR(100) NOT NULL,
    lat               DOUBLE PRECISION NOT NULL,
    lng               DOUBLE PRECISION NOT NULL,
    event_description TEXT,
    event_type        VARCHAR(20) DEFAULT 'normal',
    geom              GEOMETRY(Point, 4326),
    CONSTRAINT chk_movement_years CHECK (year_end >= year_start)
);
CREATE INDEX idx_movements_poet ON poet_movements(poet_id, year_start);

-- 诗人生平事件
CREATE TABLE poet_life_events (
    id               BIGSERIAL PRIMARY KEY,
    poet_id          BIGINT NOT NULL REFERENCES poets(id),
    year             INT NOT NULL,
    age              INT NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      TEXT NOT NULL,
    location_name    VARCHAR(100),
    event_type       VARCHAR(20) DEFAULT 'milestone',
    delay_multiplier REAL DEFAULT 1.0,
    sort_order       INT DEFAULT 0
);
CREATE INDEX idx_life_events_poet ON poet_life_events(poet_id, year);

-- 诗词库
CREATE TABLE poems (
    id            BIGSERIAL PRIMARY KEY,
    poet_id       BIGINT NOT NULL REFERENCES poets(id),
    title         VARCHAR(200) NOT NULL,
    content       TEXT NOT NULL,
    year_written  INT,
    context       TEXT,
    translation   TEXT,
    appreciation  TEXT,
    tags          JSONB DEFAULT '[]'::jsonb,
    created_at    TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_poems_poet ON poems(poet_id);

-- ========== 用户数据 ==========

-- 用户表
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    phone       VARCHAR(20) UNIQUE NOT NULL,
    nickname    VARCHAR(50),
    avatar_url  TEXT,
    bio         TEXT,
    created_at  TIMESTAMPTZ DEFAULT now(),
    updated_at  TIMESTAMPTZ DEFAULT now()
);

-- 用户位置表
CREATE TABLE user_locations (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    dynasty_id          VARCHAR(20) NOT NULL REFERENCES dynasties(id),
    location_name       VARCHAR(100) NOT NULL,
    lat                 DOUBLE PRECISION NOT NULL,
    lng                 DOUBLE PRECISION NOT NULL,
    status              VARCHAR(10) DEFAULT 'settled',
    moving_to_name      VARCHAR(100),
    moving_to_lat       DOUBLE PRECISION,
    moving_to_lng       DOUBLE PRECISION,
    moving_start_time   TIMESTAMPTZ,
    moving_arrival_time TIMESTAMPTZ,
    geom                GEOMETRY(Point, 4326),
    updated_at          TIMESTAMPTZ DEFAULT now(),
    UNIQUE(user_id, dynasty_id)
);

-- ========== 会话与消息 ==========

-- 会话表
CREATE TABLE conversations (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id),
    poet_id                 BIGINT NOT NULL REFERENCES poets(id),
    mode                    VARCHAR(10) NOT NULL DEFAULT 'open',
    dynasty_id              VARCHAR(20) NOT NULL REFERENCES dynasties(id),
    storyline_current_year  INT,
    storyline_completed     BOOLEAN DEFAULT false,
    background_setting      TEXT,
    created_at              TIMESTAMPTZ DEFAULT now(),
    updated_at              TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_conv_user ON conversations(user_id);

-- 消息表
CREATE TABLE messages (
    id                     BIGSERIAL PRIMARY KEY,
    conversation_id        BIGINT NOT NULL REFERENCES conversations(id),
    sender_type            VARCHAR(5) NOT NULL,
    content_text           TEXT,
    content_image_url      TEXT,
    translation            TEXT,
    is_delivered           BOOLEAN DEFAULT false,
    scheduled_delivery_at  TIMESTAMPTZ,
    delivered_at           TIMESTAMPTZ,
    delay_seconds          INT,
    delay_factors          JSONB,
    created_at             TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_msg_conv ON messages(conversation_id, created_at);
CREATE INDEX idx_msg_delivery ON messages(is_delivered, scheduled_delivery_at)
    WHERE is_delivered = false;

-- 对话上下文摘要
CREATE TABLE conversation_summaries (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT NOT NULL REFERENCES conversations(id),
    summary_text     TEXT NOT NULL,
    covers_up_to     BIGINT NOT NULL,
    created_at       TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- Trigger: auto-sync geom from lat/lng
-- ============================================
CREATE OR REPLACE FUNCTION sync_geom_from_latlng()
RETURNS TRIGGER AS $$
BEGIN
    NEW.geom := ST_SetSRID(ST_MakePoint(NEW.lng, NEW.lat), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cities_geom
    BEFORE INSERT OR UPDATE OF lat, lng ON dynasty_cities
    FOR EACH ROW EXECUTE FUNCTION sync_geom_from_latlng();

CREATE TRIGGER trg_movements_geom
    BEFORE INSERT OR UPDATE OF lat, lng ON poet_movements
    FOR EACH ROW EXECUTE FUNCTION sync_geom_from_latlng();

CREATE TRIGGER trg_user_locations_geom
    BEFORE INSERT OR UPDATE OF lat, lng ON user_locations
    FOR EACH ROW EXECUTE FUNCTION sync_geom_from_latlng();
