/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.JDBCException;

/**
 * Represents an error trying to apply a pessimistic {@link LockingStrategy} to an entity
 *
 * @author Steve Ebersole
 *
 * @see jakarta.persistence.PessimisticLockException
 */
public class PessimisticEntityLockException extends LockingStrategyException {
	/**
	 * Constructs a PessimisticEntityLockException
	 *
	 * @param entity The entity we were trying to lock
	 * @param message Message explaining the condition
	 * @param cause The underlying cause
	 */
	public PessimisticEntityLockException(Object entity, String message, JDBCException cause) {
		super( entity, message, cause );
	}
}
