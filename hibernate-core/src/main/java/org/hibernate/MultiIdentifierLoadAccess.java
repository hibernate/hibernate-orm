/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.util.List;

/**
 * Loads multiple entities at once by identifiers
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
	 * Should we check the Session to see whether it already contains any of the
	 * entities to be loaded in a managed state <b>for the purpose of not including those
	 * ids to the batch-load SQL</b>
	 *
	 * @param enabled {@code true} enables this checking; {@code false} disables it.
	 *
	 * @return {@code this}, for method chaining
	 */
	MultiIdentifierLoadAccess<T> enableSessionCheck(boolean enabled);

	/**
	 * Perform a load of multiple entities by identifiers
	 *
	 * @param ids The ids to load
	 * @param <K> The identifier type
	 *
	 * @return The persistent entities.
	 */
	<K extends Serializable> List<T> multiLoad(K... ids);

	/**
	 * Perform a load of multiple entities by identifiers
	 *
	 * @param ids The ids to load
	 * @param <K> The identifier type
	 *
	 * @return The persistent entities.
	 */
	<K extends Serializable> List<T> multiLoad(List<K> ids);
}
