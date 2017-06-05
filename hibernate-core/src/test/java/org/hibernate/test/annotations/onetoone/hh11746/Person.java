/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone.hh11746;

import javax.persistence.*;

/**
 * @author Jonathan Ihm
 */
@Entity(name = "Person")
public class Person {

    @Column(name = "id")
    @EmbeddedId
    private PersonId id;

    @OneToOne(cascade = CascadeType.ALL)
    @MapsId("id")
    private Address address;

    public PersonId getId() {
        return id;
    }

    public void setId(PersonId id) {
        this.id = id;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}