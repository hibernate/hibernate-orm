package org.hibernate.test.cache.infinispan.functional.entities;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Test class with incorrectly defined equals and hashCode.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
@Embeddable
public class Name implements Serializable {
    String firstName;
    String lastName;

    public Name() {}

    public Name(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}
