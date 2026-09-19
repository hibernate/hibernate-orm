/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

import jakarta.annotation.Nonnull;

import java.util.Objects;

import org.hibernate.type.Type;

/**
 * Useful metadata representing a unique key within a Persister
 */
public final class UniqueKeyEntry {

	private final String uniqueKeyName;
	private final int stateArrayPosition;
	private final Type propertyType;

	public UniqueKeyEntry(@Nonnull final String uniqueKeyName, final int stateArrayPosition, @Nonnull final Type propertyType) {
		this.uniqueKeyName = Objects.requireNonNull( uniqueKeyName );
		this.stateArrayPosition = stateArrayPosition;
		this.propertyType = Objects.requireNonNull( propertyType );
	}

	@Nonnull
	public String getUniqueKeyName() {
		return this.uniqueKeyName;
	}

	public int getStateArrayPosition() {
		return this.stateArrayPosition;
	}

	@Nonnull
	public Type getPropertyType() {
		return this.propertyType;
	}

}
