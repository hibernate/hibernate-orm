/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine;

import org.hibernate.annotations.OptimisticLockType;

/**
 * Describes how an entity should be optimistically locked.
 *
 * @apiNote  This enumeration is mainly for internal use, since it
 *           is isomorphic to {@link OptimisticLockType}. In the
 *           future, it would be nice to replace them both with a
 *           new {@code org.hibernate.OptimisticLockCheck} enum.
 *
 * @author Steve Ebersole
 */
public enum OptimisticLockStyle {
	/**
	 * No optimistic locking.
	 */
	NONE,
	/**
	 * Optimistic locking via a dedicated version or timestamp column.
	 */
	VERSION,
	/**
	 * Optimistic locking via comparison of dirty columns.
	 */
	DIRTY,
	/**
	 * Optimistic locking via comparison of all columns.
	 */
	ALL;

	public static OptimisticLockStyle fromLockType(OptimisticLockType type) {
		return switch ( type ) {
			case VERSION -> VERSION;
			case NONE -> NONE;
			case DIRTY -> DIRTY;
			case ALL -> ALL;
		};
	}

	public boolean isAllOrDirty() {
		return isAll() || isDirty();
	}

	public boolean isAll() {
		return this == ALL;
	}

	public boolean isDirty() {
		return this == DIRTY;
	}

	public boolean isVersion() {
		return this == VERSION;
	}

	public boolean isNone() {
		return this == NONE;
	}
}
