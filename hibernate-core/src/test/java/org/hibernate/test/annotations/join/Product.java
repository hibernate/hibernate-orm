package org.hibernate.test.annotations.join;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Product {

    @Id
    @GeneratedValue
    @Column(name = "id_product")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "id_product_version", nullable = false)
    private ProductVersion productVersion;

    @Column(name = "code", unique = true, nullable = false, precision = 4, scale = 0)
    private Long code;

    public Integer getId() {
        return this.id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public ProductVersion getProductVersion() {
        return this.productVersion;
    }

    public void setProductVersion(final ProductVersion productVersion) {
        this.productVersion = productVersion;
    }

    public Long getCode() {
        return this.code;
    }

    public void setCode(final Long code) {
        this.code = code;
    }
}
