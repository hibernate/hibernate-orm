/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid.cid;

import jakarta.persistence.Embeddable;

/**
 * @author Donnchadh O Donnabhain
 */

@Embeddable
public class AId implements java.io.Serializable {
	private final int id;

	protected AId() {
		this.id = 0;
	}

	public AId(int id) {
		this.id = id;
	}
	public int intValue() {
		return id;
	}
	@Override
	public int hashCode() {
		return id;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AId other = (AId) obj;
		if (other != null && id != other.id)
			return false;
		return true;
	}
}
