/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
