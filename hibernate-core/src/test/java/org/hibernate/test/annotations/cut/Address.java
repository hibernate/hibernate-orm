//$Id: Address.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
package org.hibernate.test.annotations.cut;

import java.io.Serializable;

public class Address implements Serializable {
	private static final long serialVersionUID = 1L;

	String address1;

    String city;
    
    public String getAddress1() {
        return address1;
    }

    public String getCity() {
        return city;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
