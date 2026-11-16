package edu.itc.shop.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryProductRepository implements ProductRepository {

    private final ConcurrentMap<Long, Product> products =
            new ConcurrentHashMap<>();

    private final AtomicLong nextId = new AtomicLong(0);

    public InMemoryProductRepository() {
        save(new Product(0, "Kettle", "KT-01",
                new BigDecimal("24.90"), 12, 0));

        save(new Product(0, "Mug", "MG-01",
                new BigDecimal("6.50"), 40, 0));

        save(new Product(0, "Tea", "TE-01",
                new BigDecimal("4.20"), 0, 0));
        String[][] extraProducts = {
        {"Notebook", "NB-01", "2.50", "100"},
        {"Pen", "PN-01", "1.20", "200"},
        {"Pencil", "PC-01", "0.80", "150"},
        {"Eraser", "ER-01", "0.50", "80"},
        {"Ruler", "RL-01", "1.50", "60"},
        {"Backpack", "BP-01", "29.90", "15"},
        {"Laptop", "LP-01", "899.00", "8"},
        {"Mouse", "MS-01", "15.90", "25"},
        {"Keyboard", "KB-01", "35.00", "18"},
        {"Monitor", "MN-01", "179.00", "10"},
        {"Headphones", "HP-01", "49.90", "20"},
        {"USB Cable", "UC-01", "5.00", "50"},
        {"Charger", "CH-01", "12.90", "30"},
        {"Webcam", "WC-01", "39.90", "12"},
        {"Desk Lamp", "DL-01", "22.50", "16"},
        {"Water Bottle", "WB-01", "9.90", "45"},
        {"Coffee", "CF-01", "7.50", "35"},
        {"Sugar", "SG-01", "3.20", "0"},
        {"Milk", "MK-01", "2.80", "25"},
        {"Bread", "BR-01", "1.90", "40"},
        {"Rice", "RC-01", "18.00", "30"},
        {"Pasta", "PA-01", "3.50", "55"},
        {"Olive Oil", "OO-01", "11.90", "14"},
        {"Chocolate", "CL-01", "4.50", "60"},
        {"Cookies", "CK-01", "3.90", "0"},
        {"T-Shirt", "TS-01", "14.90", "22"},
        {"Jeans", "JN-01", "39.90", "12"},
        {"Jacket", "JK-01", "69.90", "7"},
        {"Sneakers", "SN-01", "59.90", "9"},
        {"Umbrella", "UM-01", "12.50", "18"}
};

for (String[] item : extraProducts) {
    save(new Product(
            0,
            item[0],
            item[1],
            new BigDecimal(item[2]),
            Integer.parseInt(item[3]),
            0
    ));
   }
 }
    @Override
    public List<Product> findAll() {
        return products.values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return products.values().stream()
                .filter(product -> product.sku().equals(sku))
                .findFirst();
    }

    @Override
    public Product save(Product product) {
        long id = product.id() == 0
                ? nextId.incrementAndGet()
                : product.id();

        Product saved = new Product(
                id,
                product.name(),
                product.sku(),
                product.price(),
                product.stock(),
                product.id() == 0 ? 1 : product.version() + 1
        );

        products.put(id, saved);
        nextId.accumulateAndGet(id, Math::max);

        return saved;
    }

    @Override
    public boolean delete(long id) {
        return products.remove(id) != null;
    }
}
