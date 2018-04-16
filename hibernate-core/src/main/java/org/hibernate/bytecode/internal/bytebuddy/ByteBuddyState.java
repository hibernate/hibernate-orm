/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.dynamic.scaffold.TypeValidation;

/**
 * An utility to hold all ByteBuddy related state, as in the current version of
 * Hibernate the Bytecode Provider state is held in a static field, yet ByteBuddy
 * is able to benefit from some caching and general state reuse.
 */
public final class ByteBuddyState {

	/**
	 * Ideally shouldn't be static but it has to until we can remove the
	 * deprecated static methods.
	 */
	private static final ByteBuddy buddy = new ByteBuddy().with( TypeValidation.DISABLED );

	/**
	 * This currently needs to be static: the BytecodeProvider is a static field of Environment and
	 * is being accessed from static methods.
	 * It will be easier to maintain the cache and its state when it will no longer be static
	 * in Hibernate ORM 6+.
	 * Opted for WEAK keys to avoid leaking the classloader in case the SessionFactory isn't closed.
	 * Avoiding Soft keys as they are prone to cause issues with unstable performance.
	 */
	private static final TypeCache<TypeCache.SimpleKey> CACHE = new TypeCache.WithInlineExpunction<TypeCache.SimpleKey>(
			TypeCache.Sort.WEAK );

	/**
	 * Access to ByteBuddy. It's almost equivalent to creating a new ByteBuddy instance,
	 * yet slightly preferrable so to be able to reuse the same instance.
	 * @return
	 */
	public ByteBuddy getCurrentyByteBuddy() {
		return buddy;
	}

	/**
	 * @deprecated as we should not need static access to this state.
	 * This will be removed with no replacement.
	 * It's actually likely that this whole class becomes unnecessary in the near future.
	 */
	@Deprecated
	public static TypeCache<TypeCache.SimpleKey> getCacheForProxies() {
		return CACHE;
	}

	/**
	 * Wipes out all known caches used by ByteBuddy. This implies it might trigger the need
	 * to re-create some helpers if used at runtime, especially as this state is shared by
	 * multiple SessionFactory instances, but at least ensures we cleanup anything which is no
	 * longer needed after a SessionFactory close.
	 * The assumption is that closing SessionFactories is a rare event; in this perspective the cost
	 * of re-creating the small helpers should be negligible.
	 */
	void clearState() {
		CACHE.clear();
	}

	/**
	 * @deprecated as we should not need static access to this state.
	 * This will be removed with no replacement.
	 * It's actually likely that this whole class becomes unnecessary in the near future.
	 */
	@Deprecated
	public static ByteBuddy getStaticByteBuddyInstance() {
		return buddy;
	}

}
