/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import static org.hibernate.internal.CoreLogging.messageLogger;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyFactory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;

/**
 * An utility to hold all ByteBuddy related state, as in the current version of
 * Hibernate the Bytecode Provider state is held in a static field, yet ByteBuddy
 * is able to benefit from some caching and general state reuse.
 */
public final class ByteBuddyState {

	private static final CoreMessageLogger LOG = messageLogger( ByteBuddyProxyFactory.class );

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

	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

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

	public static ClassLoadingStrategy<?> resolveClassLoadingStrategy(Class<?> originalClass) {
		if ( ClassInjector.UsingLookup.isAvailable() ) {
			// This is only enabled for JDK 9+

			Method privateLookupIn;
			try {
				privateLookupIn = MethodHandles.class.getMethod( "privateLookupIn", Class.class, MethodHandles.Lookup.class );
			}
			catch (Exception e) {
				throw new HibernateException( LOG.bytecodeEnhancementFailed( originalClass.getName() ), e );
			}

			try {
				Object privateLookup;

				try {
					privateLookup = privateLookupIn.invoke( null, originalClass, LOOKUP );
				}
				catch (InvocationTargetException exception) {
					if ( exception.getCause() instanceof IllegalAccessException ) {
						return new ClassLoadingStrategy.ForUnsafeInjection( originalClass.getProtectionDomain() );
					}
					else {
						throw new HibernateException( LOG.bytecodeEnhancementFailed( originalClass.getName() ), exception.getCause() );
					}
				}

				return ClassLoadingStrategy.UsingLookup.of( privateLookup );
			}
			catch (Throwable e) {
				throw new HibernateException( LOG.bytecodeEnhancementFailedUnableToGetPrivateLookupFor( originalClass.getName() ), e );
			}
		}
		else {
			return new ClassLoadingStrategy.ForUnsafeInjection( originalClass.getProtectionDomain() );
		}
	}

}
