package edu.itc.se.shop.domain;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Tag.class)
public abstract class Tag_ {
    public static volatile SingularAttribute<Tag, Long> id;
    public static volatile SingularAttribute<Tag, String> name;
    public static volatile SetAttribute<Tag, Product> products;
    public static volatile EntityType<Tag> class_;
}
