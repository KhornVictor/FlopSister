package edu.itc.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

/**
 * Integration tests covering Task 5 pagination, caching, versioning, and reviews sub-resource.
 */
class Task5ApiTest extends JerseyTest {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_KEY = "demo-123";

    @Override
    protected Application configure() {
        return Main.config();
    }

    @Test
    void paginatesProductsWithTotalCountAndLinks() throws Exception {
        try (Response response = target("products")
                .queryParam("page", 1)
                .queryParam("size", 10)
                .request(MediaType.APPLICATION_JSON)
                .get();
             Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals(200, response.getStatus());
            assertEquals("33", response.getHeaderString("X-Total-Count"));
            List<?> items = jsonb.fromJson(response.readEntity(String.class), List.class);
            assertEquals(10, items.size());

            Link first = response.getLink("first");
            Link prev = response.getLink("prev");
            Link next = response.getLink("next");
            Link last = response.getLink("last");
            assertNotNull(first);
            assertNotNull(prev);
            assertNotNull(next);
            assertNotNull(last);
            assertTrue(first.getUri().toString().contains("page=0"));
            assertTrue(first.getUri().toString().contains("size=10"));
            assertTrue(prev.getUri().toString().contains("page=0"));
            assertTrue(next.getUri().toString().contains("page=2"));
            assertTrue(last.getUri().toString().contains("page=3"));
        }

        try (Response firstPage = target("products")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, firstPage.getStatus());
            assertNotNull(firstPage.getLink("first"));
            assertNull(firstPage.getLink("prev"));
            assertNotNull(firstPage.getLink("next"));
            assertNotNull(firstPage.getLink("last"));
        }

        try (Response lastPage = target("products")
                .queryParam("page", 3)
                .queryParam("size", 10)
                .request(MediaType.APPLICATION_JSON)
                .get();
             Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals(200, lastPage.getStatus());
            List<?> items = jsonb.fromJson(lastPage.readEntity(String.class), List.class);
            assertEquals(3, items.size());
            assertNotNull(lastPage.getLink("first"));
            assertNotNull(lastPage.getLink("prev"));
            assertNull(lastPage.getLink("next"));
            assertNotNull(lastPage.getLink("last"));
        }

        try (Response filtered = target("products")
                .queryParam("minPrice", "5 EUR")
                .queryParam("size", 1)
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, filtered.getStatus());
            assertEquals("2", filtered.getHeaderString("X-Total-Count"));
            assertTrue(filtered.getLink("last").getUri().toString().contains("page=1"));
        }
    }

    @Test
    void validatesPaginationQueryParameters() {
        try (Response negativePage = target("products")
                .queryParam("page", -1)
                .request()
                .get()) {
            assertProblem(negativePage, 400);
        }

        try (Response tooLargeSize = target("products")
                .queryParam("size", 101)
                .request()
                .get()) {
            assertProblem(tooLargeSize, 400);
        }

        try (Response zeroSize = target("products")
                .queryParam("size", 0)
                .request()
                .get()) {
            assertProblem(zeroSize, 400);
        }
    }

    @Test
    void supportsEtagCachingAndConditionalPut() throws Exception {
        try (Response initial = target("products/1").request().get()) {
            assertEquals(200, initial.getStatus());
            assertNotNull(initial.getEntityTag());
            assertEquals("1", initial.getEntityTag().getValue());
            String cc = initial.getHeaderString(HttpHeaders.CACHE_CONTROL);
            assertNotNull(cc);
            assertTrue(cc.contains("private"));
            assertTrue(cc.contains("max-age=60"));
            assertTrue(cc.contains("no-transform"));
        }

        try (Response notModified = target("products/1")
                .request()
                .header(HttpHeaders.IF_NONE_MATCH, "\"1\"")
                .get()) {
            assertEquals(304, notModified.getStatus());
            assertEquals("1", notModified.getEntityTag().getValue());
        }

        String updateJson = "{\"name\":\"Kettle XL\",\"sku\":\"KT-01\",\"price\":29.90,\"stock\":12}";

        try (Response missingPrecondition = target("products/1")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .put(Entity.entity(updateJson, MediaType.APPLICATION_JSON))) {
            assertProblem(missingPrecondition, 428);
        }

        try (Response stalePrecondition = target("products/1")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header(HttpHeaders.IF_MATCH, "\"0\"")
                .put(Entity.entity(updateJson, MediaType.APPLICATION_JSON))) {
            assertEquals(412, stalePrecondition.getStatus());
        }

        try (Response updated = target("products/1")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .header(HttpHeaders.IF_MATCH, "\"1\"")
                .put(Entity.entity(updateJson, MediaType.APPLICATION_JSON))) {
            assertEquals(200, updated.getStatus());
            assertNotNull(updated.getEntityTag());
            assertEquals("2", updated.getEntityTag().getValue());
            assertTrue(updated.readEntity(String.class).contains("Kettle XL"));
        }

        try (Response afterUpdate = target("products/1")
                .request()
                .header(HttpHeaders.IF_NONE_MATCH, "\"1\"")
                .get()) {
            assertEquals(200, afterUpdate.getStatus());
            assertEquals("2", afterUpdate.getEntityTag().getValue());
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest patchRequest = HttpRequest.newBuilder(target("products/1/stock").getUri())
                .header(API_KEY_HEADER, API_KEY)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"delta\":-1}"))
                .build();
        HttpResponse<String> patchResponse = httpClient.send(patchRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, patchResponse.statusCode());
        assertTrue(patchResponse.body().contains("\"stock\":11"));

        try (Response afterPatch = target("products/1").request().get()) {
            assertEquals(200, afterPatch.getStatus());
            assertEquals("3", afterPatch.getEntityTag().getValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void negotiatesProductV1AndV2MediaTypes() throws Exception {
        try (Response plainDefault = target("products/1")
                .request("*/*")
                .get()) {
            assertEquals(200, plainDefault.getStatus());
            assertEquals(MediaType.APPLICATION_JSON_TYPE, plainDefault.getMediaType());
            String body = plainDefault.readEntity(String.class);
            assertFalse(body.contains("\"available\""));
        }

        try (Response explicitV1 = target("products/1")
                .request("application/vnd.shop.v1+json")
                .get()) {
            assertEquals(200, explicitV1.getStatus());
            assertEquals("application/vnd.shop.v1+json", explicitV1.getMediaType().toString());
        }

        try (Response v2InStock = target("products/1")
                .request("application/vnd.shop.v2+json")
                .get();
             Jsonb jsonb = JsonbBuilder.create()) {
            assertEquals(200, v2InStock.getStatus());
            assertEquals("application/vnd.shop.v2+json", v2InStock.getMediaType().toString());
            Map<String, Object> parsed = jsonb.fromJson(v2InStock.readEntity(String.class), Map.class);
            assertEquals(Boolean.TRUE, parsed.get("available"));
            Map<String, Object> price = (Map<String, Object>) parsed.get("price");
            assertNotNull(price);
            assertEquals("EUR", price.get("currency"));
        }

        try (Response v2OutOfStock = target("products/3")
                .request("application/vnd.shop.v2+json")
                .get()) {
            assertEquals(200, v2OutOfStock.getStatus());
            assertTrue(v2OutOfStock.readEntity(String.class).contains("\"available\":false"));
        }
    }

    @Test
    void managesReviewsSubResourceAndValidatesInputs() {
        try (Response initial = target("products/1/reviews")
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, initial.getStatus());
            assertEquals("[]", initial.readEntity(String.class));
        }

        try (Response invalid = target("products/1/reviews")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .post(Entity.entity(
                        "{\"stars\":6,\"author\":\"\",\"text\":\"ok\"}",
                        MediaType.APPLICATION_JSON))) {
            assertProblem(invalid, 400);
        }

        try (Response created = target("products/1/reviews")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .post(Entity.entity(
                        "{\"stars\":5,\"author\":\"Ana\",\"text\":\"Boils fast.\"}",
                        MediaType.APPLICATION_JSON))) {
            assertEquals(201, created.getStatus());
            assertNotNull(created.getLocation());
            assertTrue(created.getLocation().toString().endsWith("/products/1/reviews/1"));
            assertTrue(created.readEntity(String.class).contains("\"stars\":5"));
        }

        try (Response list = target("products/1/reviews")
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, list.getStatus());
            String body = list.readEntity(String.class);
            assertTrue(body.contains("\"author\":\"Ana\""));
            assertTrue(body.contains("\"text\":\"Boils fast.\""));
        }

        try (Response missingGet = target("products/999/reviews")
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertProblem(missingGet, 404);
        }

        try (Response missingPost = target("products/999/reviews")
                .request()
                .header(API_KEY_HEADER, API_KEY)
                .post(Entity.entity(
                        "{\"stars\":5,\"author\":\"Ana\",\"text\":\"Boils fast.\"}",
                        MediaType.APPLICATION_JSON))) {
            assertProblem(missingPost, 404);
        }
    }

    private static void assertProblem(Response response, int status) {
        assertEquals(status, response.getStatus());
        assertNotNull(response.getMediaType());
        assertTrue(response.getMediaType().isCompatible(
                MediaType.valueOf("application/problem+json")));
    }
}
