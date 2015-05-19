/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */package org.hibernate.test.bytecode.enhancement.entity.customer;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Embeddable
public class Address implements Serializable {
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String country;
    private String zip;
    private String phone;

    public Address() {
    }
    public Address(String street1, String street2, String city, String state,
                   String country, String zip, String phone) {
        this.street1 = street1;
        this.street2 = street2;
        this.city    = city;
        this.state   = state;
        this.country = country;
        setZip(zip);
        setPhone(phone);
    }

    public String toString() {
        return street1 + "\n" + street2 + "\n" + city + "," + state + " " + zip + "\n" + phone;
    }

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        assertNumeric(zip, "Non-numeric zip ");
        this.zip = zip;

    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        assertNumeric(zip, "Non-numeric phone ");
        this.phone = phone;
    }

    void assertNumeric(String s, String error) {
        for (int i=0; i<s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                throw new IllegalArgumentException(error + s);
            }
        }
    }
}
