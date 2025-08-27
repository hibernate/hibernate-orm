/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.graph.GraphSemantic;

import java.util.Optional;

/**
 * Loads an entity by its natural identifier. This simplified API is
 * used when the entity has exactly one field or property annotated
 * {@link org.hibernate.annotations.NaturalId @NaturalId}. If an
 * entity has multiple attributes annotated {@code @NaturalId}, then
 * {@link NaturalIdLoadAccess} should be used instead.
 * <p>
 * <pre>
 * Book book = session.bySimpleNaturalId(Book.class).load(isbn);
 * </pre>
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 *
 * @see Session#bySimpleNaturalId(Class)
 * @see org.hibernate.annotations.NaturalId
 * @see NaturalIdLoadAccess
 */
public interface SimpleNaturalIdLoadAccess<T> {

	/**
	 * Specify the {@linkplain LockMode lock mode} to use when
	 * querying the database.
	 *
	 * @param lockMode The lock mode to apply
	 * @return {@code this}, for method chaining
	 */
	default SimpleNaturalIdLoadAccess<T> with(LockMode lockMode) {
		return with( lockMode, PessimisticLockScope.NORMAL );
	}

	/**
	 * Specify the {@linkplain LockMode lock mode} to use when
	 * querying the database.
	 *
	 * @param lockMode The lock mode to apply
	 *
	 * @return {@code this}, for method chaining
	 */
	SimpleNaturalIdLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope);

	/**
	 * Specify the {@linkplain Timeout timeout} to use when
	 * querying the database.
	 *
	 * @param timeout The timeout to apply to the database operation
	 *
	 * @return {@code this}, for method chaining
	 */
	SimpleNaturalIdLoadAccess<T> with(Timeout timeout);

	/**
	 * Specify the {@linkplain LockOptions lock options} to use when
	 * querying the database.
	 *
	 * @param lockOptions The lock options to use
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Use one of {@linkplain #with(LockMode)},
	 * {@linkplain #with(LockMode, PessimisticLockScope)}
	 * and/or {@linkplain #with(Timeout)} instead.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	SimpleNaturalIdLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Override the associations fetched by default by specifying
	 * the complete list of associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default SimpleNaturalIdLoadAccess<T> withFetchGraph(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.FETCH );
	}

	/**
	 * Augment the associations fetched by default by specifying a
	 * list of additional associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default SimpleNaturalIdLoadAccess<T> withLoadGraph(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * Customize the associations fetched by specifying an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph},
	 * and how it should be {@linkplain GraphSemantic interpreted}.
	 *
	 * @since 6.3
	 */
	SimpleNaturalIdLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic);

	/**
	 * Customize the associations fetched by specifying a
	 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}
	 * that should be enabled during this operation.
	 * <p>
	 * This allows the {@linkplain Session#isFetchProfileEnabled(String)
	 * session-level fetch profiles} to be temporarily overridden.
	 *
	 * @since 6.3
	 */
	SimpleNaturalIdLoadAccess<T> enableFetchProfile(String profileName);

	/**
	 * Customize the associations fetched by specifying a
	 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}
	 * that should be disabled during this operation.
	 * <p>
	 * This allows the {@linkplain Session#isFetchProfileEnabled(String)
	 * session-level fetch profiles} to be temporarily overridden.
	 *
	 * @since 6.3
	 */
	SimpleNaturalIdLoadAccess<T> disableFetchProfile(String profileName);

	/**
	 * For entities with mutable natural ids, should Hibernate perform
	 * "synchronization" prior to performing lookups? The default is
	 * to perform "synchronization" (for correctness).
	 * <p>
	 * See {@link NaturalIdLoadAccess#setSynchronizationEnabled} for
	 * detailed discussion.
	 *
	 * @param enabled Should synchronization be performed?
	 *                {@code true} indicates synchronization will be performed;
	 *                {@code false} indicates it will be circumvented.
	 *
	 * @return {@code this}, for method chaining
	 */
	SimpleNaturalIdLoadAccess<T> setSynchronizationEnabled(boolean enabled);

	/**
	 * Return the persistent instance with the given natural id value,
	 * assuming that the instance exists. This method might return a
	 * proxied instance that is initialized on-demand, when a
	 * non-identifier method is accessed.
	 * <p>
	 * You should not use this method to determine if an instance exists;
	 * to check for existence, use {@link #load} instead. Use this only to
	 * retrieve an instance that you assume exists, where non-existence
	 * would be an actual error.
	 *
	 * @param naturalIdValue The value of the natural id
	 *
	 * @return The persistent instance or proxy, if an instance exists.
	 *         Otherwise, {@code null}.
	 */
	T getReference(Object naturalIdValue);

	/**
	 * Return the persistent instance with the given natural id value,
	 * or {@code null} if there is no such persistent instance. If the
	 * instance is already associated with the session, return that
	 * instance, initializing it if needed. This method never returns
	 * an uninitialized instance.
	 *
	 * @param naturalIdValue The value of the natural-id
	 *
	 * @return The persistent instance or {@code null}
	 */
	T load(Object naturalIdValue);

	/**
	 * Just like {@link #load}, except that here an {@link Optional}
	 * is returned.
	 *
	 * @param naturalIdValue The identifier
	 *
	 * @return The persistent instance, if any, as an {@link Optional}
	 */
	Optional<T> loadOptional(Object naturalIdValue);
}
