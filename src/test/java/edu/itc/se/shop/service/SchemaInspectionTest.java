package edu.itc.se.shop.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SchemaInspectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void inspectGeneratedDatabaseSchema() throws Exception {

        try (Connection connection = dataSource.getConnection()) {

            DatabaseMetaData metadata = connection.getMetaData();

            System.out.println("\n========== DATABASE TABLES ==========");

            try (ResultSet tables = metadata.getTables(
                    null, "PUBLIC", "%", new String[]{"TABLE"})) {

                while (tables.next()) {

                    String tableName = tables.getString("TABLE_NAME");

                    System.out.println("\nTABLE: " + tableName);

                    // 1. Columns
                    System.out.println("--- COLUMNS ---");

                    try (ResultSet columns = metadata.getColumns(
                            null, "PUBLIC", tableName, "%")) {

                        while (columns.next()) {
                            System.out.printf(
                                    "%s | TYPE=%s | SIZE=%s | NULLABLE=%s | DEFAULT=%s%n",
                                    columns.getString("COLUMN_NAME"),
                                    columns.getString("TYPE_NAME"),
                                    columns.getString("COLUMN_SIZE"),
                                    columns.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls
                                            ? "NO" : "YES",
                                    columns.getString("COLUMN_DEF")
                            );
                        }
                    }

                    // 2. Primary keys
                    System.out.println("--- PRIMARY KEYS ---");

                    try (ResultSet keys = metadata.getPrimaryKeys(
                            null, "PUBLIC", tableName)) {

                        while (keys.next()) {
                            System.out.printf(
                                    "PK=%s | COLUMN=%s%n",
                                    keys.getString("PK_NAME"),
                                    keys.getString("COLUMN_NAME")
                            );
                        }
                    }

                    // 3. Foreign keys
                    System.out.println("--- FOREIGN KEYS ---");

                    try (ResultSet keys = metadata.getImportedKeys(
                            null, "PUBLIC", tableName)) {

                        while (keys.next()) {
                            System.out.printf(
                                    "%s -> %s.%s%n",
                                    keys.getString("FKCOLUMN_NAME"),
                                    keys.getString("PKTABLE_NAME"),
                                    keys.getString("PKCOLUMN_NAME")
                            );
                        }
                    }

                    // 4. Unique indexes and constraints
                    System.out.println("--- UNIQUE INDEXES / CONSTRAINTS ---");

                    try (ResultSet indexes = metadata.getIndexInfo(
                            null, "PUBLIC", tableName, true, false)) {

                        while (indexes.next()) {

                            String indexName = indexes.getString("INDEX_NAME");
                            String columnName = indexes.getString("COLUMN_NAME");

                            if (indexName != null && columnName != null) {
                                System.out.printf(
                                        "INDEX=%s | COLUMN=%s | NON_UNIQUE=%s%n",
                                        indexName,
                                        columnName,
                                        indexes.getBoolean("NON_UNIQUE")
                                );
                            }
                        }
                    }
                }
            }

            // 5. Database sequences
            System.out.println("\n========== DATABASE SEQUENCES ==========");

            String sequenceQuery =
                    "SELECT SEQUENCE_NAME, START_VALUE, INCREMENT, BASE_VALUE " +
                    "FROM INFORMATION_SCHEMA.SEQUENCES " +
                    "WHERE SEQUENCE_SCHEMA = 'PUBLIC' " +
                    "ORDER BY SEQUENCE_NAME";

            try (Statement statement = connection.createStatement();
                 ResultSet sequences = statement.executeQuery(sequenceQuery)) {

                while (sequences.next()) {
                    System.out.printf(
                            "%s | START=%s | INCREMENT=%s | BASE=%s%n",
                            sequences.getString("SEQUENCE_NAME"),
                            sequences.getString("START_VALUE"),
                            sequences.getString("INCREMENT"),
                            sequences.getString("BASE_VALUE")
                    );
                }
            }

            System.out.println("\n========== END SCHEMA INSPECTION ==========");
        }
    }
}
