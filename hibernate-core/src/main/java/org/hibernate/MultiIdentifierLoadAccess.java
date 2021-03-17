/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.List;

import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;

/**
 * Loads multiple entities at once by identifiers, ultimately via one of the
 * {@link #multiLoad} methods, using the various options specified (if any)
 *
 * @author Steve Ebersole
 */
public interface MultiIdentifierLoadAccess<T> {
	/**
	 * Specify the {@link LockOptions} to use when retrieving the entity.
	 *
	 * @param lockOptions The lock options to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> with(LockOptions lockOptions);

	/**
	 * Specify the {@link CacheMode} to use when retrieving the entity.
	 *
	 * @param cacheMode The CacheMode to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> with(CacheMode cacheMode);

	default MultiIdentifierLoadAccess<T> with(RootGraph<T> graph) {
		return with( graph, GraphSemantic.LOAD );
	}

	MultiIdentifierLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic);

	/**
	 * Specify a batch size for loading the entities (how many at a time).  The default is
	 * to use a batch sizing strategy defined by the Dialect in use.  Any greater-than-one
	 * value here will override that default behavior.  If giving an explicit value here,
	 * care should be taken to not exceed the capabilities of of the underlying database.
	 * <p/>
	 * Note that overall a batch-size is considered a hint.  How the underlying loading
	 * mechanism interprets that is completely up to that underlying loading mechanism.
	 *
	 * @param batchSize The batch size
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> withBatchSize(int batchSize);

	/**
	 * Specify whether we should check the {@link Session} to see whether the first-level cache already contains any of the
	 * entities to be loaded in a managed state <b>for the purpose of not including those
	 * ids to the batch-load SQL</b>.
	 *
	 * @param enabled {@code true} enables this checking; {@code false} (the default) disables it.
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> enableSessionCheck(boolean enabled);

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
	MultiIdentifierLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled);

	/**
	 * Should the return List be ordered and positional in relation to the
	 * incoming ids?  If enabled (the default), the return List is ordered and
	 * positional relative to the incoming ids.  In other words, a request to
	 * {@code multiLoad([2,1,3])} will return {@code [Entity#2, Entity#1, Entity#3]}.
	 * <p/>
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
	MultiIdentifierLoadAccess<T> enableOrderedReturn(boolean enabled);

	/**
	 * Perform a load of multiple entities by identifiers.  See {@link #enableOrderedReturn}
	 * and {@link #enableReturnOfDeletedEntities} for options which effect
	 * the size and "shape" of the return list.
	 *
	 * @param ids The ids to load
	 * @param <K> The identifier type
	 *
	 * @return The persistent entities.
	 */
	<K extends Serializable> List<T> multiLoad(K... ids);

	/**
	 * Perform a load of multiple entities by identifiers.  See {@link #enableOrderedReturn}
	 * and {@link #enableReturnOfDeletedEntities} for options which effect
	 * the size and "shape" of the return list.
	 *
	 * @param ids The ids to load
	 * @param <K> The identifier type
	 *
	 * @return The persistent entities.
	 */
	<K extends Serializable> List<T> multiLoad(List<K> ids);
}
