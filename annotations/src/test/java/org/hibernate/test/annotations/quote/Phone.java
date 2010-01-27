package org.hibernate.test.annotations.quote;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "PHONE")
public class Phone implements java.io.Serializable {

    private Integer id;
    private String brandName;
    private float price;

    public Phone() {
    }

	@Id
    @Column(name="ID")
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    @Column(name="BRANDNAME")
    public String getBrandName() {
        return brandName;
    }
    public void setBrandName(String bName) {
        this.brandName = bName;
    }

    @Column(name="PRICE")
    public float getPrice() {
        return price;
    }
    public void setPrice(float price) {
        this.price = price;
    }
}
