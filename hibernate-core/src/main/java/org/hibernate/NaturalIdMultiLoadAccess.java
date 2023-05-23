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
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Defines the ability to load multiple entities by simple natural-id simultaneously.
 */
public interface NaturalIdMultiLoadAccess<T> {
	/**
	 * Specify the {@link LockOptions} to use when retrieving the entity.
	 *
	 * @param lockOptions The lock options to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdMultiLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Specify the {@link CacheMode} to use when retrieving the entity.
	 *
	 * @param cacheMode The CacheMode to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdMultiLoadAccess<T> with(CacheMode cacheMode);

	default NaturalIdMultiLoadAccess<T> withFetchGraph(RootGraph<T> graph) {
		return with( graph, GraphSemantic.FETCH );
	}

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
	 * Define a load or fetch graph to be used when retrieving the entity
	 */
	NaturalIdMultiLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic);

	/**
	 * Specify a batch size for loading the entities (how many at a time).  The default is
	 * to use a batch sizing strategy defined by the Dialect in use.  Any greater-than-one
	 * value here will override that default behavior.  If giving an explicit value here,
	 * care should be taken to not exceed the capabilities of the underlying database.
	 * <p>
	 * Note that overall a batch-size is considered a hint.
	 *
	 * @param batchSize The batch size
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdMultiLoadAccess<T> withBatchSize(int batchSize);

	/**
	 * Should the multi-load operation be allowed to return entities that are locally
	 * deleted?  A locally deleted entity is one which has been passed to this
	 * Session's {@link Session#delete} / {@link Session#remove} method, but not
	 * yet flushed.  The default behavior is to handle them as null in the return
	 * (see {@link #enableOrderedReturn}).
	 *
	 * @param enabled {@code true} enables returning the deleted entities;
	 * {@code false} (the default) disables it.
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdMultiLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled);

	/**
	 * Should the return List be ordered and positional in relation to the
	 * incoming ids?  If enabled (the default), the return List is ordered and
	 * positional relative to the incoming ids.  In other words, a request to
	 * {@code multiLoad([2,1,3])} will return {@code [Entity#2, Entity#1, Entity#3]}.
	 * <p>
	 * An important distinction is made here in regards to the handling of
	 * unknown entities depending on this "ordered return" setting.  If enabled
	 * a null is inserted into the List at the proper position(s).  If disabled,
	 * the nulls are not put into the return List.  In other words, consumers of
	 * the returned ordered List would need to be able to handle null elements.
	 *
	 * @param enabled {@code true} (the default) enables ordering;
	 * {@code false} disables it.
	 *
	 * @return {@code this}, for method chaining
	 */
	NaturalIdMultiLoadAccess<T> enableOrderedReturn(boolean enabled);

	/**
	 * Perform a load of multiple entities by natural-id.
	 * <p>
	 * See {@link #enableOrderedReturn} and {@link #enableReturnOfDeletedEntities}
	 * for options which effect the size and "shape" of the return list.
	 *
	 * @param ids The natural-id values to load
	 *
	 * @return The managed entities.
	 */
	List<T> multiLoad(Object... ids);

	/**
	 * Perform a load of multiple entities by natural-id.
	 * <p>
	 * See {@link #enableOrderedReturn} and {@link #enableReturnOfDeletedEntities}
	 * for options which effect the size and "shape" of the return list.
	 *
	 * @param ids The natural-id values to load
	 *
	 * @return The managed entities.
	 */
	List<T> multiLoad(List<?> ids);

	/**
	 * Helper for creating a Map that represents the value of a compound natural-id
	 * for use in loading.  The passed array is expected to have an even number of elements
	 * representing key, value pairs.  E.g.  `using( "system", "matrix", "username", "neo" )`
	 */
	static Map<String,?> compoundValue(Object... elements) {
		return CollectionHelper.asMap( elements );
	}
}
