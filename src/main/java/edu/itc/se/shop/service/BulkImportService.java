package edu.itc.se.shop.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;

@Service
public class BulkImportService {

    private final SessionFactory sessionFactory;
    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BulkImportService(EntityManagerFactory emf, ProductRepository productRepository) {
        this.sessionFactory = emf.unwrap(SessionFactory.class);
        this.productRepository = productRepository;
    }

    // 1. StatelessSession insertMultiple(500)
    public int importCsv(Path csv) throws IOException {
        int imported = 0;
        try (StatelessSession ss = sessionFactory.openStatelessSession();
             Stream<String> lines = Files.lines(csv).skip(1)) {
            Transaction tx = ss.beginTransaction();
            ss.setJdbcBatchSize(500);
            List<Product> chunk = new ArrayList<>(500);
            for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
                chunk.add(parse(it.next()));
                if (chunk.size() == 500) {
                    ss.insertMultiple(chunk);
                    imported += chunk.size();
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                ss.insertMultiple(chunk);
                imported += chunk.size();
            }
            tx.commit();
        }
        return imported;
    }

    // 2. saveAll in one @Transactional method, no clear
    @Transactional
    public int importSaveAll(List<Product> products) {
        productRepository.saveAll(products);
        return products.size();
    }

    // 3. saveAll + flush/clear every 500
    @Transactional
    public int importSaveAllWithFlushClear(List<Product> products) {
        for (int i = 0; i < products.size(); i++) {
            entityManager.persist(products.get(i));
            if ((i + 1) % 500 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
        return products.size();
    }

    public Product parse(String line) {
        String[] parts = line.split(";");
        String sku = parts[0].trim();
        String name = parts[1].trim();
        BigDecimal amount = new BigDecimal(parts[2].trim());
        String currency = parts[3].trim();
        Category category = Category.valueOf(parts[4].trim());
        int stock = Integer.parseInt(parts[5].trim());
        return new Product(sku, name, new Money(amount, currency), category, stock);
    }
}
