/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.CloseableIterable;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.filter.AcceptAllKeyValueFilter;
import org.infinispan.filter.Converter;
import org.infinispan.filter.KeyValueFilter;
import org.infinispan.filter.NullValueConverter;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcOptions;

/**
 * Helper for dealing with Infinispan cache instances.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
public class Caches {

	private Caches() {
		// Suppresses default constructor, ensuring non-instantiability.
	}

	/**
	 * Call an operation within a transaction. This method guarantees that the
	 * right pattern is used to make sure that the transaction is always either
	 * committed or rollback.
	 *
	 * @param cache instance whose transaction manager to use
	 * @param c callable instance to run within a transaction
	 * @param <T> type of callable return
	 * @return returns whatever the callable returns
	 * @throws Exception if any operation within the transaction fails
	 */
	public static <T> T withinTx(
			AdvancedCache cache,
			Callable<T> c) throws Exception {
		// Retrieve transaction manager
		return withinTx( cache.getTransactionManager(), c );
	}

	/**
	 * Call an operation within a transaction. This method guarantees that the
	 * right pattern is used to make sure that the transaction is always either
	 * committed or rollbacked.
	 *
	 * @param tm transaction manager
	 * @param c callable instance to run within a transaction
	 * @param <T> type of callable return
	 * @return returns whatever the callable returns
	 * @throws Exception if any operation within the transaction fails
	 */
	public static <T> T withinTx(
			TransactionManager tm,
			Callable<T> c) throws Exception {
		if ( tm == null ) {
			try {
				return c.call();
			}
			catch (Exception e) {
				throw e;
			}
		}
		else {
			tm.begin();
			try {
				return c.call();
			}
			catch (Exception e) {
				tm.setRollbackOnly();
				throw e;
			}
			finally {
				if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
					tm.commit();
				}
				else {
					tm.rollback();
				}
			}
		}
	}

	public static void withinTx(TransactionManager tm, final Runnable runnable) throws Exception {
		withinTx(tm, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				runnable.run();
				return null;
			}
		});
	}

	/**
	 * Transform a given cache into a local cache
	 *
	 * @param cache to be transformed
	 * @return a cache that operates only in local-mode
	 */
	public static AdvancedCache localCache(AdvancedCache cache) {
		return cache.withFlags( Flag.CACHE_MODE_LOCAL );
	}

	/**
	 * Transform a given cache into a cache that ignores return values for
	 * operations returning previous values, i.e. {@link AdvancedCache#put(Object, Object)}
	 *
	 * @param cache to be transformed
	 * @return a cache that ignores return values
	 */
	public static AdvancedCache ignoreReturnValuesCache(AdvancedCache cache) {
		return cache.withFlags( Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP, Flag.IGNORE_RETURN_VALUES );
	}

	/**
	 * Transform a given cache into a cache that ignores return values for
	 * operations returning previous values, i.e. {@link AdvancedCache#put(Object, Object)},
	 * adding an extra flag.
	 *
	 * @param cache to be transformed
	 * @param extraFlag to add to the returned cache
	 * @return a cache that ignores return values
	 */
	public static AdvancedCache ignoreReturnValuesCache(
			AdvancedCache cache, Flag extraFlag) {
		return cache.withFlags(
				Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP, Flag.IGNORE_RETURN_VALUES, extraFlag
		);
	}

	/**
	 * Transform a given cache into a cache that writes cache entries without
	 * waiting for them to complete, adding an extra flag.
	 *
	 * @param cache to be transformed
	 * @param extraFlag to add to the returned cache
	 * @return a cache that writes asynchronously
	 */
	public static AdvancedCache asyncWriteCache(
			AdvancedCache cache,
			Flag extraFlag) {
		return cache.withFlags(
				Flag.SKIP_CACHE_LOAD,
				Flag.SKIP_REMOTE_LOOKUP,
				Flag.FORCE_ASYNCHRONOUS,
				extraFlag
		);
	}

	/**
	 * Transform a given cache into a cache that fails silently if cache writes fail.
	 *
	 * @param cache to be transformed
	 * @return a cache that fails silently if cache writes fail
	 */
	public static AdvancedCache failSilentWriteCache(AdvancedCache cache) {
		return cache.withFlags(
				Flag.FAIL_SILENTLY,
				Flag.ZERO_LOCK_ACQUISITION_TIMEOUT,
				Flag.SKIP_CACHE_LOAD,
				Flag.SKIP_REMOTE_LOOKUP
		);
	}

	/**
	 * Transform a given cache into a cache that fails silently if
	 * cache writes fail, adding an extra flag.
	 *
	 * @param cache to be transformed
	 * @param extraFlag to be added to returned cache
	 * @return a cache that fails silently if cache writes fail
	 */
	public static AdvancedCache failSilentWriteCache(
			AdvancedCache cache,
			Flag extraFlag) {
		return cache.withFlags(
				Flag.FAIL_SILENTLY,
				Flag.ZERO_LOCK_ACQUISITION_TIMEOUT,
				Flag.SKIP_CACHE_LOAD,
				Flag.SKIP_REMOTE_LOOKUP,
				extraFlag
		);
	}

	/**
	 * Transform a given cache into a cache that fails silently if
	 * cache reads fail.
	 *
	 * @param cache to be transformed
	 * @return a cache that fails silently if cache reads fail
	 */
	public static AdvancedCache failSilentReadCache(AdvancedCache cache) {
		return cache.withFlags(
				Flag.FAIL_SILENTLY,
				Flag.ZERO_LOCK_ACQUISITION_TIMEOUT
		);
	}

	/**
	 * Broadcast an evict-all command with the given cache instance.
	 *
	 * @param cache instance used to broadcast command
	 */
	public static void broadcastEvictAll(AdvancedCache cache) {
		final RpcManager rpcManager = cache.getRpcManager();
		if ( rpcManager != null ) {
			// Only broadcast evict all if it's clustered
			final CacheCommandInitializer factory = cache.getComponentRegistry()
					.getComponent( CacheCommandInitializer.class );
			final boolean isSync = isSynchronousCache( cache );

			final EvictAllCommand cmd = factory.buildEvictAllCommand( cache.getName() );
			final RpcOptions options = rpcManager.getDefaultRpcOptions( isSync );
			rpcManager.invokeRemotely( null, cmd, options );
		}
	}

	/**
	 * Indicates whether the given cache is configured with
	 * {@link org.infinispan.configuration.cache.CacheMode#INVALIDATION_ASYNC} or
	 * {@link org.infinispan.configuration.cache.CacheMode#INVALIDATION_SYNC}.
	 *
	 * @param cache to check for invalidation configuration
	 * @return true if the cache is configured with invalidation, false otherwise
	 */
	public static boolean isInvalidationCache(AdvancedCache cache) {
		return cache.getCacheConfiguration()
				.clustering().cacheMode().isInvalidation();
	}

	/**
	 * Indicates whether the given cache is configured with
	 * {@link org.infinispan.configuration.cache.CacheMode#REPL_SYNC},
	 * {@link org.infinispan.configuration.cache.CacheMode#INVALIDATION_SYNC}, or
	 * {@link org.infinispan.configuration.cache.CacheMode#DIST_SYNC}.
	 *
	 * @param cache to check for synchronous configuration
	 * @return true if the cache is configured with synchronous mode, false otherwise
	 */
	public static boolean isSynchronousCache(AdvancedCache cache) {
		return cache.getCacheConfiguration()
				.clustering().cacheMode().isSynchronous();
	}

	/**
	 * Indicates whether the given cache is configured to cluster its contents.
	 * A cache is considered to clustered if it's configured with any cache mode
	 * except {@link org.infinispan.configuration.cache.CacheMode#LOCAL}
	 *
	 * @param cache to check whether it clusters its contents
	 * @return true if the cache is configured with clustering, false otherwise
	 */
	public static boolean isClustered(AdvancedCache cache) {
		return cache.getCacheConfiguration()
				.clustering().cacheMode().isClustered();
	}

	public static boolean isTransactionalCache(AdvancedCache cache) {
		return cache.getCacheConfiguration().transaction().transactionMode().isTransactional();
	}


	public static void removeAll(AdvancedCache cache) {
		CloseableIterator it = cache.keySet().iterator();
		try {
			while (it.hasNext()) {
				// Cannot use it.next(); it.remove() due to ISPN-5653
				cache.remove(it.next());
			}
		}
		finally {
			it.close();
		}
	}

	/**
	 * This interface is provided for convenient fluent use of CloseableIterable
	 */
	public interface CollectableCloseableIterable<T> extends CloseableIterable<T> {
		Set<T> toSet();
	}

	public interface MapCollectableCloseableIterable<K, V> extends CloseableIterable<CacheEntry<K, V>> {
		Map<K, V> toMap();
	}

	public static <K, V> CollectableCloseableIterable<K> keys(AdvancedCache<K, V> cache, KeyValueFilter<K, V> filter) {
		// HHH-10023: we can't use keySet()
		final CloseableIterable<CacheEntry<K, Void>> entryIterable = cache
				.filterEntries( filter )
				.converter( NullValueConverter.getInstance() );
		return new CollectableCloseableIterableImpl<K, Void, K>(entryIterable, Selector.KEY);
	}

	public static <K, V> CollectableCloseableIterable<V> values(AdvancedCache<K, V> cache, KeyValueFilter<K, V> filter) {
		if (cache.getCacheConfiguration().transaction().transactionMode().isTransactional()) {
			// Dummy read to enlist the LocalTransaction as workaround for ISPN-5676
			cache.containsKey(false);
		}
		// HHH-10023: we can't use values()
		final CloseableIterable<CacheEntry<K, V>> entryIterable = cache.filterEntries(filter);
		return new CollectableCloseableIterableImpl<K, V, V>(entryIterable, Selector.VALUE);
	}

	public static <K, V, T> CollectableCloseableIterable<T> values(AdvancedCache<K, V> cache, KeyValueFilter<K, V> filter, Converter<K, V, T> converter) {
		if (cache.getCacheConfiguration().transaction().transactionMode().isTransactional()) {
			// Dummy read to enlist the LocalTransaction as workaround for ISPN-5676
			cache.containsKey(false);
		}
		// HHH-10023: we can't use values()
		final CloseableIterable<CacheEntry<K, T>> entryIterable = cache.filterEntries(filter).converter(converter);
		return new CollectableCloseableIterableImpl<K, T, T>(entryIterable, Selector.VALUE);
	}

	public static <K, V> MapCollectableCloseableIterable<K, V> entrySet(AdvancedCache<K, V> cache) {
		return entrySet(cache, (KeyValueFilter<K, V>) AcceptAllKeyValueFilter.getInstance());
	}

	public static <K, V> MapCollectableCloseableIterable<K, V> entrySet(AdvancedCache<K, V> cache, KeyValueFilter<K, V> filter) {
		if (cache.getCacheConfiguration().transaction().transactionMode().isTransactional()) {
			// Dummy read to enlist the LocalTransaction as workaround for ISPN-5676
			cache.containsKey(false);
		}
		// HHH-10023: we can't use values()
		final CloseableIterable<CacheEntry<K, V>> entryIterable = cache.filterEntries(filter);
		return new MapCollectableCloseableIterableImpl<K, V>(entryIterable);
	}

	public static <K, V, T> MapCollectableCloseableIterable<K, T> entrySet(AdvancedCache<K, V> cache, KeyValueFilter<K, V> filter, Converter<K, V, T> converter) {
		if (cache.getCacheConfiguration().transaction().transactionMode().isTransactional()) {
			// Dummy read to enlist the LocalTransaction as workaround for ISPN-5676
			cache.containsKey(false);
		}
		// HHH-10023: we can't use values()
		final CloseableIterable<CacheEntry<K, T>> entryIterable = cache.filterEntries(filter).converter(converter);
		return new MapCollectableCloseableIterableImpl<K, T>(entryIterable);
	}

	/* Function<CacheEntry<K, V>, T> */
	private interface Selector<K, V, T> {
		Selector KEY = new Selector<Object, Void, Object>() {
			@Override
			public Object apply(CacheEntry<Object, Void> entry) {
				return entry.getKey();
			}
		};

		Selector VALUE = new Selector<Object, Object, Object>() {
			@Override
			public Object apply(CacheEntry<Object, Object> entry) {
				return entry.getValue();
			}
		};

		T apply(CacheEntry<K, V> entry);
	}

	private static class CollectableCloseableIterableImpl<K, V, T> implements CollectableCloseableIterable<T> {
		private final CloseableIterable<CacheEntry<K, V>> entryIterable;
		private final Selector<K, V, T> selector;

		public CollectableCloseableIterableImpl(CloseableIterable<CacheEntry<K, V>> entryIterable, Selector<K, V, T> selector) {
			this.entryIterable = entryIterable;
			this.selector = selector;
		}

		@Override
		public void close() {
			entryIterable.close();
		}

		@Override
		public CloseableIterator<T> iterator() {
			final CloseableIterator<CacheEntry<K, V>> entryIterator = entryIterable.iterator();
			return new CloseableIterator<T>() {
				@Override
				public void close() {
					entryIterator.close();
				}

				@Override
				public boolean hasNext() {
					return entryIterator.hasNext();
				}

				@Override
				public T next() {
					return selector.apply(entryIterator.next());
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException( "remove() not supported" );
				}
			};
		}

		@Override
		public String toString() {
			CloseableIterator<CacheEntry<K, V>> it = entryIterable.iterator();
			try {
				if (!it.hasNext()) {
					return "[]";
				}

				StringBuilder sb = new StringBuilder();
				sb.append('[');
				for (; ; ) {
					CacheEntry<K, V> entry = it.next();
					sb.append(selector.apply(entry));
					if (!it.hasNext()) {
						return sb.append(']').toString();
					}
					sb.append(',').append(' ');
				}
			}
			finally {
				it.close();
			}
		}

		@Override
		public Set toSet() {
			HashSet set = new HashSet();
			CloseableIterator it = iterator();
			try {
				while (it.hasNext()) {
					set.add(it.next());
				}
			}
			finally {
				it.close();
			}
			return set;
		}
	}

	private static class MapCollectableCloseableIterableImpl<K, V> implements MapCollectableCloseableIterable<K, V> {
		private final CloseableIterable<CacheEntry<K, V>> entryIterable;

		public MapCollectableCloseableIterableImpl(CloseableIterable<CacheEntry<K, V>> entryIterable) {
			this.entryIterable = entryIterable;
		}

		@Override
		public Map<K, V> toMap() {
			Map<K, V> map = new HashMap<K, V>();
			CloseableIterator<CacheEntry<K, V>> it = entryIterable.iterator();
			try {
				while (it.hasNext()) {
					CacheEntry<K, V> entry = it.next();
					V value = entry.getValue();
					if (value != null) {
						map.put(entry.getKey(), value);
					}
				}
				return map;
			}
			finally {
				it.close();
			}
		}

		@Override
		public String toString() {
			CloseableIterator<CacheEntry<K, V>> it = entryIterable.iterator();
			try {
				if (!it.hasNext()) {
					return "{}";
				}

				StringBuilder sb = new StringBuilder();
				sb.append('{');
				for (; ; ) {
					CacheEntry<K, V> entry = it.next();
					sb.append(entry.getKey()).append('=').append(entry.getValue());
					if (!it.hasNext()) {
						return sb.append('}').toString();
					}
					sb.append(',').append(' ');
				}
			}
			finally {
				it.close();
			}
		}

		@Override
		public void close() {
			entryIterable.close();
		}

		@Override
		public CloseableIterator<CacheEntry<K, V>> iterator() {
			return entryIterable.iterator();
		}
	}
}
