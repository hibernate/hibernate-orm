/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.cid;

import jakarta.persistence.Embeddable;

/**
 * @author Donnchadh O Donnabhain
 */
@Embeddable
public class AccountId implements java.io.Serializable {
	private final int id;

	protected AccountId() {
		this.id = 0;
	}

	public AccountId(int id) {
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
		AccountId other = (AccountId) obj;
		if (other != null && id != other.id)
			return false;
		return true;
	}
}
