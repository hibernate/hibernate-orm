/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.serialization.entity;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;

@Embeddable
public class BuildRecordId implements Serializable {
	private long id;

	public BuildRecordId() {
	}

	public BuildRecordId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		BuildRecordId longId = (BuildRecordId) o;
		return id == longId.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

}
