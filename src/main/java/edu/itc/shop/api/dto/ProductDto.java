package edu.itc.shop.api.dto;

import java.math.BigDecimal;

import edu.itc.shop.domain.Product;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

// One class serves JSON (JSON-B) and XML (JAXB). JAXB needs a class with a
// no-arg constructor; records are JSON-only, which is why this is not a record.
@XmlRootElement(name = "product")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductDto {

    @XmlAttribute
    public long id;
    public String name;
    public String sku;
    public BigDecimal price;
    public int stock;

    public ProductDto() {
    }

    public ProductDto(long id, String name, String sku, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.price = price;
        this.stock = stock;
    }

    public static ProductDto from(Product p) {
        var d = new ProductDto();
        d.id = p.id();
        d.name = p.name();
        d.sku = p.sku();
        d.price = p.price();
        d.stock = p.stock();
        return d;
    }

    public Product toProduct(long id, int version) {
        return new Product(id, name, sku, price, stock, version);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
