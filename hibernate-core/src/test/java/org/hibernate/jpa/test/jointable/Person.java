/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.jointable;

import javax.persistence.*;
import java.util.Set;

/**
 *
 * @author Christian Beikov
 */
@Entity
public class Person {
	private Long id;
	private Address address;
	private Set<Address> addresses;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinTable( name = "SOME_OTHER_TABLE" )
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@OneToMany
	@JoinTable( name = "SOME_OTHER_TABLE2" )
	public Set<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Set<Address> addresses) {
		this.addresses = addresses;
	}
}
