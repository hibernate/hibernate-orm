/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import jakarta.persistence.LockModeType;

import org.hibernate.LockMode;

/**
 * Helper to deal with conversions (both directions) between {@link LockMode} and
 * {@link jakarta.persistence.LockModeType}.
 *
 * @author Steve Ebersole
 */
public final class LockModeConverter {
	private LockModeConverter() {
	}

	/**
	 * Convert from the Hibernate-specific {@link LockMode} to the JPA defined {@link LockModeType}.
	 *
	 * @param lockMode The Hibernate {@link LockMode}.
	 * @return The JPA {@link LockModeType}
	 */
	public static LockModeType convertToLockModeType(LockMode lockMode) {
		return switch ( lockMode ) {
			case NONE, READ -> LockModeType.NONE; // no exact equivalent in JPA
			case OPTIMISTIC -> LockModeType.OPTIMISTIC;
			case OPTIMISTIC_FORCE_INCREMENT -> LockModeType.OPTIMISTIC_FORCE_INCREMENT;
			case PESSIMISTIC_READ -> LockModeType.PESSIMISTIC_READ;
			case PESSIMISTIC_WRITE, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED -> LockModeType.PESSIMISTIC_WRITE; // no exact equivalent in JPA
			case WRITE, PESSIMISTIC_FORCE_INCREMENT -> LockModeType.PESSIMISTIC_FORCE_INCREMENT;
		};
	}


	/**
	 * Convert from JPA defined {@link LockModeType} to Hibernate-specific {@link LockMode}.
	 *
	 * @param lockModeType The JPA {@link LockModeType}
	 * @return The Hibernate {@link LockMode}.
	 */
	public static LockMode convertToLockMode(LockModeType lockModeType) {
		return switch ( lockModeType ) {
			case NONE -> LockMode.NONE;
			case READ, OPTIMISTIC -> LockMode.OPTIMISTIC;
			case WRITE, OPTIMISTIC_FORCE_INCREMENT -> LockMode.OPTIMISTIC_FORCE_INCREMENT;
			case PESSIMISTIC_READ -> LockMode.PESSIMISTIC_READ;
			case PESSIMISTIC_WRITE -> LockMode.PESSIMISTIC_WRITE;
			case PESSIMISTIC_FORCE_INCREMENT -> LockMode.PESSIMISTIC_FORCE_INCREMENT;
		};
	}
}
