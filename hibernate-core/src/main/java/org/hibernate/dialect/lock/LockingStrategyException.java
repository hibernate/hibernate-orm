/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;

/**
 * Represents an error trying to apply a {@link LockingStrategy} to an entity
 *
 * @author Steve Ebersole
 */
public abstract class LockingStrategyException extends HibernateException {
	private final Object entity;

	/**
	 * Constructs a LockingStrategyException
	 *
	 * @param entity The entity we were trying to lock
	 * @param message Message explaining the condition
	 */
	public LockingStrategyException(Object entity, String message) {
		super( message );
		this.entity = entity;
	}

	/**
	 * Constructs a LockingStrategyException
	 *
	 * @param entity The entity we were trying to lock
	 * @param message Message explaining the condition
	 * @param cause The underlying cause
	 */
	public LockingStrategyException(Object entity, String message, Throwable cause) {
		super( message, cause );
		this.entity = entity;
	}

	public Object getEntity() {
		return entity;
	}
}
