package org.hibernate.test.annotations.join;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "product_version")
public class ProductVersion {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id_product_version", unique = true, nullable = false)
    private Integer id;
    
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "productVersion", cascade = {CascadeType.ALL})
    private List<Product> products = new ArrayList<Product>(0);

    public Integer getId() {
        return this.id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public List<Product> getProducts() {
        return this.products;
    }

    public void setProducts(final ArrayList<Product> products) {
        this.products = products;
    }

}
