/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public interface SimpleNaturalIdLoadAccess<T> {
	/**
	 * Specify the {@link org.hibernate.LockOptions} to use when retrieving the entity.
	 *
	 * @param lockOptions The lock options to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public SimpleNaturalIdLoadAccess<T> with(LockOptions lockOptions);

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
	public SimpleNaturalIdLoadAccess<T> setSynchronizationEnabled(boolean enabled);

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
	public T getReference(Object naturalIdValue);

	/**
	 * Return the persistent instance with the given natural id value, or {@code null} if there is no such persistent
	 * instance.  If the instance is already associated with the session, return that instance, initializing it if
	 * needed.  This method never returns an uninitialized instance.
	 *
	 * @param naturalIdValue The value of the natural-id for the entity to retrieve
	 * 
	 * @return The persistent instance or {@code null}
	 */
	public T load(Object naturalIdValue);

}
