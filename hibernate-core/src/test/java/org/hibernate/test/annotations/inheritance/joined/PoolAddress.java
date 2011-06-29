package org.hibernate.test.annotations.inheritance.joined;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class PoolAddress {
    @Column(table = "POOL_ADDRESS")
    private String address;
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
