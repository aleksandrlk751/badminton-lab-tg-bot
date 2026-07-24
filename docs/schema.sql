-- Черновик схемы PostgreSQL для badminton-lab-tg-bot
-- Управление: Flyway (Spring Boot). ORM: Spring Data JPA.
-- PK сущностей Player/Tournament = ID с badminton4u.ru
-- Синхронизировать с core/src/main/resources/db/migration/ (V1 + V2 player.sex + …)

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- Мета слепка
-- ---------------------------------------------------------------------------

CREATE TABLE snapshot_meta (
    region_code     VARCHAR(16) PRIMARY KEY,  -- например 'r77'
    last_sync_at    TIMESTAMPTZ NOT NULL,
    tournaments_from DATE,
    tournaments_to   DATE
);

-- ---------------------------------------------------------------------------
-- Игроки
-- ---------------------------------------------------------------------------

CREATE TABLE player (
    id              BIGINT PRIMARY KEY,       -- badminton4u player ID
    nick            VARCHAR(128) NOT NULL,
    first_name      VARCHAR(128),
    last_name       VARCHAR(128),
    patronymic      VARCHAR(128),
    city            VARCHAR(128),
    birth_date      DATE,
    playing_hand    VARCHAR(16),              -- 'RIGHT', 'LEFT'
    sex             VARCHAR(1),               -- 'M' / 'F' (Flyway V2: enum player_sex)
    hall_id         BIGINT,
    registered_at   DATE,
    last_seen_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_player_nick_trgm ON player USING gin (nick gin_trgm_ops);
CREATE INDEX idx_player_last_name_trgm ON player USING gin (last_name gin_trgm_ops);
CREATE INDEX idx_player_first_name_trgm ON player USING gin (first_name gin_trgm_ops);

-- ---------------------------------------------------------------------------
-- Рейтинги
-- ---------------------------------------------------------------------------

CREATE TYPE discipline AS ENUM ('S', 'D', 'MS', 'WS', 'MD', 'WD', 'XD');

CREATE TABLE player_rating (
    player_id       BIGINT NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    discipline      discipline NOT NULL,
    rating          NUMERIC(6,1) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, discipline)
);

CREATE TABLE player_rating_history (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    discipline      discipline NOT NULL,
    recorded_at     DATE NOT NULL,
    rating          NUMERIC(6,1) NOT NULL,
    UNIQUE (player_id, discipline, recorded_at)
);

CREATE INDEX idx_rating_history_player ON player_rating_history (player_id, discipline, recorded_at);

-- ---------------------------------------------------------------------------
-- Турниры
-- ---------------------------------------------------------------------------

CREATE TYPE tournament_status AS ENUM ('UPCOMING', 'COMPLETED', 'CANCELLED');

CREATE TABLE tournament (
    id              BIGINT PRIMARY KEY,       -- badminton4u tournament ID
    name            VARCHAR(256) NOT NULL,
    category_code   VARCHAR(16),              -- SE, DF, XDC …
    rating_limit    NUMERIC(6,1),             -- из <var>; NULL = «отк» / без лимита
    max_player_rating_limit NUMERIC(6,1),     -- макс. игрока в паре; NULL = без лимита
    avg_rating      NUMERIC(6,1),
    coefficient     NUMERIC(4,2),
    hall_id         BIGINT,
    city            VARCHAR(128),
    starts_at       TIMESTAMPTZ NOT NULL,
    status          tournament_status NOT NULL DEFAULT 'UPCOMING',
    region_code     VARCHAR(16) NOT NULL DEFAULT 'r77',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tournament_starts ON tournament (region_code, starts_at);
CREATE INDEX idx_tournament_status ON tournament (status, starts_at);

-- ---------------------------------------------------------------------------
-- Пары (для парных турниров и S_partner)
-- ---------------------------------------------------------------------------

CREATE TABLE pair (
    id              BIGSERIAL PRIMARY KEY,
    player1_id      BIGINT NOT NULL REFERENCES player(id),
    player2_id      BIGINT NOT NULL REFERENCES player(id),
    discipline      discipline NOT NULL,
    CHECK (player1_id < player2_id),
    UNIQUE (player1_id, player2_id, discipline)
);

-- ---------------------------------------------------------------------------
-- Участие в турнире
-- ---------------------------------------------------------------------------

CREATE TABLE participation (
    id              BIGSERIAL PRIMARY KEY,
    tournament_id   BIGINT NOT NULL REFERENCES tournament(id) ON DELETE CASCADE,
    player_id       BIGINT NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    pair_id         BIGINT REFERENCES pair(id),
    place           SMALLINT,
    rating_before   NUMERIC(6,1),
    rating_delta    NUMERIC(6,1),
    rating_after    NUMERIC(6,1),
    matches_won     SMALLINT,
    matches_lost    SMALLINT,
    sets_won        SMALLINT,
    sets_lost       SMALLINT,
    UNIQUE (tournament_id, player_id)
);

CREATE INDEX idx_participation_player ON participation (player_id);
CREATE INDEX idx_participation_tournament ON participation (tournament_id);

-- ---------------------------------------------------------------------------
-- Матчи (1v1 и 2v2 через match_player)
-- ---------------------------------------------------------------------------

CREATE TABLE match (
    id              BIGSERIAL PRIMARY KEY,
    tournament_id   BIGINT NOT NULL REFERENCES tournament(id) ON DELETE CASCADE,
    discipline      discipline NOT NULL,
    played_at       TIMESTAMPTZ NOT NULL,
    stage           VARCHAR(64),              -- «за 3-е», «1/4» …
    score_sets      VARCHAR(16) NOT NULL,       -- «2:0»
    duration_min    SMALLINT,
    source          VARCHAR(32) NOT NULL DEFAULT 'badminton4u',
    external_key    VARCHAR(128),             -- для идемпотентности парсинга
    UNIQUE (source, external_key)
);

CREATE TYPE match_side AS ENUM ('A', 'B');

CREATE TABLE match_player (
    match_id        BIGINT NOT NULL REFERENCES match(id) ON DELETE CASCADE,
    player_id       BIGINT NOT NULL REFERENCES player(id),
    side            match_side NOT NULL,
    rating_before   NUMERIC(6,1),
    rating_delta    NUMERIC(6,1),
    PRIMARY KEY (match_id, player_id)
);

CREATE INDEX idx_match_player_player ON match_player (player_id);
CREATE INDEX idx_match_tournament ON match (tournament_id, played_at);

-- ---------------------------------------------------------------------------
-- Сводка соперников (агрегат player↔opponent из Match; вариант C — только W/L)
-- H2H, Form, delta — из match / match_player, не из этой таблицы
-- ---------------------------------------------------------------------------

CREATE TABLE rival_summary (
    player_id       BIGINT NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    opponent_id     BIGINT NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    discipline      discipline NOT NULL,
    wins            SMALLINT NOT NULL DEFAULT 0,
    losses          SMALLINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, opponent_id, discipline),
    CHECK (player_id <> opponent_id)
);

CREATE INDEX idx_rival_summary_opponent ON rival_summary (opponent_id);

-- ---------------------------------------------------------------------------
-- Регистрация на будущий турнир (для подбора партнёра)
-- ---------------------------------------------------------------------------

CREATE TABLE tournament_registration (
    tournament_id   BIGINT NOT NULL REFERENCES tournament(id) ON DELETE CASCADE,
    player_id       BIGINT NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    pair_id         BIGINT REFERENCES pair(id),  -- NULL = ищет партнёра / одиночная заявка
    registered_at   TIMESTAMPTZ,
    PRIMARY KEY (tournament_id, player_id)
);

CREATE INDEX idx_registration_tournament_unpaired
    ON tournament_registration (tournament_id)
    WHERE pair_id IS NULL;
