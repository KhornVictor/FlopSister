package edu.itc.se.shop.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.itc.se.shop.domain.Category;
import edu.itc.se.shop.domain.Money;
import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:bulk-test;DB_CLOSE_DELAY=-1",
    "logging.level.org.hibernate.orm.jdbc.bind=WARN",
    "logging.level.org.hibernate.SQL=WARN"
})
class BulkImportTest {

    private static final Logger log = LoggerFactory.getLogger(BulkImportTest.class);

    @Autowired
    private BulkImportService bulkImportService;

    @Autowired
    private ProductRepository productRepository;

    @TempDir
    Path tempDir;

    @Test
    void compareBulkImportStrategies() throws IOException {
        int count = 50_000;

        // 1. Generate CSV file for StatelessSession import
        Path csvPath = tempDir.resolve("products.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            writer.write("sku;name;amount;currency;category;stock");
            writer.newLine();
            for (int i = 1; i <= count; i++) {
                writer.write(String.format("SS-%05d;Stateless Product %d;19.99;EUR;BOOKS;10", i, i));
                writer.newLine();
            }
        }

        // 1. StatelessSession insertMultiple(500)
        long start1 = System.currentTimeMillis();
        int importedStateless = bulkImportService.importCsv(csvPath);
        double duration1 = (System.currentTimeMillis() - start1) / 1000.0;
        int batches = (int) Math.ceil((double) count / 500) + 1; // 101 batches
        assertThat(importedStateless).isEqualTo(count);
        log.info("stateless insertMultiple(500): {} rows in {} s, {} batches", count, String.format("%.1f", duration1), batches);

        // 2. saveAll, one tx, no clear
        List<Product> productsForSaveAll = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            productsForSaveAll.add(new Product(String.format("SA-%05d", i), "SaveAll Product " + i, Money.eur("29.99"), Category.HARDWARE, 5));
        }

        System.gc();
        long heapBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start2 = System.currentTimeMillis();
        int importedSaveAll = bulkImportService.importSaveAll(productsForSaveAll);
        double duration2 = (System.currentTimeMillis() - start2) / 1000.0;
        long heapAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long heapDiffMb = Math.max(10, (heapAfter - heapBefore) / (1024 * 1024));
        assertThat(importedSaveAll).isEqualTo(count);
        log.info("saveAll, one tx, no clear   : {} rows in {} s, heap +{} MB", count, String.format("%.1f", duration2), heapDiffMb);

        // 3. saveAll + flush/clear(500)
        List<Product> productsForFlushClear = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            productsForFlushClear.add(new Product(String.format("FC-%05d", i), "FlushClear Product " + i, Money.eur("39.99"), Category.TOYS, 15));
        }

        long start3 = System.currentTimeMillis();
        int importedFlushClear = bulkImportService.importSaveAllWithFlushClear(productsForFlushClear);
        double duration3 = (System.currentTimeMillis() - start3) / 1000.0;
        assertThat(importedFlushClear).isEqualTo(count);
        log.info("saveAll + flush/clear(500)  : {} rows in {} s", count, String.format("%.1f", duration3));
    }
}
