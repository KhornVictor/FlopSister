package edu.itc.shop.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import edu.itc.shop.api.dto.LineDto;
import edu.itc.shop.api.dto.OrderDto;
import edu.itc.shop.api.dto.PlaceOrder;
import edu.itc.shop.api.util.Hashes;
import edu.itc.shop.domain.Line;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderService;
import edu.itc.shop.domain.OrderStatus;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * HATEOAS orders API supporting idempotent creation, retrieval, payment, and cancellation.
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Orders", description = "HATEOAS order lifecycle and idempotent order placement")
public class OrderResource {

    public record Stored(String bodyHash, Response.StatusType status, URI location, OrderDto body) {
    }

    private static final Response.StatusType PRECONDITION_REQUIRED =
            statusType(428, "Precondition Required");
    private static final Response.StatusType UNPROCESSABLE_CONTENT =
            statusType(422, "Unprocessable Content");

    private final OrderService orders;
    private final ConcurrentMap<String, Stored> idempotency;

    @Inject
    public OrderResource(OrderService orders, ConcurrentMap<String, Stored> idempotency) {
        this.orders = orders;
        this.idempotency = idempotency;
    }

    /**
     * Places a new order idempotently using {@code Idempotency-Key}, reserving product stock atomically.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Place an order",
            description = "Creates an order and reserves stock. Replays identical retries for the same Idempotency-Key.")
    @APIResponse(responseCode = "201", description = "Order created or replayed")
    @APIResponse(responseCode = "400", description = "Invalid order payload")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Referenced product not found")
    @APIResponse(responseCode = "409", description = "Insufficient stock for a requested line")
    @APIResponse(responseCode = "415", description = "Unsupported Content-Type")
    @APIResponse(responseCode = "422", description = "Idempotency-Key reused with a different request body")
    @APIResponse(responseCode = "428", description = "Missing Idempotency-Key header")
    public Response place(
            @Parameter(
                    name = "Idempotency-Key",
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Client-supplied key guaranteeing at-most-once order creation")
            @HeaderParam("Idempotency-Key") String key,
            @Valid @NotNull PlaceOrder cmd,
            @Context UriInfo uriInfo) {
        if (key == null || key.isBlank()) {
            throw new ClientErrorException(
                    "Idempotency-Key header required",
                    Response.status(PRECONDITION_REQUIRED).build());
        }

        String hash = Hashes.sha256(cmd);
        boolean[] replayed = {true};
        Stored stored = idempotency.computeIfAbsent(key, ignored -> {
            replayed[0] = false;
            List<Line> domainLines = cmd.lines().stream().map(LineDto::toLine).toList();
            Order order = orders.place(domainLines);
            URI location = uriInfo.getAbsolutePathBuilder().path(order.id()).build();
            return new Stored(hash, Response.Status.CREATED, location, represent(order, uriInfo));
        });

        if (!stored.bodyHash().equals(hash)) {
            throw new ClientErrorException(
                    "Idempotency-Key reused with a different body",
                    Response.status(UNPROCESSABLE_CONTENT).build());
        }

        OrderDto dto = stored.body();
        return Response.status(stored.status())
                .location(stored.location())
                .links(linkHeaders(dto))
                .header("Idempotency-Replayed", replayed[0])
                .entity(dto)
                .build();
    }

    /**
     * Retrieves an order representation with current HATEOAS hypermedia links.
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get order by ID", description = "Returns order details and available state transition links.")
    @APIResponse(responseCode = "200", description = "Order found")
    @APIResponse(responseCode = "404", description = "Order not found")
    @APIResponse(responseCode = "406", description = "Unsupported Accept media type")
    public Response get(
            @Parameter(description = "Order identifier", required = true)
            @PathParam("id") String id,
            @Context UriInfo uriInfo) {
        OrderDto dto = represent(orders.get(id), uriInfo);
        return Response.ok(dto)
                .links(linkHeaders(dto))
                .build();
    }

    /**
     * Pays a {@code NEW} order, transitioning its status to {@code PAID}.
     */
    @PUT
    @Path("/{id}/payment")
    @Operation(summary = "Pay an order", description = "Transitions a NEW order to PAID.")
    @APIResponse(responseCode = "200", description = "Order paid")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Order not found")
    @APIResponse(responseCode = "409", description = "Order is not in NEW status")
    public Response pay(
            @Parameter(description = "Order identifier", required = true)
            @PathParam("id") String id,
            @Context UriInfo uriInfo) {
        OrderDto dto = represent(orders.pay(id), uriInfo);
        return Response.ok(dto)
                .links(linkHeaders(dto))
                .build();
    }

    /**
     * Cancels a {@code NEW} order and restores reserved product stock.
     */
    @DELETE
    @Path("/{id}")
    @Operation(summary = "Cancel an order", description = "Transitions a NEW order to CANCELLED and restores stock.")
    @APIResponse(responseCode = "200", description = "Order cancelled and stock restored")
    @APIResponse(responseCode = "401", description = "Missing or invalid API key")
    @APIResponse(responseCode = "404", description = "Order not found")
    @APIResponse(responseCode = "409", description = "Order is already PAID or CANCELLED")
    public Response cancel(
            @Parameter(description = "Order identifier", required = true)
            @PathParam("id") String id,
            @Context UriInfo uriInfo) {
        OrderDto dto = represent(orders.cancel(id), uriInfo);
        return Response.ok(dto)
                .links(linkHeaders(dto))
                .build();
    }

    private static OrderDto represent(Order order, UriInfo uriInfo) {
        String selfUri = uriInfo.getBaseUriBuilder()
                .path(OrderResource.class)
                .path(order.id())
                .build()
                .toString();

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", selfUri);
        if (order.status() == OrderStatus.NEW) {
            String payUri = uriInfo.getBaseUriBuilder()
                    .path(OrderResource.class)
                    .path(order.id())
                    .path("payment")
                    .build()
                    .toString();
            links.put("pay", payUri);
            links.put("cancel", selfUri);
        }
        for (Line line : order.lines()) {
            String productUri = uriInfo.getBaseUriBuilder()
                    .path(ProductResource.class)
                    .path(Long.toString(line.productId()))
                    .build()
                    .toString();
            links.putIfAbsent("product-" + line.productId(), productUri);
        }

        List<LineDto> lines = order.lines().stream().map(LineDto::from).toList();
        return new OrderDto(order.id(), order.status(), order.total(), lines, links);
    }

    private static Link[] linkHeaders(OrderDto dto) {
        return dto._links().entrySet().stream()
                .map(entry -> Link.fromUri(entry.getValue()).rel(entry.getKey()).build())
                .toArray(Link[]::new);
    }

    private static Response.StatusType statusType(int code, String reason) {
        return new Response.StatusType() {
            @Override
            public int getStatusCode() {
                return code;
            }

            @Override
            public Response.Status.Family getFamily() {
                return Response.Status.Family.CLIENT_ERROR;
            }

            @Override
            public String getReasonPhrase() {
                return reason;
            }
        };
    }
}
