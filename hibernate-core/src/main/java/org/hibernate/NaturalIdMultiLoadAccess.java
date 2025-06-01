/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.graph.GraphSemantic;

import java.util.List;

/**
 * Loads multiple instances of a given entity type at once, by
 * specifying a list of natural id values. This allows the entities
 * to be fetched from the database in batches.
 * <p>
 * <pre>
 * List&lt;Book&gt; books =
 *         session.byMultipleNaturalId(Book.class)
 *             .withBatchSize(10)
 *             .multiLoad(isbnList);
 * </pre>
 * <p>
 * Composite natural ids may be accommodated by passing a list of
 * maps of type {@code Map<String,Object>}  to {@link #multiLoad}.
 * Each map must contain the natural id attribute values keyed by
 * {@link org.hibernate.annotations.NaturalId @NaturalId} attribute
 * name.
 * <pre>
 * var compositeNaturalId =
 *         Map.of(Book_.ISBN, isbn, Book_.PRINTING, printing);
 * </pre>
 *
 * @see Session#byMultipleNaturalId(Class)
 * @see org.hibernate.annotations.NaturalId
 */
public interface NaturalIdMultiLoadAccess<T> {

	/**
	 * Specify the {@linkplain LockMode lock mode} to use when
	 * querying the database.
	 *
	 * @param lockMode The lock mode to apply
	 * @return {@code this}, for method chaining
	 */
	default NaturalIdMultiLoadAccess<T> with(LockMode lockMode) {
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
	NaturalIdMultiLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope);

	/**
	 * Specify the {@linkplain Timeout timeout} to use when
	 * querying the database.
	 *
	 * @param timeout The timeout to apply to the database operation
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdMultiLoadAccess<T> with(Timeout timeout);

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
	NaturalIdMultiLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Specify the {@link CacheMode} to use when obtaining an entity.
	 *
	 * @param cacheMode The {@code CacheMode} to use
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdMultiLoadAccess<T> with(CacheMode cacheMode);

	/**
	 * Override the associations fetched by default by specifying
	 * the complete list of associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default NaturalIdMultiLoadAccess<T> withFetchGraph(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.FETCH );
	}

	/**
	 * Augment the associations fetched by default by specifying a
	 * list of additional associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default NaturalIdMultiLoadAccess<T> withLoadGraph(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * @deprecated use {@link #withLoadGraph}
	 */
	@Deprecated(since = "6.3")
	default NaturalIdMultiLoadAccess<T> with(EntityGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * Customize the associations fetched by specifying an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph},
	 * and how it should be {@linkplain GraphSemantic interpreted}.
	 */
	NaturalIdMultiLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic);

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
	 * A batch size is considered a hint.
	 *
	 * @param batchSize The batch size
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdMultiLoadAccess<T> withBatchSize(int batchSize);

	/**
	 * Should {@link #multiLoad} return entity instances that have been
	 * {@link Session#remove(Object) marked for removal} in the current
	 * session, but not yet deleted in the database?
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
	NaturalIdMultiLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled);

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
	NaturalIdMultiLoadAccess<T> enableOrderedReturn(boolean enabled);

	/**
	 * Retrieve the entities with the given natural id values.
	 * <p>
	 * Note that the options {@link #enableReturnOfDeletedEntities} and
	 * {@link #enableOrderedReturn} affect the size and shape of the
	 * returned list of entity instances.
	 *
	 * @param ids The natural id values to load
	 *
	 * @return The managed entities.
	 */
	List<T> multiLoad(Object... ids);

	/**
	 * Retrieve the entities with the given natural id values.
	 * <p>
	 * Note that the options {@link #enableReturnOfDeletedEntities} and
	 * {@link #enableOrderedReturn} affect the size and shape of the
	 * returned list of entity instances.
	 *
	 * @param ids The natural id values to load
	 *
	 * @return The managed entities.
	 */
	List<T> multiLoad(List<?> ids);
}
