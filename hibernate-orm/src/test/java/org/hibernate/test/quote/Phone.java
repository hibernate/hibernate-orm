/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.quote;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
