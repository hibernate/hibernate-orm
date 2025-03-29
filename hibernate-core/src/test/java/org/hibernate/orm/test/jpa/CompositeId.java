/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.io.Serializable;
import jakarta.persistence.Embeddable;

/**
 *
 */
@Embeddable
public class CompositeId implements Serializable {

	private int id1;
	private int id2;

	public CompositeId() {
	}

	public CompositeId(int id1, int id2) {
		this.id1 = id1;
		this.id2 = id2;
	}

	public int getId1() {
		return id1;
	}

	public void setId1( int id1 ) {
		this.id1 = id1;
	}

	public int getId2() {
		return id2;
	}

	public void setId2( int id2 ) {
		this.id2 = id2;
	}

	@Override
	public boolean equals( Object obj ) {
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final CompositeId other = (CompositeId)obj;
		if (this.id1 != other.id1) return false;
		if (this.id2 != other.id2) return false;
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 73 * hash + this.id1;
		hash = 73 * hash + this.id2;
		return hash;
	}
}
