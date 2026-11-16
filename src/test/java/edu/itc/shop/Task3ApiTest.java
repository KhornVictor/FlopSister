package edu.itc.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.itc.shop.api.dto.StockChange;
import jakarta.validation.Validation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

class Task3ApiTest extends JerseyTest {

    @Override
    protected Application configure() {
        return Main.config().register(FailingResource.class);
    }

    @Test
    void filtersAndSortsProducts() {
        try (Response response = target("products")
                .queryParam("minPrice", "5 EUR")
                .queryParam("inStock", true)
                .queryParam("sort", "price,desc")
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus());
            String body = response.readEntity(String.class);
            assertTrue(body.contains("Kettle"));
            assertTrue(body.contains("Mug"));
            assertTrue(body.indexOf("Kettle") < body.indexOf("Mug"));
            assertFalse(body.contains("Tea"));
        }

        try (Response response = target("products")
                .queryParam("q", "mUG")
                .queryParam("maxPrice", "6.50 EUR")
                .request(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus());
            String body = response.readEntity(String.class);
            assertTrue(body.contains("Mug"));
            assertFalse(body.contains("Kettle"));
            assertFalse(body.contains("Tea"));
        }
    }

    @Test
    void invalidQueryAndBodyUseProblemResponses() {
        try (Response response = target("products")
                .queryParam("minPrice", "abc")
                .request()
                .get()) {
            assertProblem(response, 400);
            assertTrue(response.readEntity(String.class).contains("Cannot parse Money"));
        }

        try (Response response = target("products")
                .queryParam("sort", "colour,asc")
                .request()
                .get()) {
            assertProblem(response, 400);
            assertTrue(response.readEntity(String.class).contains("\"field\":\"sort\""));
        }

        try (Response response = target("products")
                .request()
                .header("X-Api-Key", "demo-123")
                .post(jakarta.ws.rs.client.Entity.entity(
                        "{\"name\":\"\",\"sku\":\"kettle\",\"price\":0,\"stock\":-1}",
                        MediaType.APPLICATION_JSON))) {
            assertProblem(response, 400);
            String body = response.readEntity(String.class);
            assertTrue(body.contains("\"field\":\"name\""));
            assertTrue(body.contains("\"field\":\"sku\""));
            assertTrue(body.contains("\"field\":\"price\""));
            assertTrue(body.contains("\"field\":\"stock\""));
        }

        try (Response response = target("products")
                .queryParam("minPrice", "5 USD")
                .request()
                .get()) {
            assertProblem(response, 400);
        }
    }

    @Test
    void notFoundAndUnexpectedErrorsAreProblemResponses() {
        try (Response response = target("products/99").request().get()) {
            assertProblem(response, 404);
            assertTrue(response.readEntity(String.class).contains("Product 99 not found"));
        }

        try (Response response = target("task3-test/fail").request().get()) {
            assertProblem(response, 500);
            String body = response.readEntity(String.class);
            assertTrue(body.contains("reference "));
            assertFalse(body.contains("sensitive exception detail"));
        }

        try (Response response = target("products")
                .request()
                .header("X-Api-Key", "demo-123")
                .post(jakarta.ws.rs.client.Entity.entity(
                        "Tea", MediaType.TEXT_PLAIN))) {
            assertProblem(response, 415);
        }

        try (Response response = target("products")
                .request()
                .header("X-Api-Key", "demo-123")
                .post(jakarta.ws.rs.client.Entity.entity(
                        "{\"name\":\"Duplicate\",\"sku\":\"KT-01\",\"price\":1,\"stock\":1}",
                        MediaType.APPLICATION_JSON))) {
            assertProblem(response, 409);
        }
    }

    private static void assertProblem(Response response, int status) {
        assertEquals(status, response.getStatus());
        assertTrue(response.getMediaType().isCompatible(
                MediaType.valueOf("application/problem+json")));
    }

    @Test
    void stockChangeDeltaIsValidated() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new StockChange(1001));
            assertEquals(1, violations.size());
            assertEquals("delta", violations.iterator().next().getPropertyPath().toString());
        }
    }

    @Path("task3-test/fail")
    public static class FailingResource {
        @GET
        public String fail() {
            throw new IllegalStateException("sensitive exception detail");
        }
    }
}
