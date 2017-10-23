/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.basic;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@Table(name="contact")
public class Contact implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private Set<EmailAddress> emailAddresses = new HashSet<EmailAddress>();
    private Set<EmailAddress> emailAddresses2 = new HashSet<EmailAddress>();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Basic
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ElementCollection
    @CollectionTable(name = "user_email_addresses2", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
    public Set<EmailAddress> getEmailAddresses2() {
        return emailAddresses2;
    }

    public void setEmailAddresses2(Set<EmailAddress> emailAddresses2) {
        this.emailAddresses2 = emailAddresses2;
    }

    @ElementCollection
    @CollectionTable(name = "user_email_addresses", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
    public Set<EmailAddress> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(Set<EmailAddress> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Contact)) {
            return false;
        }
        final Contact other = (Contact) obj;
        if (this.id == null || other.id == null) {
            return this == obj;
        }
        if(!this.id.equals(other.id)) {
            return this == obj;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.clevercure.web.hibernateissuecache.User[ id=" + id + " ]";
    }
}
