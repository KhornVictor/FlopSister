# FlopSister — RESTful Web Services
## Task 1: Product REST API
This task implements a RESTful API using Jakarta REST (JAX-RS), Jersey, and an embedded Grizzly HTTP server.

### Base URL
`http://localhost:8080/api/`

### Available Endpoints
| Method | Endpoint | Description |
|---|---|---|
| GET | `/products` | Retrieve all products |
| GET | `/products/{id}` | Retrieve a product by ID |

### Product Repository
The application uses an `InMemoryProductRepository` backed by `ConcurrentHashMap` and `AtomicLong`.
Three products are seeded when the repository is created:
- Kettle (KT-01)
- Mug (MG-01)
- Tea (TE-01)

### Dependency Injection
Jersey uses `AbstractBinder` to bind one `ProductRepository` instance. `ProductResource` receives the repository through constructor injection.

### Stateless Resource Design
The resource class has no mutable fields because it is stateless and can safely handle multiple HTTP requests without sharing request-specific state.

### Running the Application
```bash
bash mvnw clean compile
bash mvnw -Dexec.executable=java -Dexec.args="-classpath target/classes:%classpath edu.itc.shop.Main" exec:exec
```

### HTTP Test Evidence
See [`docs/task1-http.md`](docs/task1-http.md) for the four HTTP test commands and their actual responses.
