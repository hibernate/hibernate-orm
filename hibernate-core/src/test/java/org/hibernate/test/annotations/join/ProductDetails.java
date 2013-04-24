package org.hibernate.test.annotations.join;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "product_details")
public class ProductDetails {
    @Id
    @GeneratedValue
    @Column(name = "id_product_details")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumns({ @JoinColumn(name = "id_product", referencedColumnName = "id_product", nullable = false),
            @JoinColumn(name = "id_product_version", referencedColumnName = "id_product_version", nullable = false) })
    private Product product;

    public Integer getId() {
        return this.id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Product getProduct() {
        return this.product;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }

}
