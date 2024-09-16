/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.JDBCException;

/**
 * Represents an error trying to apply a pessimistic {@link LockingStrategy} to an entity
 *
 * @author Steve Ebersole
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
