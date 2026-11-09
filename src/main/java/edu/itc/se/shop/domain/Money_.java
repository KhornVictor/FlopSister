package edu.itc.se.shop.domain;

import java.math.BigDecimal;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(Money.class)
public abstract class Money_ {
    public static volatile SingularAttribute<Money, BigDecimal> amount;
    public static volatile SingularAttribute<Money, String> currency;
    public static volatile EmbeddableType<Money> class_;
}
