-- V1: Initial database schema

-- 1. Sequences (allocationSize = 50)
CREATE SEQUENCE customer_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE orders_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE order_line_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE product_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE tag_seq START WITH 1 INCREMENT BY 50;

-- 2. Customers
CREATE TABLE customers (
    id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_email UNIQUE (email)
);

-- 3. Products
CREATE TABLE products (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    stock INTEGER NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id)
);

-- 4. Tags
CREATE TABLE tags (
    id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT uk_tags_name UNIQUE (name)
);

-- 5. Orders
CREATE TABLE orders (
    id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    placed_at TIMESTAMP WITH TIME ZONE,
    status ENUM('CANCELLED', 'PAID', 'PENDING'),
    version BIGINT NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- 6. Order lines
CREATE TABLE order_lines (
    id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_order_lines_order
        FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_lines_product
        FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 7. Product-tag many-to-many join table
CREATE TABLE products_tags (
    products_id BIGINT NOT NULL,
    tags_id BIGINT NOT NULL,
    CONSTRAINT pk_products_tags PRIMARY KEY (products_id, tags_id),
    CONSTRAINT fk_products_tags_product
        FOREIGN KEY (products_id) REFERENCES products(id),
    CONSTRAINT fk_products_tags_tag
        FOREIGN KEY (tags_id) REFERENCES tags(id)
);
