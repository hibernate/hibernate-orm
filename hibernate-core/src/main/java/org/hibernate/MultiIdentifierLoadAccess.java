/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.List;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.graph.GraphSemantic;

/**
 * Loads multiple instances of a given entity type at once, by
 * specifying a list of identifier values. This allows the entities
 * to be fetched from the database in batches.
 * <p>
 * <pre>
 * var graph = session.createEntityGraph(Book.class);
 * graph.addSubgraph(Book_.publisher);
 *
 * List&lt;Book&gt; books =
 *         session.byMultipleIds(Book.class)
 *             .withFetchGraph(graph)
 *             .withBatchSize(20)
 *             .multiLoad(bookIds);
 * </pre>
 *
 * @see Session#byMultipleIds(Class)
 *
 * @author Steve Ebersole
 */
public interface MultiIdentifierLoadAccess<T> {

	/**
	 * Specify the {@linkplain LockMode lock mode} to use when
	 * querying the database.
	 *
	 * @param lockMode The lock mode to apply
	 * @return {@code this}, for method chaining
	 */
	default MultiIdentifierLoadAccess<T> with(LockMode lockMode) {
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
	MultiIdentifierLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope);

	/**
	 * Specify the {@linkplain Timeout timeout} to use when
	 * querying the database.
	 *
	 * @param timeout The timeout to apply to the database operation
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> with(Timeout timeout);

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
	MultiIdentifierLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Specify the {@link CacheMode} to use when obtaining an entity.
	 *
	 * @param cacheMode The {@code CacheMode} to use
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> with(CacheMode cacheMode);

	/**
	 * Specify whether the entities should be loaded in read-only mode.
	 *
	 * @see Session#setDefaultReadOnly(boolean)
	 *
	 * @since 7.0
	 */
	MultiIdentifierLoadAccess<T> withReadOnly(boolean readOnly);

	/**
	 * Override the associations fetched by default by specifying
	 * the complete list of associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default MultiIdentifierLoadAccess<T> withFetchGraph(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.FETCH );
	}

	/**
	 * Augment the associations fetched by default by specifying a
	 * list of additional associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default MultiIdentifierLoadAccess<T> withLoadGraph(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * @deprecated use {@link #withLoadGraph}
	 */
	@Deprecated(since = "6.3")
	default MultiIdentifierLoadAccess<T> with(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * Customize the associations fetched by specifying an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph},
	 * and how it should be {@linkplain GraphSemantic interpreted}.
	 */
	MultiIdentifierLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic);

	/**
	 * Customize the associations fetched by specifying a
	 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}
	 * that should be enabled during this operation.
	 * <p>
	 * This allows the {@linkplain Session#isFetchProfileEnabled(String)
	 * session-level fetch profiles} to be temporarily overridden.
	 *
	 * @since 7.0
	 */
	MultiIdentifierLoadAccess<T> enableFetchProfile(String profileName);

	/**
	 * Customize the associations fetched by specifying a
	 * {@linkplain org.hibernate.annotations.FetchProfile fetch profile}
	 * that should be disabled during this operation.
	 * <p>
	 * This allows the {@linkplain Session#isFetchProfileEnabled(String)
	 * session-level fetch profiles} to be temporarily overridden.
	 *
	 * @since 7.0
	 */
	MultiIdentifierLoadAccess<T> disableFetchProfile(String profileName);

	/**
	 * Specify a batch size, that is, how many entities should be
	 * fetched in each request to the database.
	 * <ul>
	 * <li>By default, the batch sizing strategy is determined by the
	 *     {@linkplain org.hibernate.dialect.Dialect#getBatchLoadSizingStrategy
	 *    SQL dialect}, but
	 * <li>if some {@code batchSize>1} is specified as an
	 *     argument to this method, then that batch size will be used.
	 * </ul>
	 * <p>
	 * If an explicit batch size is set manually, care should be taken
	 * to not exceed the capabilities of the underlying database.
	 * <p>
	 * The performance impact of setting a batch size depends on whether
	 * a SQL array may be used to pass the list of identifiers to the
	 * database:
	 * <ul>
	 * <li>for databases which support standard SQL arrays, a smaller
	 *     batch size might be extremely inefficient compared to a very
	 *     large batch size or no batching at all, but
	 * <li>on the other hand, for databases with no SQL array type, a
	 *     large batch size results in long SQL statements with many JDBC
	 *     parameters.
	 * </ul>
	 * <p>
	 * A batch size is considered a hint.
	 *
	 * @param batchSize The batch size
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> withBatchSize(int batchSize);

	/**
	 * Specifies whether the ids of managed entity instances already
	 * cached in the current persistence context should be excluded
	 * from the list of ids sent to the database.
	 * <p>
	 * By default, all ids are included and sent to the database.
	 *
	 * @param enabled {@code true} if they should be excluded;
	 *                {@code false} if they should be included.
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> enableSessionCheck(boolean enabled);

	/**
	 * Should {@link #multiLoad} return entity instances that have been
	 * {@linkplain Session#remove(Object) marked for removal} in the
	 * current session, but not yet deleted in the database?
	 * <p>
	 * By default, instances marked for removal are replaced by null in
	 * the returned list of entities when {@link #enableOrderedReturn}
	 * is used.
	 *
	 * @param enabled {@code true} if removed entities should be returned;
	 *                {@code false} if they should be replaced by null values.
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled);

	/**
	 * Should the returned list of entity instances be ordered, with the
	 * position of an entity instance determined by the position of its
	 * identifier in the list if ids passed to {@link #multiLoad}?
	 * <p>
	 * By default, the returned list is ordered and the positions of the
	 * entities correspond to the positions of their ids. In this case,
	 * the {@linkplain #enableReturnOfDeletedEntities handling of entities
	 * marked for removal} becomes important.
	 *
	 * @param enabled {@code true} if entity instances should be ordered;
	 *                {@code false} if they may be returned in any order.
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> enableOrderedReturn(boolean enabled);

	/**
	 * Retrieve the entities with the given identifiers.
	 * <p>
	 * Note that the options {@link #enableReturnOfDeletedEntities} and
	 * {@link #enableOrderedReturn} affect the size and shape of the
	 * returned list of entity instances.
	 *
	 * @param <K> The identifier type
	 *
	 * @param ids The ids to load
	 * @return The persistent entities.
	 */
	<K> List<T> multiLoad(K... ids);

	/**
	 * Retrieve the entities with the given identifiers.
	 * <p>
	 * Note that the options {@link #enableReturnOfDeletedEntities} and
	 * {@link #enableOrderedReturn} affect the size and shape of the
	 * returned list of entity instances.
	 *
	 * @param ids The ids to load
	 * @param <K> The identifier type
	 *
	 * @return The persistent entities.
	 */
	<K> List<T> multiLoad(List<K> ids);
}
