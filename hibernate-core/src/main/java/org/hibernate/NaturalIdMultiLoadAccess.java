/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate;

import java.util.List;
import java.util.Map;

import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;

import static org.hibernate.internal.util.collections.CollectionHelper.asMap;

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
	 * Specify the {@linkplain LockOptions lock options} to use when
	 * querying the database.
	 *
	 * @param lockOptions The lock options to use
	 *
	 * @return {@code this}, for method chaining
	 */
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
	default NaturalIdMultiLoadAccess<T> withFetchGraph(RootGraph<T> graph) {
		return with( graph, GraphSemantic.FETCH );
	}

	/**
	 * Augment the associations fetched by default by specifying a
	 * list of additional associations to be fetched as an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph}.
	 *
	 * @since 6.3
	 */
	default NaturalIdMultiLoadAccess<T> withLoadGraph(RootGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * @deprecated use {@link #withLoadGraph}
	 */
	@Deprecated(since = "6.3")
	default NaturalIdMultiLoadAccess<T> with(RootGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	/**
	 * Customize the associations fetched by specifying an
	 * {@linkplain jakarta.persistence.EntityGraph entity graph},
	 * and how it should be {@linkplain GraphSemantic interpreted}.
	 */
	NaturalIdMultiLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic);

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
	 * session, but not yet {@code delete}d in the database?
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

	/**
	 * Helper for creating a {@link Map} that represents the value of a
	 * composite natural id. An even number of arguments is expected,
	 * with each attribute name followed by its value.
	 *
	 * @see NaturalIdLoadAccess#using(Object...)
	 *
	 * @deprecated use {@link Map#of} instead
	 */
	@Deprecated(since = "6.3")
	static Map<String,?> compoundValue(Object... elements) {
		return asMap( elements );
	}
}
