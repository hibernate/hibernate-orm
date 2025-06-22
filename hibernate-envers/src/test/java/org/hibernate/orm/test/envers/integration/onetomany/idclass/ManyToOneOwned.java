/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.idclass;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class ManyToOneOwned {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	public ManyToOneOwned() {

	}

	public ManyToOneOwned(String data) {
		this.data = data;
	}

	public ManyToOneOwned(Long id, String data) {
		this.id = id;
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof ManyToOneOwned ) ) {
			return false;
		}

		ManyToOneOwned that = (ManyToOneOwned) o;
		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 3;
		result = 11 * result + ( data != null ? data.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "ManyToOneOwned(id = " + id + ", data = " + data + ")";
	}

	public Long getId() {
		return id;
	}

	public String getData() {
		return data;
	}
}
