package org.hibernate.jpamodelgen.test.collectiontype;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author helloztt
 */
@Entity
public class Product {
    private int proId;
    private String proName;

    @Id
    public int getProId() {
        return proId;
    }

    public void setProId(int proId) {
        this.proId = proId;
    }

    public String getProName() {
        return proName;
    }

    public void setProName(String proName) {
        this.proName = proName;
    }
}
