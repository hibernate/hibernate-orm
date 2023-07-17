/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.util.Objects;

import org.hibernate.type.Type;

/**
 * Useful metadata representing a unique key within a Persister
 */
public final class UniqueKeyEntry {

	private final String uniqueKeyName;
	private final int stateArrayPosition;
	private final Type propertyType;

	public UniqueKeyEntry(final String uniqueKeyName, final int stateArrayPosition, final Type propertyType) {
		this.uniqueKeyName = Objects.requireNonNull( uniqueKeyName );
		this.stateArrayPosition = stateArrayPosition;
		this.propertyType = Objects.requireNonNull( propertyType );
	}

	public String getUniqueKeyName() {
		return this.uniqueKeyName;
	}

	public int getStateArrayPosition() {
		return this.stateArrayPosition;
	}

	public Type getPropertyType() {
		return this.propertyType;
	}

}
