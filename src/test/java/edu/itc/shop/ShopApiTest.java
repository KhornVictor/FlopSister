package edu.itc.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

/**
 * Full integration test suite covering all promised HTTP status codes, HATEOAS orders,
 * idempotent retries, OpenAPI exposure, and cross-cutting filters.
 */
class ShopApiTest extends JerseyTest {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_KEY = "demo-123";
    private static final MediaType PROBLEM_TYPE = MediaType.valueOf("application/problem+json");

    @Override
    protected Application configure() {
        return Main.config();
    }

    @Test
    void returns200WithPaginationHeaders() {
        try (Response response = target("products")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus());
            assertTrue(response.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE));
            assertEquals("33", response.getHeaderString("X-Total-Count"));
            assertNotNull(response.getLink("first"));
            assertNotNull(response.getLink("prev"));
            assertNotNull(response.getLink("next"));
            assertNotNull(response.getLink("last"));
            assertNotNull(response.getHeaderString("X-Correlation-Id"));
        }
    }

    @Test
    void returns201AndLocationOnCreateOrder() {
        try (Response response = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header("Idempotency-Key", "status-201")
                .post(Entity.json("{\"lines\":[{\"productId\":2,\"quantity\":1}]}"))) {
            assertEquals(201, response.getStatus());
            assertTrue(response.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE));
            assertNotNull(response.getLocation());
            assertEquals("false", response.getHeaderString("Idempotency-Replayed"));
            assertNotNull(response.getLink("self"));
            assertNotNull(response.getLink("pay"));
            assertNotNull(response.getLink("cancel"));
        }
    }

    @Test
    void returns204OnDeleteProduct() {
        URI createdLocation;
        try (Response created = target("products")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .post(Entity.json("{\"name\":\"Disposable\",\"sku\":\"DP-01\",\"price\":2.50,\"stock\":3}"))) {
            assertEquals(201, created.getStatus());
            createdLocation = created.getLocation();
            assertNotNull(createdLocation);
        }

        try (Response deleted = client().target(createdLocation)
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .delete()) {
            assertEquals(204, deleted.getStatus());
        }
    }

    @Test
    void returns304WhenIfNoneMatchMatchesEtag() {
        String etagValue;
        try (Response initial = target("products/1").request().get()) {
            assertEquals(200, initial.getStatus());
            assertNotNull(initial.getEntityTag());
            etagValue = initial.getEntityTag().getValue();
        }

        try (Response notModified = target("products/1")
                .request()
                .header(HttpHeaders.IF_NONE_MATCH, "\"" + etagValue + "\"")
                .get()) {
            assertEquals(304, notModified.getStatus());
            assertNotNull(notModified.getEntityTag());
            assertEquals(etagValue, notModified.getEntityTag().getValue());
        }
    }

    @Test
    void returns400ProblemOnValidationError() {
        try (Response response = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header("Idempotency-Key", "status-400")
                .post(Entity.json("{\"lines\":[{\"productId\":1,\"quantity\":0}]}"))) {
            assertProblem(response, 400);
        }
    }

    @Test
    void returns401ProblemWhenApiKeyMissing() {
        try (Response response = target("orders")
                .request()
                .header("Idempotency-Key", "status-401")
                .post(Entity.json("{\"lines\":[{\"productId\":1,\"quantity\":1}]}"))) {
            assertProblem(response, 401);
            String auth = response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
            assertNotNull(auth);
            assertTrue(auth.contains("ApiKey"));
        }
    }

    @Test
    void returns404ProblemForUnknownOrder() {
        try (Response response = target("orders/o-9999").request().get()) {
            assertProblem(response, 404);
        }
    }

    @Test
    void returns406ProblemOnUnsupportedAcceptHeader() {
        try (Response response = target("products/1")
                .request(MediaType.TEXT_PLAIN)
                .get()) {
            assertProblem(response, 406);
        }
    }

    @Test
    void returns409ProblemWhenStockIsInsufficient() {
        try (Response response = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header("Idempotency-Key", "status-409")
                .post(Entity.json("{\"lines\":[{\"productId\":3,\"quantity\":1}]}"))) {
            assertProblem(response, 409);
        }
    }

    @Test
    void returns412OnStaleIfMatchPrecondition() {
        try (Response response = target("products/1")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header(HttpHeaders.IF_MATCH, "\"999\"")
                .put(Entity.json("{\"name\":\"Kettle XL\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":12}"))) {
            assertEquals(412, response.getStatus());
            assertNotNull(response.getEntityTag());
        }
    }

    @Test
    void returns415ProblemOnUnsupportedMediaType() {
        try (Response response = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header("Idempotency-Key", "status-415")
                .post(Entity.text("lines"))) {
            assertProblem(response, 415);
        }
    }

    @Test
    void returns422ProblemWhenIdempotencyKeyReusedWithDifferentBody() {
        try (Response first = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header("Idempotency-Key", "status-422")
                .post(Entity.json("{\"lines\":[{\"productId\":2,\"quantity\":1}]}"))) {
            assertEquals(201, first.getStatus());
        }

        try (Response mismatch = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header("Idempotency-Key", "status-422")
                .post(Entity.json("{\"lines\":[{\"productId\":2,\"quantity\":2}]}"))) {
            assertProblem(mismatch, 422);
        }
    }

    @Test
    void returns428ProblemWhenIdempotencyKeyMissing() {
        try (Response response = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .post(Entity.json("{\"lines\":[{\"productId\":2,\"quantity\":1}]}"))) {
            assertProblem(response, 428);
        }
    }

    @Test
    void placingTheSameOrderTwiceCreatesOneOrder() {
        Entity<String> body = Entity.json("{\"lines\":[{\"productId\":2,\"quantity\":2}]}");

        URI firstLocation;
        try (Response first = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header("Idempotency-Key", "k-replay")
                .post(body)) {
            assertEquals(201, first.getStatus());
            assertEquals("false", first.getHeaderString("Idempotency-Replayed"));
            firstLocation = first.getLocation();
            assertNotNull(firstLocation);
        }

        try (Response second = target("orders")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header("Idempotency-Key", "k-replay")
                .post(body)) {
            assertEquals(201, second.getStatus());
            assertEquals("true", second.getHeaderString("Idempotency-Replayed"));
            assertEquals(firstLocation, second.getLocation());
        }
    }

    @Test
    void concurrentIdempotentRetriesCreateSingleOrder() throws Exception {
        int beforeStock = readProductStock(1);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Response>> calls = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                calls.add(() -> target("orders")
                        .request()
                        .header(API_KEY_HEADER, API_KEY)
                        .header("Idempotency-Key", "k-concurrent")
                        .post(Entity.json("{\"lines\":[{\"productId\":1,\"quantity\":1}]}")));
            }
            List<Future<Response>> futures = pool.invokeAll(calls);
            URI expectedLocation = null;
            int freshCount = 0;
            int replayCount = 0;
            for (Future<Response> future : futures) {
                try (Response response = future.get()) {
                    assertEquals(201, response.getStatus());
                    if (expectedLocation == null) {
                        expectedLocation = response.getLocation();
                    } else {
                        assertEquals(expectedLocation, response.getLocation());
                    }
                    if ("false".equals(response.getHeaderString("Idempotency-Replayed"))) {
                        freshCount++;
                    } else if ("true".equals(response.getHeaderString("Idempotency-Replayed"))) {
                        replayCount++;
                    }
                }
            }
            assertEquals(1, freshCount);
            assertEquals(3, replayCount);
            assertEquals(beforeStock - 1, readProductStock(1));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void drivesPaymentAndCancellationFollowingHateoasLinks() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            // 1. Place order and follow pay link -> PAID; cancel afterwards must fail with 409
            Map<String, String> links;
            try (Response placed = target("orders")
                    .request()
                    .header(API_KEY_HEADER, API_KEY)
                    .header("Idempotency-Key", "hateoas-pay")
                    .post(Entity.json("{\"lines\":[{\"productId\":2,\"quantity\":1}]}"))) {
                assertEquals(201, placed.getStatus());
                Map<String, Object> body = jsonb.fromJson(placed.readEntity(String.class), Map.class);
                assertEquals("NEW", body.get("status"));
                links = (Map<String, String>) body.get("_links");
                assertNotNull(links.get("self"));
                assertNotNull(links.get("pay"));
                assertNotNull(links.get("cancel"));
                assertNotNull(links.get("product-2"));
            }

            try (Response paid = client().target(links.get("pay"))
                    .request()
                    .header(API_KEY_HEADER, API_KEY)
                    .put(Entity.text(""))) {
                assertEquals(200, paid.getStatus());
                Map<String, Object> body = jsonb.fromJson(paid.readEntity(String.class), Map.class);
                assertEquals("PAID", body.get("status"));
                Map<String, String> paidLinks = (Map<String, String>) body.get("_links");
                assertNotNull(paidLinks.get("self"));
                assertFalse(paidLinks.containsKey("pay"));
                assertFalse(paidLinks.containsKey("cancel"));
            }

            try (Response cancelPaid = client().target(links.get("self"))
                    .request()
                    .header(API_KEY_HEADER, API_KEY)
                    .delete()) {
                assertProblem(cancelPaid, 409);
            }

            // 2. Place another order and follow cancel link -> CANCELLED and stock restored
            int stockBefore = readProductStock(2);
            Map<String, String> cancelLinks;
            try (Response placed = target("orders")
                    .request()
                    .header(API_KEY_HEADER, API_KEY)
                    .header("Idempotency-Key", "hateoas-cancel")
                    .post(Entity.json("{\"lines\":[{\"productId\":2,\"quantity\":3}]}"))) {
                assertEquals(201, placed.getStatus());
                assertEquals(stockBefore - 3, readProductStock(2));
                Map<String, Object> body = jsonb.fromJson(placed.readEntity(String.class), Map.class);
                cancelLinks = (Map<String, String>) body.get("_links");
            }

            try (Response cancelled = client().target(cancelLinks.get("cancel"))
                    .request()
                    .header(API_KEY_HEADER, API_KEY)
                    .delete()) {
                assertEquals(200, cancelled.getStatus());
                Map<String, Object> body = jsonb.fromJson(cancelled.readEntity(String.class), Map.class);
                assertEquals("CANCELLED", body.get("status"));
                Map<String, String> afterLinks = (Map<String, String>) body.get("_links");
                assertFalse(afterLinks.containsKey("pay"));
                assertFalse(afterLinks.containsKey("cancel"));
            }
            assertEquals(stockBefore, readProductStock(2));
        }
    }

    @Test
    void servesGeneratedOpenApiYaml() {
        try (Response response = target("openapi")
                .request("application/yaml")
                .get()) {
            assertEquals(200, response.getStatus());
            assertTrue(response.getMediaType().isCompatible(MediaType.valueOf("application/yaml")));
            String yaml = response.readEntity(String.class);
            assertTrue(yaml.contains("openapi: 3.1"));
            assertTrue(yaml.contains("/products"));
            assertTrue(yaml.contains("/orders"));
        }
    }

    @Test
    void echoesCorrelationIdAndCompressesProductListWhenAccepted() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(target("products").getUri())
                .header("X-Correlation-Id", "shop-trace-1")
                .header("Origin", "http://localhost:5173")
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertEquals("shop-trace-1", response.headers().firstValue("X-Correlation-Id").orElse(null));
        assertEquals("http://localhost:5173",
                response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
        assertEquals("gzip", response.headers().firstValue(HttpHeaders.CONTENT_ENCODING).orElse(null));
        assertTrue(response.headers().firstValue(HttpHeaders.VARY).orElse("").contains(HttpHeaders.ACCEPT_ENCODING));

        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
            String json = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("Kettle"));
        }
    }

    @SuppressWarnings("unchecked")
    private int readProductStock(long id) throws Exception {
        try (Response response = target("products/" + id).request(MediaType.APPLICATION_JSON).get();
             Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals(200, response.getStatus());
            Map<String, Object> map = jsonb.fromJson(response.readEntity(String.class), Map.class);
            return ((Number) map.get("stock")).intValue();
        }
    }

    private static void assertProblem(Response response, int status) {
        assertEquals(status, response.getStatus());
        assertNotNull(response.getMediaType());
        assertTrue(response.getMediaType().isCompatible(PROBLEM_TYPE));
    }
}
