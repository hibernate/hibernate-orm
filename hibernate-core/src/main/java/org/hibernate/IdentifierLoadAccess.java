/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.Optional;

import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;

/**
 * Loads an entity by its primary identifier.
 * 
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public interface IdentifierLoadAccess<T> {
	/**
	 * Specify the {@link LockOptions} to use when retrieving the entity.
	 *
	 * @param lockOptions The lock options to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	IdentifierLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Specify the {@link CacheMode} to use when retrieving the entity.
	 *
	 * @param cacheMode The CacheMode to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	IdentifierLoadAccess<T> with(CacheMode cacheMode);

	default IdentifierLoadAccess<T> with(RootGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	IdentifierLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic);

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
	T getReference(Serializable id);

	/**
	 * Return the persistent instance with the given identifier, or null if there is no such persistent instance.
	 * If the instance is already associated with the session, return that instance, initializing it if needed.  This
	 * method never returns an uninitialized instance.
	 *
	 * @param id The identifier
	 *
	 * @return The persistent instance or {@code null}
	 */
	T load(Serializable id);

	/**
	 * Same semantic as {@link #load} except that here {@link Optional} is returned to
	 * handle nullability.
	 *
	 * @param id The identifier
	 *
	 * @return The persistent instance, if one, wrapped in Optional
	 */
	Optional<T> loadOptional(Serializable id);
}
