/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hashcode;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class HashEntity {
	private long id;

	@Id
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Boolean goo() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		HashEntity that = (HashEntity) o;

		if ( id != that.id ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return (int) ( id ^ ( id >>> 32 ) );
	}
}
