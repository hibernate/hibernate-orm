/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.Optional;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.graph.GraphSemantic;

/**
 * Loads an entity by its primary identifier.
 * <p>
 * The interface is especially useful when customizing association
 * fetching using an {@link jakarta.persistence.EntityGraph}.
 * <pre>
 * var graph = session.createEntityGraph(Book.class);
 * graph.addSubgraph(Book_.publisher);
 * graph.addPluralSubgraph(Book_.authors)
 *     .addSubgraph(Author_.person);
 *
 * Book book =
 *         session.byId(Book.class)
 *             .withFetchGraph(graph)
 *             .load(bookId);
 * </pre>
 * <p>
 * It's also useful for loading entity instances with a specific
 * {@linkplain CacheMode cache interaction mode} in effect, or in
 * {@linkplain Session#setReadOnly(Object, boolean) read-only mode}.
 * <pre>
 * Book book =
 *         session.byId(Book.class)
 *             .with(CacheMode.GET)
 *             .withReadOnly(true)
 *             .load(bookId);
 * </pre>
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 *
 * @see Session#byId(Class)
 */
public interface IdentifierLoadAccess<T> {

	/**
	 * Specify the {@linkplain LockMode lock mode} to use when
	 * querying the database.
	 *
	 * @param lockMode The lock mode to apply
	 * @return {@code this}, for method chaining
	 */
	default IdentifierLoadAccess<T> with(LockMode lockMode) {
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
	IdentifierLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope);

	/**
	 * Specify the {@linkplain Timeout timeout} to use when
	 * querying the database.
	 *
	 * @param timeout The timeout to apply to the database operation
	 *
	 * @return {@code this}, for method chaining
	 */
	IdentifierLoadAccess<T> with(Timeout timeout);

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
	IdentifierLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Specify the {@link CacheMode} to use when obtaining an entity.
	 *
	 * @param cacheMode The {@code CacheMode} to use
	 *
	 * @return {@code this}, for method chaining
	 */
	IdentifierLoadAccess<T> with(CacheMode cacheMode);

	/**
	 * Specify whether the entity should be loaded in read-only mode.
	 *
	 * @see Session#setDefaultReadOnly(boolean)
	 */
	IdentifierLoadAccess<T> withReadOnly(boolean readOnly);

	/**
	 * Override the associations fetched by default by specifying
	 * the complete list of associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default IdentifierLoadAccess<T> withFetchGraph(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.FETCH );
	}

	/**
	 * Augment the associations fetched by default by specifying a
	 * list of additional associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default IdentifierLoadAccess<T> withLoadGraph(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * @deprecated use {@link #withLoadGraph}
	 */
	@Deprecated(since = "6.3")
	default IdentifierLoadAccess<T> with(EntityGraph<T> graph) {
		return withLoadGraph( graph );
	}

	/**
	 * Customize the associations fetched by specifying an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph},
	 * and how it should be {@linkplain GraphSemantic interpreted}.
	 */
	IdentifierLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic);

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
	IdentifierLoadAccess<T> enableFetchProfile(String profileName);

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
	IdentifierLoadAccess<T> disableFetchProfile(String profileName);

	/**
	 * Return the persistent instance with the given identifier, assuming
	 * that the instance exists. This method might return a proxied instance
	 * that is initialized on-demand, when a non-identifier method is accessed.
	 * <p>
	 * You should not use this method to determine if an instance exists;
	 * to check for existence, use {@link #load} instead. Use this only to
	 * retrieve an instance that you assume exists, where non-existence would
	 * be an actual error.
	 *
	 * @param id The identifier for which to obtain a reference
	 *
	 * @return the persistent instance or proxy
	 */
	T getReference(Object id);

	/**
	 * Return the persistent instance with the given identifier, or null
	 * if there is no such persistent instance. If the instance is already
	 * associated with the session, return that instance, initializing it
	 * if needed. This method never returns an uninitialized instance.
	 *
	 * @param id The identifier
	 *
	 * @return The persistent instance or {@code null}
	 */
	T load(Object id);

	/**
	 * Just like {@link #load}, except that here an {@link Optional} is
	 * returned.
	 *
	 * @param id The identifier
	 *
	 * @return The persistent instance, if any, as an {@link Optional}
	 */
	Optional<T> loadOptional(Object id);
}
