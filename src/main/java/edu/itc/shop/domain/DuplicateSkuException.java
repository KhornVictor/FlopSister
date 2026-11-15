package edu.itc.shop.domain;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class DuplicateSkuException extends ClientErrorException {

    public DuplicateSkuException(String sku) {
        super("Product with SKU '" + sku + "' already exists", Response.Status.CONFLICT);
    }

    public DuplicateSkuException() {
        super(Response.Status.CONFLICT);
    }
}
