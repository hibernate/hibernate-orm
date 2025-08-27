/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author Gavin King
 * @author Steve Ebersole
 */
@Entity
public class Email {
	private Long id;
	private String address;

	Email() {
	}

	public Email(String address) {
		this.address = address;
	}

	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String type) {
		this.address = type;
	}

	@Override
	public boolean equals(Object that) {
		if ( !(that instanceof Email ) ) return false;
		Email p = (Email) that;
		return this.address.equals(p.address);
	}

	@Override
	public int hashCode() {
		return address.hashCode();
	}

}
