/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;

/**
 * A TypePool which only loads, and caches, types whose package
 * name starts with certain chosen prefixes.
 * The default is to only load classes whose package names start with
 * either "jakarta." or "java.".
 * This allows to reuse these caches independently from application
 * code and classloader changes, as during enhancement we frequently
 * encounter such symbols as well, for example triggered by JPA annotations
 * or properties mapped via standard java types and collections.
 * Symbols resolved by this pool are backed by loaded classes from
 * ORM's classloader.
 */
public class CoreTypePool extends TypePool.AbstractBase implements TypePool {

	private final ClassLoader hibernateClassLoader = CoreTypePool.class.getClassLoader();
	private final ConcurrentHashMap<String, Resolution> resolutions = new ConcurrentHashMap<>();
	private final CorePrefixFilter acceptedPrefixes;

	/**
	 * Construct a new {@link CoreTypePool} with its default configuration.
	 *
	 * @see CorePrefixFilter
	 */
	public CoreTypePool() {
		this( CorePrefixFilter.DEFAULT_INSTANCE );
	}

	/**
	 * Construct a new {@link CoreTypePool} with a choice of which prefixes
	 * for fully qualified classnames will be loaded by this {@link TypePool}.
	 *
	 * @deprecated used by Quarkus
	 */
	@Deprecated
	public CoreTypePool(final String... acceptedPrefixes) {
		this( new CorePrefixFilter( acceptedPrefixes ) );
	}

	public CoreTypePool(CorePrefixFilter acceptedPrefixes) {
		//While we implement a cache in this class we also want to enable
		//ByteBuddy's default caching mechanism as it will cache the more
		//useful output of the parsing and introspection of such types.
		super( new CoreCacheProvider( acceptedPrefixes ) );
		this.acceptedPrefixes = Objects.requireNonNull( acceptedPrefixes );
	}

	@Override
	protected Resolution doDescribe(final String name) {
		if ( acceptedPrefixes.isCoreClassName( name ) ) {
			final Resolution resolution = resolutions.get( name );
			if ( resolution != null ) {
				return resolution;
			}
			else {
				//We implement this additional layer of caching, which is on top of
				//ByteBuddy's default caching, so as to prevent resolving the same
				//types concurrently from the classloader.
				//This is merely an efficiency improvement and will NOT provide a
				//strict guarantee of symbols being resolved exactly once as there
				//is no SPI within ByteBuddy which would allow this: the point is to
				//make it exceptionally infrequent, which greatly helps with
				//processing of large models.
				return resolutions.computeIfAbsent( name, this::actualResolve );
			}
		}
		else {
			//These are not cached to not leak references to application code names
			return new Resolution.Illegal( name );
		}
	}

	private Resolution actualResolve(final String name) {
		try {
			final Class<?> aClass = Class.forName( name, false, hibernateClassLoader );
			return new TypePool.Resolution.Simple( TypeDescription.ForLoadedType.of( aClass ) );
		}
		catch ( ClassNotFoundException e ) {
			return new Resolution.Illegal( name );
		}
	}

}
