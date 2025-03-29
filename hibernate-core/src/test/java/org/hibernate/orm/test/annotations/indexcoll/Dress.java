/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dress {
	private Integer id;

	@Id
	@GeneratedValue
	@Column(name = "dress_id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Dress ) ) return false;

		final Dress dress = (Dress) o;

		if ( !getId().equals( dress.getId() ) ) return false;

		return true;
	}

	public int hashCode() {
		return getId().hashCode();
	}
}
