# Structure of Each Tasks

## Task 0: KHORN VICTOR - Project Setup & Baseline Toolchain

Files and directories created during the initial setup:

```text
shop-jpa/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties
└── src/
    ├── main/
    │   ├── java/edu/itc/shop/
    │   │   └── ShopJpaApplication.java
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/               # Empty directory placeholder
    └── test/resources/
        └── application-test.yml
```

## Task 1: HOUR SAMNANG - Entities and Mappings, Validated by Flyway

Files introduced for the core domain model, baseline migration, and mapping verification:

```text
shop-jpa/
├── README.md                               # Document deliberate schema mismatches
└── src/
    ├── main/
    │   ├── java/edu/itc/shop/domain/
    │   │   ├── Address.java                # @Embeddable record
    │   │   ├── Category.java               # Enum
    │   │   ├── Customer.java               # Entity with embedded addresses
    │   │   ├── Money.java                  # @Embeddable record
    │   │   ├── Order.java                  # Initial entity skeleton
    │   │   ├── OrderLine.java              # Initial entity skeleton
    │   │   ├── OrderStatus.java            # Enum
    │   │   ├── Product.java                # Entity
    │   │   └── Tag.java                    # Entity
    │   └── resources/db/migration/
    │       └── V1__init.sql                # 6 tables, 5 sequences
    └── test/java/edu/itc/shop/domain/
        └── ProductMappingTest.java
```

## Task 2: KIM BORAMEY - Associations, Cascading, and Entity Identity

Domain updates for relationships, helper methods, identity contracts, and cascade tests:

```text
shop-jpa/
├── README.md                               # Excerpts: delete SQL & toString recursion
└── src/
    ├── main/java/edu/itc/shop/domain/
    │   ├── Customer.java                   # Updated equals/hashCode/toString
    │   ├── Order.java                      # @OneToMany lines, addLine, removeLine, total
    │   ├── OrderLine.java                  # Owning side @ManyToOne, updated equals/hashCode
    │   ├── Product.java                    # @ManyToMany Set<Tag> tags, tag() helper
    │   └── Tag.java                        # Inverse @ManyToMany Set<Product> products
    └── test/java/edu/itc/shop/domain/
        ├── EntityIdentityTest.java
        └── OrderCascadeTest.java
```

## Task 3: KHORN VICTOR - Repositories, Queries, Projections, and Paginated Endpoints

Persistence queries, dynamic specifications, dev seeder, DTOs, and REST controllers:

```text
shop-jpa/
├── api-session.md                          # HTTP/curl transcripts
├── README.md                               # Document boot-time typo error
└── src/
    ├── main/
    │   ├── java/edu/itc/shop/
    │   │   ├── repository/
    │   │   │   ├── CustomerRepository.java
    │   │   │   ├── OrderRepository.java    # Derived queries, @Query fetch joins
    │   │   │   ├── ProductRepository.java  # Derived, native queries, Spec executor
    │   │   │   └── ProductSpecs.java       # Specification.allOf combinations
    │   │   ├── service/
    │   │   │   └── DataSeeder.java         # @Profile("dev") ApplicationRunner
    │   │   └── web/
    │   │       ├── ApiExceptionHandler.java
    │   │       ├── OrderController.java    # /api/orders, /api/customers/{email}/orders
    │   │       ├── ProductController.java  # /api/products
    │   │       └── dto/
    │   │           ├── OrderSummary.java   # Interface projection
    │   │           ├── PlaceOrderRequest.java
    │   │           └── ProductView.java    # Record projection
    │   └── resources/
    │       └── application-dev.yml         # Dev profile configuration
    └── test/java/edu/itc/shop/repository/
        └── OrderRepositoryTest.java
```

## Task 4: HENG MEYLORNG - Entity States, Dirty Checking, Transactions, and Optimistic Locking

Implemented in the current `FlopSister` workspace, using its existing `edu.itc.se.shop` package:

```text
FlopSister/
├── README.md                               # Dirty checking, merge, read-only trap, flush order, logs
├── pom.xml                                 # Adds Spring Data JPA
└── src/
    ├── main/
    │   ├── java/edu/itc/se/shop/
    │   │   ├── domain/                     # Customer, Order, OrderStatus, Product, Tag
    │   │   ├── repository/                 # JPA repositories
    │   │   ├── service/
    │   │   │   ├── OrderFacade.java        # changeStatusWithRetry loop
    │   │   │   └── OrderService.java       # markPaid, changeStatus, view
    │   │   └── web/dto/OrderDto.java
    │   └── resources/application.yml        # Schema update for the local H2 app
    └── test/
        ├── java/edu/itc/se/shop/
        │   ├── service/
        │   │   ├── FlushOrderTest.java
        │   │   ├── OptimisticLockingTest.java
        │   │   └── OrderServiceTest.java
        │   └── support/TestData.java
        └── resources/application-test.yml  # Test schema and required logging levels
```

## Task 5: BE MENGSEANG - N+1, Performance Tuning, and a Second Migration

Index migration, JDBC batching, fetch optimization tests, and execution statistics:

```text
shop-jpa/
├── README.md                               # Performance comparison table, EXPLAIN plans
└── src/
    ├── main/
    │   ├── java/edu/itc/shop/domain/
    │   │   ├── domain/Order.java           # Added @BatchSize(size = 16)
    │   │   └── service/ReportService.java  # linesPerPaidOrder variants
    │   └── resources/db/migration/
    │       └── V2__indexes.sql             # 3 composite indexes
    └── test/java/edu/itc/shop/
        ├── service/
        │   ├── BatchInsertTest.java
        │   └── NPlusOneTest.java
        └── support/
            └── QueryCounter.java           # SessionFactory statistics wrapper
```

## Challenge: KHORN VICTOR - Reporting and Bulk Module (Extra Credit)

Stateless bulk loader, pessimistic lock handling, Criteria API repository fragment, and infinite scroll:

```text
shop-jpa/
├── README.md                               # Mermaid ER diagram & SQL mapping table
└── src/
    ├── main/java/edu/itc/shop/
    │   ├── repository/
    │   │   ├── ProductReportRepository.java
    │   │   └── ProductReportRepositoryImpl.java # CriteriaBuilder & Metamodel
    │   ├── service/
    │   │   ├── BulkImportService.java           # StatelessSession CSV importer
    │   │   └── StockService.java                # Pessimistic locking reservation
    │   └── web/dto/
    │       ├── OrderFeedResponse.java           # Keyset scroll cursor wrapper
    │       └── ProductReportRow.java            # cb.construct projection record
    └── test/java/edu/itc/shop/service/
        ├── BulkImportTest.java
        └── StockReservationTest.java
```
