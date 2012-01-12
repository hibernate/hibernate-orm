/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.io.Serializable;

/**
 * Loads an entity by its primary identifier
 * 
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public interface IdentifierLoadAccess {
	/**
	 * Specify the {@link LockOptions} to use when retrieving the entity.
	 *
	 * @param lockOptions The lock options to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public IdentifierLoadAccess with(LockOptions lockOptions);

	/**
	 * Return the persistent instance with the given identifier, assuming that the instance exists. This method
	 * might return a proxied instance that is initialized on-demand, when a non-identifier method is accessed.
	 *
	 * You should not use this method to determine if an instance exists; to check for existence, use {@link #load}
	 * instead.  Use this only to retrieve an instance that you assume exists, where non-existence would be an
	 * actual error.
	 *
	 * @param id The identifier for which to obtain a reference
	 *
	 * @return the persistent instance or proxy
	 */
	public Object getReference(Serializable id);

	/**
	 * Return the persistent instance with the given identifier, or null if there is no such persistent instance.
	 * If the instance is already associated with the session, return that instance, initializing it if needed.  This
	 * method never returns an uninitialized instance.
	 *
	 * @param id The identifier
	 *
	 * @return The persistent instance or {@code null}
	 */
	public Object load(Serializable id);
}
