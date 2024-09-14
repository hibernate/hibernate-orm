/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.fetchprofile;

import org.hibernate.annotations.FetchProfile;
import org.hibernate.orm.test.annotations.fetchprofile.mappedby.Address;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
@FetchProfile(name = "customer-with-address", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer6.class, association = "address")
})
public class Customer6 {

	@Id
	@GeneratedValue
	private long id;

	private String name;

	@OneToOne(fetch = FetchType.LAZY)
	private Address address;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

}
