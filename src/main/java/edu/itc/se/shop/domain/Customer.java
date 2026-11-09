
package edu.itc.se.shop.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq_gen")
    @SequenceGenerator(
        name = "customer_seq_gen",
        sequenceName = "customer_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Embedded
    private Address billing;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(
            name = "street",
            column = @Column(name = "ship_street")
        ),
        @AttributeOverride(
            name = "city",
            column = @Column(name = "ship_city")
        ),
        @AttributeOverride(
            name = "zip",
            column = @Column(name = "ship_zip")
        )
    })
    private Address shipping;

    protected Customer() {
    }

    public Customer(String name, String email, Address billing, Address shipping) {
        this.name = name;
        this.email = email;
        this.billing = billing;
        this.shipping = shipping;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Address getBilling() {
        return billing;
    }

    public Address getShipping() {
        return shipping;
    }
}
