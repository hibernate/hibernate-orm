/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
