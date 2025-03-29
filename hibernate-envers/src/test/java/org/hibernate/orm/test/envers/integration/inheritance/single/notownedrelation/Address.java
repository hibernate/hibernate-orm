/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single.notownedrelation;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class Address implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String address1;

	@ManyToOne
	private Contact contact;

	public Address() {
	}

	public Address(Long id, String address1) {
		this.id = id;
		this.address1 = address1;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	public Contact getContact() {
		return contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Address) ) {
			return false;
		}

		Address address = (Address) o;

		if ( address1 != null ? !address1.equals( address.address1 ) : address.address1 != null ) {
			return false;
		}
		if ( id != null ? !id.equals( address.id ) : address.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (address1 != null ? address1.hashCode() : 0);
		return result;
	}
}
