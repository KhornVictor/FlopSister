-- V1: Initial database schema

-- 1. Sequences (allocationSize = 50)
CREATE SEQUENCE IF NOT EXISTS customer_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS orders_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS order_line_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS product_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS tag_seq START WITH 1 INCREMENT BY 50;

-- 2. Customer
CREATE TABLE IF NOT EXISTS customer (
    id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    street VARCHAR(255),
    city VARCHAR(255),
    zip VARCHAR(255),
    ship_street VARCHAR(255),
    ship_city VARCHAR(255),
    ship_zip VARCHAR(255),
    CONSTRAINT pk_customer PRIMARY KEY (id),
    CONSTRAINT uk_customer_email UNIQUE (email)
);

-- 3. Product
CREATE TABLE IF NOT EXISTS product (
    id BIGINT NOT NULL,
    sku VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    price_amount DECIMAL(12,2) NOT NULL,
    price_currency CHAR(3) NOT NULL,
    stock INTEGER NOT NULL CHECK (stock >= 0),
    category VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_product PRIMARY KEY (id),
    CONSTRAINT uk_product_sku UNIQUE (sku)
);

-- 4. Tag
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id),
    CONSTRAINT uk_tag_name UNIQUE (name)
);

-- 5. Orders
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
);

-- 6. Order lines
CREATE TABLE IF NOT EXISTS order_lines (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_amount DECIMAL(12,2) NOT NULL,
    unit_price_currency CHAR(3) NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_lines_product FOREIGN KEY (product_id) REFERENCES product(id)
);

-- 7. Product-tag join table
CREATE TABLE IF NOT EXISTS product_tag (
    product_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    CONSTRAINT pk_product_tag PRIMARY KEY (product_id, tag_id),
    CONSTRAINT fk_product_tag_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_product_tag_tag FOREIGN KEY (tag_id) REFERENCES tag(id)
);
