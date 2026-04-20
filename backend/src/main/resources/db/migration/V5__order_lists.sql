CREATE TABLE IF NOT EXISTS order_lists (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id             UUID NOT NULL REFERENCES recipes(id),
    volume_l              DECIMAL(6,2) NOT NULL,
    ai_result             JSONB,
    estimated_total_min   DECIMAL(10,2),
    estimated_total_max   DECIMAL(10,2),
    created_by            UUID NOT NULL REFERENCES users(id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_lists_created_by ON order_lists(created_by);
