/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
