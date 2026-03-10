/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a unique constraint "slot" that can hold at most one value.
 * When DELETE and INSERT target the same slot, DELETE must be ordered first
 * to avoid unique constraint violations.
 *
 * @param tableName The table containing the constraint
 * @param constraintName The constraint name
 * @param keyValues The actual values forming the unique key (null means not yet extracted)
 *
 * @author Steve Ebersole
 */
public record UniqueSlot(
		String tableName,
		String constraintName,
		Object[] keyValues) implements Serializable {

	/**
	 * Check if this slot conflicts with another (same table, constraint, and values)
	 */
	public boolean conflictsWith(UniqueSlot other) {
		if (!tableName.equals(other.tableName)) {
			return false;
		}
		if (!constraintName.equals(other.constraintName)) {
			return false;
		}
		// Both must have values to conflict
		if (keyValues == null || other.keyValues == null) {
			return false;
		}
		return Arrays.equals(keyValues, other.keyValues);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UniqueSlot that = (UniqueSlot) o;
		return Objects.equals(tableName, that.tableName) &&
				Objects.equals(constraintName, that.constraintName) &&
				Arrays.equals(keyValues, that.keyValues);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tableName, constraintName, Arrays.hashCode(keyValues));
	}

	@Override
	public String toString() {
		return "UniqueSlot{" +
				"table='" + tableName + '\'' +
				", constraint='" + constraintName + '\'' +
				", values=" + Arrays.toString(keyValues) +
				'}';
	}
}
