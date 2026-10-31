DELETE FROM product;
DELETE FROM products;

INSERT INTO product (name, price, stock) VALUES
    ('Laptop', 1200.00, 10),
    ('Phone', 800.00, 20),
    ('Keyboard', 29.90, 8),
    ('Mouse', 15.50, 15),
    ('Monitor', 199.99, 5);

INSERT INTO products (name, price, stock) VALUES
    ('Keyboard', 49.99, 2),
    ('USB-C cable', 12.50, 25),
    ('Monitor', 199.99, 8),
    ('Laptop', 1200.00, 10),
    ('Mouse', 15.50, 15);
