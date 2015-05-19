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
 */
public interface NaturalIdLoadAccess<T> {
	/**
	 * Specify the {@link LockOptions} to use when retrieving the entity.
	 *
	 * @param lockOptions The lock options to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	public NaturalIdLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Add a NaturalId attribute value.
	 * 
	 * @param attributeName The entity attribute name that is marked as a NaturalId
	 * @param value The value of the attribute
	 *
	 * @return {@code this}, for method chaining
	 */
	public NaturalIdLoadAccess<T> using(String attributeName, Object value);

	/**
	 * For entities with mutable natural ids, should Hibernate perform "synchronization" prior to performing
	 * lookups?  The default is to perform "synchronization" (for correctness).
	 * <p/>
	 * "synchronization" here indicates updating the natural-id -> pk cross reference maintained as part of the
	 * session.  When enabled, prior to performing the lookup, Hibernate will check all entities of the given
	 * type associated with the session to see if its natural-id values have changed and, if so, update the
	 * cross reference.  There is a performance impact associated with this, so if application developers are
	 * certain the natural-ids in play have not changed, this setting can be disabled to circumvent that impact.
	 * However, disabling this setting when natural-ids values have changed can result in incorrect results!
	 *
	 * @param enabled Should synchronization be performed?  {@code true} indicates synchronization will be performed;
	 * {@code false} indicates it will be circumvented.
	 *
	 * @return {@code this}, for method chaining
	 */
	public NaturalIdLoadAccess<T> setSynchronizationEnabled(boolean enabled);

	/**
	 * Return the persistent instance with the natural id value(s) defined by the call(s) to {@link #using}.  This
	 * method might return a proxied instance that is initialized on-demand, when a non-identifier method is accessed.
	 *
	 * You should not use this method to determine if an instance exists; to check for existence, use {@link #load}
	 * instead.  Use this only to retrieve an instance that you assume exists, where non-existence would be an
	 * actual error.
	 *
	 * @return the persistent instance or proxy
	 */
	public T getReference();

	/**
	 * Return the persistent instance with the natural id value(s) defined by the call(s) to {@link #using}, or
	 * {@code null} if there is no such persistent instance.  If the instance is already associated with the session,
	 * return that instance, initializing it if needed.  This method never returns an uninitialized instance.
	 *
	 * @return The persistent instance or {@code null} 
	 */
	public T load();

}
