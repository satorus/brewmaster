ALTER TABLE order_lists ADD COLUMN IF NOT EXISTS estimated_total_min DECIMAL(10,2);
ALTER TABLE order_lists ADD COLUMN IF NOT EXISTS estimated_total_max DECIMAL(10,2);
