/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.basic;

import java.io.Serializable;
import javax.persistence.*;

@Embeddable
public class EmailAddress implements Serializable {

    private static final long serialVersionUID = 1L;
    private String email;

    public EmailAddress() {
    }

    public EmailAddress(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (email != null ? email.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EmailAddress)) {
            return false;
        }
        final EmailAddress other = (EmailAddress) obj;
        if (this.email == null || other.email == null) {
            return this == obj;
        }
        if(!this.email.equals(other.email)) {
            return this == obj;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.clevercure.web.hibernateissuecache.EmailAddress[ email=" + email + " ]";
    }
}
