/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.dialect.lock.OptimisticEntityLockException;

/**
 * Throw when an optimistic locking conflict occurs.
 *
 * @author Scott Marlow
 *
 * @deprecated Use {@link org.hibernate.dialect.lock.OptimisticEntityLockException} instead
 */
@Deprecated
public class OptimisticLockException extends OptimisticEntityLockException {
	/**
	 * Constructs a OptimisticLockException using the specified information.
	 *
	 * @param entity The entity instance that could not be locked
	 * @param message A message explaining the exception condition
	 */
	public OptimisticLockException(Object entity, String message) {
		super( entity, message );
	}
}
