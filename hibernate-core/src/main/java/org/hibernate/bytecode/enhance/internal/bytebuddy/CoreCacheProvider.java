/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;

/**
 * A CacheProvider for ByteBuddy which is specifically designed for
 * our CoreTypePool: it ensures the cache content doesn't get tainted
 * by model types or anything else which isn't responsibility of this
 * particular type pool.
 * @implNote This cache instance shares the same @{@link CorePrefixFilter}
 * instance of the @{@link CoreTypePool} which is using it, and uses it
 * to guard writes into its internal caches.
 */
class CoreCacheProvider implements TypePool.CacheProvider {

	private final ConcurrentMap<String, TypePool.Resolution> storage = new ConcurrentHashMap<>();
	private final CorePrefixFilter acceptedPrefixes;

	CoreCacheProvider(final CorePrefixFilter acceptedPrefixes) {
		this.acceptedPrefixes = Objects.requireNonNull( acceptedPrefixes );
		register(
				Object.class.getName(),
				new TypePool.Resolution.Simple( TypeDescription.ForLoadedType.of( Object.class ) )
		);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TypePool.Resolution find(final String name) {
		return storage.get( name );
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TypePool.Resolution register(String name, TypePool.Resolution resolution) {
		//Ensure we DO NOT cache anything from a non-core namespace, to not leak application specific code:
		if ( acceptedPrefixes.isCoreClassName( name ) ) {
			TypePool.Resolution cached = storage.putIfAbsent( name, resolution );
			return cached == null
					? resolution
					: cached;
		}
		else {
			return resolution;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		storage.clear();
	}

}
