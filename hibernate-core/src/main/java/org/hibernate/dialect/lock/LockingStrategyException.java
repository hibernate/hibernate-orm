/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
