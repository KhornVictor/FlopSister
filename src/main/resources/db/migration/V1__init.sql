
-- ==========================================
-- 1. SEQUENCES
-- ==========================================

CREATE SEQUENCE customer_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE product_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE tag_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE order_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE order_line_seq START WITH 1 INCREMENT BY 50;


-- ==========================================
-- 2. CUSTOMER TABLE
-- ==========================================

CREATE TABLE customer (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,

    -- Billing address
    street VARCHAR(255),
    city VARCHAR(255),
    zip VARCHAR(255),

    -- Shipping address
    ship_street VARCHAR(255),
    ship_city VARCHAR(255),
    ship_zip VARCHAR(255)
);


-- ==========================================
-- 3. PRODUCT TABLE
-- ==========================================

CREATE TABLE product (
    id BIGINT PRIMARY KEY,
    sku VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    price_amount DECIMAL(12, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    category VARCHAR(20) NOT NULL,
    stock INT NOT NULL,

    CONSTRAINT chk_product_stock CHECK (stock >= 0)
);


-- ==========================================
-- 4. TAG TABLE
-- ==========================================

CREATE TABLE tag (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);


-- ==========================================
-- 5. PRODUCT_TAG JOIN TABLE
-- ==========================================

CREATE TABLE product_tag (
    product_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,

    PRIMARY KEY (product_id, tag_id),

    FOREIGN KEY (product_id)
        REFERENCES product(id),

    FOREIGN KEY (tag_id)
        REFERENCES tag(id)
);


-- ==========================================
-- 6. ORDERS TABLE
-- ==========================================

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,

    FOREIGN KEY (customer_id)
        REFERENCES customer(id)
);


-- ==========================================
-- 7. ORDER_LINE TABLE
-- ==========================================

CREATE TABLE order_line (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price_amount DECIMAL(12, 2) NOT NULL,
    unit_price_currency VARCHAR(3) NOT NULL,

    CONSTRAINT chk_order_line_quantity CHECK (quantity > 0),

    FOREIGN KEY (order_id)
        REFERENCES orders(id),

    FOREIGN KEY (product_id)
        REFERENCES product(id)
);
