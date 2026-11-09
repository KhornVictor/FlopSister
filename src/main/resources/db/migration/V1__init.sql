-- 5 Sequences with allocationSize = 50 for JDBC batching support
CREATE SEQUENCE customer_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE order_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE order_line_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE product_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE tag_seq START WITH 1 INCREMENT BY 50;

-- 1. Customers Table (with embedded shipping and billing addresses)
CREATE TABLE customers (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    shipping_street VARCHAR(255),
    shipping_city VARCHAR(255),
    shipping_state VARCHAR(255),
    shipping_zip_code VARCHAR(255),
    shipping_country VARCHAR(255),
    billing_street VARCHAR(255),
    billing_city VARCHAR(255),
    billing_state VARCHAR(255),
    billing_zip_code VARCHAR(255),
    billing_country VARCHAR(255)
);

-- 2. Tags Table
CREATE TABLE tags (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- 3. Products Table (with embedded Money and optimistic locking version)
CREATE TABLE products (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price_amount NUMERIC(12, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    category VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 0
);

-- 4. Product-Tags Join Table (ManyToMany)
CREATE TABLE product_tags (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, tag_id)
);

-- 5. Orders Table
CREATE TABLE orders (
    id BIGINT NOT NULL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    status VARCHAR(30) NOT NULL,
    placed_at TIMESTAMP NOT NULL,
    version INT NOT NULL DEFAULT 0
);

-- 6. Order Lines Table
CREATE TABLE order_lines (
    id BIGINT NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    unit_price_amount NUMERIC(12, 2) NOT NULL,
    unit_price_currency VARCHAR(3) NOT NULL
);
