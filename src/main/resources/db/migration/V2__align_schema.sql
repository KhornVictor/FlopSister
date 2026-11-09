-- 1. Rename tables to match the Java entities
ALTER TABLE customers RENAME TO customer;
ALTER TABLE products RENAME TO product;
ALTER TABLE tags RENAME TO tag;
ALTER TABLE product_tags RENAME TO product_tag;
ALTER TABLE order_lines RENAME TO order_line;

-- 2. Rename the order sequence
ALTER SEQUENCE order_seq RENAME TO orders_seq;

-- 3. Rename customer shipping columns
ALTER TABLE customer RENAME COLUMN shipping_street TO ship_street;
ALTER TABLE customer RENAME COLUMN shipping_city TO ship_city;
ALTER TABLE customer RENAME COLUMN shipping_state TO ship_state;
ALTER TABLE customer RENAME COLUMN shipping_zip_code TO ship_zip_code;
ALTER TABLE customer RENAME COLUMN shipping_country TO ship_country;

-- 4. Align product columns
ALTER TABLE product
    ADD COLUMN sku VARCHAR(20);

UPDATE product
SET sku = 'S' || id::TEXT;

ALTER TABLE product
    ALTER COLUMN sku SET NOT NULL;

ALTER TABLE product
    ADD CONSTRAINT uk_product_sku UNIQUE (sku);

ALTER TABLE product
    ALTER COLUMN name TYPE VARCHAR(120);

ALTER TABLE product
    ALTER COLUMN category TYPE VARCHAR(20);

ALTER TABLE product
    ADD COLUMN stock INTEGER NOT NULL DEFAULT 0;

ALTER TABLE product
    ADD CONSTRAINT chk_product_stock CHECK (stock >= 0);

ALTER TABLE product
    ALTER COLUMN price_currency TYPE CHAR(3)
    USING price_currency::CHAR(3);

-- 5. Align order columns
ALTER TABLE orders
    ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE orders
    ALTER COLUMN placed_at TYPE TIMESTAMP WITH TIME ZONE
    USING placed_at AT TIME ZONE 'UTC';

ALTER TABLE orders
    ALTER COLUMN version TYPE BIGINT
    USING version::BIGINT;

-- 6. Align order line currency
ALTER TABLE order_line
    ALTER COLUMN unit_price_currency TYPE CHAR(3)
    USING unit_price_currency::CHAR(3);