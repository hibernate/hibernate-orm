/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

/**
 * Represents an error trying to apply an optimistic {@link LockingStrategy} to an entity
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.OptimisticLockException
 */
public class OptimisticEntityLockException extends LockingStrategyException {
	/**
	 * Constructs a OptimisticEntityLockException
	 *
	 * @param entity The entity we were trying to lock
	 * @param message Message explaining the condition
	 */
	public OptimisticEntityLockException(Object entity, String message) {
		super( entity, message );
	}

	/**
	 * Constructs a OptimisticEntityLockException
	 *
	 * @param entity The entity we were trying to lock
	 * @param message Message explaining the condition
	 * @param cause The underlying cause
	 */
	public OptimisticEntityLockException(Object entity, String message, Throwable cause) {
		super( entity, message, cause );
	}
}
