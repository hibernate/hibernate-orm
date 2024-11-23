/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Identifies a named association belonging to a particular
 * entity instance. Used to record the fact that an association
 * is null during loading.
 *
 * @author Gavin King
 */
public final class AssociationKey implements Serializable {
	private final EntityKey ownerKey;
	private final String propertyName;

	/**
	 * Constructs an AssociationKey
	 *
	 * @param ownerKey The EntityKey of the association owner
	 * @param propertyName The name of the property on the owner which defines the association
	 */
	public AssociationKey(EntityKey ownerKey, String propertyName) {
		this.ownerKey = ownerKey;
		this.propertyName = propertyName;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final AssociationKey that = (AssociationKey) o;
		return ownerKey.equals( that.ownerKey )
				&& propertyName.equals( that.propertyName );
	}

	@Override
	public int hashCode() {
		int result = ownerKey.hashCode();
		result = 31 * result + propertyName.hashCode();
		return result;
	}
}
