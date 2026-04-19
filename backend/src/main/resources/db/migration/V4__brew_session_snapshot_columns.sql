ALTER TABLE brew_sessions
    ADD COLUMN scaled_ingredients_json  JSONB,
    ADD COLUMN scaled_steps_json        JSONB,
    ADD COLUMN boil_off_rate_percent    DECIMAL(5,2) NOT NULL DEFAULT 10,
    ADD COLUMN water_to_grain_ratio     DECIMAL(4,2) NOT NULL DEFAULT 3.0,
    ADD COLUMN strike_water_l           DECIMAL(8,2),
    ADD COLUMN sparge_volume_l          DECIMAL(8,2),
    ADD COLUMN pre_boil_volume_l        DECIMAL(8,2);
