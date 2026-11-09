CREATE INDEX IF NOT EXISTS idx_orders_customer_placed ON orders (customer_id, placed_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_line_order ON order_lines (order_id);
CREATE INDEX IF NOT EXISTS idx_product_name_stock ON products (name, stock);
