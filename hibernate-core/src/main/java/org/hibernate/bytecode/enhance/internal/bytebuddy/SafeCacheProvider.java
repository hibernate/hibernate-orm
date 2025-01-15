/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.HashMap;
import java.util.Map;

import net.bytebuddy.pool.TypePool;

/**
 * An implementation of @{@link net.bytebuddy.pool.TypePool.CacheProvider} which scopes
 * all state to the current thread, and allows to remove specific registrations.
 * The threadscoping is necessary to resolve a race condition happening during concurrent entity enhancement:
 * while one thread is resolving metadata about the entity which needs to be enhanced, other threads
 * might be working on the same operation (or a different entity which needs this one to be resolved)
 * and the resolution output - potentially shared across them - could be tainted as they do need
 * to occasionally work on different input because of the specific overrides managed via @{@link OverridingClassFileLocator}.
 */
final class SafeCacheProvider implements TypePool.CacheProvider {

	private final ThreadLocal<Map<String, TypePool.Resolution>> delegate = ThreadLocal.withInitial( () -> new HashMap<>() );

	@Override
	public TypePool.Resolution find(final String name) {
		return delegate.get().get( name );
	}

	@Override
	public TypePool.Resolution register(final String name, final TypePool.Resolution resolution) {
		final TypePool.Resolution cached = delegate.get().putIfAbsent( name, resolution );
		return cached == null
				? resolution
				: cached;
	}

	@Override
	public void clear() {
		delegate.get().clear();
		delegate.remove();
	}

	public TypePool.Resolution remove(final String name) {
		return delegate.get().remove( name );
	}
}
