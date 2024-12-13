/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;

/**
 * A TypePool suitable for loading user's classes,
 * potentially in parallel operations.
 */
public class ModelTypePool extends TypePool.Default implements EnhancerClassLocator {

	private final ConcurrentHashMap<String, Resolution> resolutions = new ConcurrentHashMap<>();
	private final OverridingClassFileLocator locator;

	private ModelTypePool(CacheProvider cacheProvider, OverridingClassFileLocator classFileLocator, CoreTypePool parent) {
		super( cacheProvider, classFileLocator, ReaderMode.FAST, parent );
		this.locator = classFileLocator;
	}

	/**
	 * Creates a new empty EnhancerClassLocator instance which will load any application
	 * classes that need being reflected on from the ClassLoader passed as parameter.
	 * This TypePool will delegate, parent first, to a newly constructed empty instance
	 * of CoreTypePool; this parent pool will be used to load non-application types from
	 * the Hibernate classloader instead, not the one specified as argument.
	 * @see CoreTypePool
	 * @param classLoader
	 * @return the newly created EnhancerClassLocator
	 */
	public static EnhancerClassLocator buildModelTypePool(ClassLoader classLoader) {
		return ThreadsafeLocator.buildModelTypePool( classLoader );
	}

	/**
	 * Similar to {@link #buildModelTypePool(ClassLoader)} except the application classes
	 * are not necessarily sourced from a standard classloader: it accepts a {@link ClassFileLocator},
	 * which offers some more flexibility.
	 * @param classFileLocator
	 * @return the newly created EnhancerClassLocator
	 */
	public static EnhancerClassLocator buildModelTypePool(ClassFileLocator classFileLocator) {
		return buildModelTypePool( classFileLocator, new CoreTypePool() );
	}

	/**
	 * Similar to {@link #buildModelTypePool(ClassFileLocator)} but allows specifying an existing
	 * {@link CoreTypePool} to be used as parent pool.
	 * This forms allows constructing a custom CoreTypePool and also separated the cache of the parent pool,
	 * which might be useful to reuse for multiple enhancement processes while desiring a clean new
	 * state for the {@link ModelTypePool}.
	 * @param classFileLocator
	 * @param coreTypePool
	 * @return
	 */
	public static EnhancerClassLocator buildModelTypePool(ClassFileLocator classFileLocator, CoreTypePool coreTypePool) {
		return buildModelTypePool( classFileLocator, coreTypePool, new TypePool.CacheProvider.Simple() );
	}

	/**
	 * The more advanced strategy to construct a new ModelTypePool, allowing customization of all its aspects.
	 * @param classFileLocator
	 * @param coreTypePool
	 * @param cacheProvider
	 * @return
	 */
	public static EnhancerClassLocator buildModelTypePool(ClassFileLocator classFileLocator, CoreTypePool coreTypePool, CacheProvider cacheProvider) {
		Objects.requireNonNull( classFileLocator );
		Objects.requireNonNull( coreTypePool );
		Objects.requireNonNull( cacheProvider );
		return new ModelTypePool( cacheProvider, new OverridingClassFileLocator( classFileLocator ), coreTypePool );
	}

	@Override
	protected Resolution doDescribe(final String name) {
		final Resolution resolution = resolutions.get( name );
		if ( resolution != null ) {
			return resolution;
		}
		else {
			return resolutions.computeIfAbsent( name, super::doDescribe );
		}
	}

	@Override
	public void registerClassNameAndBytes(final String className, final byte[] bytes) {
		locator.put( className, new ClassFileLocator.Resolution.Explicit( Objects.requireNonNull( bytes ) ) );
	}

	@Override
	public void deregisterClassNameAndBytes(final String className) {
		locator.remove( className );
	}

	@Override
	public ClassFileLocator asClassFileLocator() {
		return locator;
	}

	/**
	 * Based on https://github.com/quarkusio/quarkus/blob/main/extensions/hibernate-orm/deployment/src/main/java/io/quarkus/hibernate/orm/deployment/integration/QuarkusClassFileLocator.java#L19
	 */
	private static final class ThreadsafeLocator implements EnhancerClassLocator {
		private final ClassFileLocator classFileLocator;
		final ThreadLocal<EnhancerClassLocator> localLocator;

		private ThreadsafeLocator() {
			classFileLocator = null;
			localLocator = null;
		}
		private ThreadsafeLocator(ClassFileLocator of) {
			classFileLocator = of;
			localLocator = ThreadLocal
			                .withInitial(() -> ModelTypePool.buildModelTypePool(classFileLocator,
									new CoreTypePool() ));
		}

		public static EnhancerClassLocator buildModelTypePool(ClassLoader classLoader) {
			return new ThreadsafeLocator( ClassFileLocator.ForClassLoader.of( classLoader ) );
		}

        @Override
        public void registerClassNameAndBytes(String s, byte[] bytes) {
            localLocator.get().registerClassNameAndBytes(s, bytes);
        }

        @Override
        public void deregisterClassNameAndBytes(String s) {
            localLocator.get().deregisterClassNameAndBytes(s);
        }

        @Override
        public ClassFileLocator asClassFileLocator() {
            return localLocator.get().asClassFileLocator();
        }

        @Override
        public Resolution describe(String s) {
            return localLocator.get().describe(s);
        }

        @Override
        public void clear() {
            //not essential as it gets discarded, but could help:
            localLocator.get().clear();
            localLocator.remove();
        }
    }
}
