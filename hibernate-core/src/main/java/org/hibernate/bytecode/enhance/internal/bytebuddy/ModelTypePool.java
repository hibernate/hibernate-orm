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
	private final SafeCacheProvider poolCache;

	private ModelTypePool(SafeCacheProvider cacheProvider, OverridingClassFileLocator classFileLocator, CoreTypePool parent) {
		super( cacheProvider, classFileLocator, ReaderMode.FAST, parent );
		this.poolCache = cacheProvider;
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
		return buildModelTypePool( ClassFileLocator.ForClassLoader.of( classLoader ) );
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
		return buildModelTypePool( classFileLocator, coreTypePool, new SafeCacheProvider() );
	}

	/**
	 * The more advanced strategy to construct a new ModelTypePool, allowing customization of all its aspects.
	 * @param classFileLocator
	 * @param coreTypePool
	 * @param cacheProvider
	 * @return
	 */
	public static EnhancerClassLocator buildModelTypePool(ClassFileLocator classFileLocator, CoreTypePool coreTypePool, SafeCacheProvider cacheProvider) {
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
		//Very important: ensure the registered override is actually effective in case this class
		//was already resolved in the recent past; this could have happened for example as a side effect
		//of symbol resolution during enhancement of a different class, or very simply when attempting
		//to re-enhanced the same class - which happens frequently in WildFly because of the class transformers
		//being triggered concurrently by multiple parallel deployments.
		resolutions.remove( className );
		poolCache.remove( className );
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

}
