-- V1: Initial schema

CREATE TABLE users (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE recipes (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(200) NOT NULL,
    style               VARCHAR(100),
    description         TEXT,
    source_url          TEXT,
    base_volume_l       DECIMAL(6,2) NOT NULL DEFAULT 20,
    original_gravity    DECIMAL(5,4),
    final_gravity       DECIMAL(5,4),
    abv                 DECIMAL(4,2),
    ibu                 INTEGER,
    srm                 DECIMAL(5,2),
    mash_temp_c         DECIMAL(4,1),
    mash_duration_min   INTEGER,
    boil_duration_min   INTEGER,
    fermentation_temp_c DECIMAL(4,1),
    fermentation_days   INTEGER,
    notes               TEXT,
    is_ai_generated     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by          UUID         REFERENCES users(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE recipe_ingredients (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id     UUID         NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    name          VARCHAR(200) NOT NULL,
    category      VARCHAR(50)  NOT NULL,
    amount        DECIMAL(10,3) NOT NULL,
    unit          VARCHAR(20)  NOT NULL,
    addition_time VARCHAR(100),
    notes         TEXT,
    sort_order    INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE recipe_steps (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id      UUID         NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    step_number    INTEGER      NOT NULL,
    phase          VARCHAR(50)  NOT NULL,
    title          VARCHAR(200) NOT NULL,
    instructions   TEXT         NOT NULL,
    duration_min   INTEGER,
    target_temp_c  DECIMAL(4,1),
    timer_required BOOLEAN      NOT NULL DEFAULT FALSE,
    notes          TEXT
);

CREATE TABLE brew_events (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    brew_date   DATE         NOT NULL,
    start_time  TIME,
    location    VARCHAR(200),
    recipe_id   UUID         REFERENCES recipes(id) ON DELETE SET NULL,
    created_by  UUID         NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE brew_event_participants (
    event_id UUID        REFERENCES brew_events(id) ON DELETE CASCADE,
    user_id  UUID        REFERENCES users(id) ON DELETE CASCADE,
    rsvp     VARCHAR(20) DEFAULT 'PENDING',
    PRIMARY KEY (event_id, user_id)
);

CREATE TABLE brew_sessions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id    UUID        REFERENCES recipes(id) ON DELETE SET NULL,
    event_id     UUID        REFERENCES brew_events(id) ON DELETE SET NULL,
    volume_l     DECIMAL(6,2) NOT NULL,
    current_step INTEGER     NOT NULL DEFAULT 0,
    status       VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    notes        TEXT,
    started_by   UUID        NOT NULL REFERENCES users(id),
    started_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE brew_session_step_log (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id    UUID        NOT NULL REFERENCES brew_sessions(id) ON DELETE CASCADE,
    step_number   INTEGER     NOT NULL,
    completed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actual_temp_c DECIMAL(4,1),
    notes         TEXT
);

CREATE TABLE order_lists (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id  UUID         NOT NULL REFERENCES recipes(id),
    volume_l   DECIMAL(6,2) NOT NULL,
    ai_result  JSONB,
    created_by UUID         NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
