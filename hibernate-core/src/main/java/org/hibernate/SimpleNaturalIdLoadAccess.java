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

/**
 * Loads an entity by its natural identifier.
 * 
 * @author Eric Dalquist
 * @author Steve Ebersole
 *
 * @see org.hibernate.annotations.NaturalId
 * @see NaturalIdLoadAccess
 */
public interface SimpleNaturalIdLoadAccess {
	/**
	 * Specify the {@link org.hibernate.LockOptions} to use when retrieving the entity.
	 *
	 * @param lockOptions The lock options to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SimpleNaturalIdLoadAccess with(LockOptions lockOptions);

	/**
	 * For entities with mutable natural ids, should Hibernate perform "synchronization" prior to performing 
	 * lookups?  The default is to perform "synchronization" (for correctness).
	 * <p/>
	 * See {@link NaturalIdLoadAccess#setSynchronizationEnabled} for detailed discussion.
	 *
	 * @param enabled Should synchronization be performed?  {@code true} indicates synchronization will be performed;
	 * {@code false} indicates it will be circumvented.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SimpleNaturalIdLoadAccess setSynchronizationEnabled(boolean enabled);

	/**
	 * Return the persistent instance with the given natural id value, assuming that the instance exists. This method
	 * might return a proxied instance that is initialized on-demand, when a non-identifier method is accessed.
	 *
	 * You should not use this method to determine if an instance exists; to check for existence, use {@link #load}
	 * instead.  Use this only to retrieve an instance that you assume exists, where non-existence would be an
	 * actual error.
	 *
	 * @param naturalIdValue The value of the natural-id for the entity to retrieve
	 *
	 * @return The persistent instance or proxy, if an instance exists.  Otherwise, {@code null}.
	 */
	public Object getReference(Object naturalIdValue);

	/**
	 * Return the persistent instance with the given natural id value, or {@code null} if there is no such persistent
	 * instance.  If the instance is already associated with the session, return that instance, initializing it if
	 * needed.  This method never returns an uninitialized instance.
	 *
	 * @param naturalIdValue The value of the natural-id for the entity to retrieve
	 * 
	 * @return The persistent instance or {@code null}
	 */
	public Object load(Object naturalIdValue);

}
