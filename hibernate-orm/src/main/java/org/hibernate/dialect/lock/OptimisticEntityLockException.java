/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

/**
 * Represents an error trying to apply an optimistic {@link LockingStrategy} to an entity
 *
 * @author Steve Ebersole
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
