package edu.itc.se.shop.domain;

import java.math.BigDecimal;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Product.class)
public abstract class Product_ {
    public static volatile SingularAttribute<Product, Long> id;
    public static volatile SingularAttribute<Product, String> sku;
    public static volatile SingularAttribute<Product, String> name;
    public static volatile SingularAttribute<Product, Money> price;
    public static volatile SingularAttribute<Product, Category> category;
    public static volatile SingularAttribute<Product, Integer> stock;
    public static volatile SingularAttribute<Product, Long> version;
    public static volatile SetAttribute<Product, Tag> tags;
    public static volatile EntityType<Product> class_;
}
