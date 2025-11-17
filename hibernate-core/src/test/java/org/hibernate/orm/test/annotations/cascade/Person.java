/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PERSON")
class Person implements Serializable {

	private static final long serialVersionUID = 960914272100925312L;

	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Long id;

	@Column(name = "NAME", nullable = false, unique = true, updatable = false, length = 256)
	private String name;

	Person() {
		super();
	}

	Person(final String pName) {
		super();
		this.name = pName;
	}

	Long getId() {
		return this.id;
	}

	String getName() {
		return this.name;
	}

	@Override
	public int hashCode() {
		final int prime = 103;
		int result = 1;
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object pObject) {
		if (this == pObject) {
			return true;
		}
		if (pObject == null) {
			return false;
		}
		if (!(pObject instanceof Person)) {
			return false;
		}
		final Person other = (Person) pObject;
		if (getName() == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!getName().equals(other.getName())) {
			return false;
		}
		return true;
	}

}
