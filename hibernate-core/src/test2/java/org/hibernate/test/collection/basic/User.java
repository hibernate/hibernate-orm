/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.basic;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private Contact contact;

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

    @ManyToOne(optional = true)
    @JoinColumn(name = "contact_id", nullable = true, unique = true)
    public Contact getContact() {
            return contact;
    }

    public void setContact(Contact contact) {
            this.contact = contact;
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
        if (!(obj instanceof User)) {
            return false;
        }
        final User other = (User) obj;
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
