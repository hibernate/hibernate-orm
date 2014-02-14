/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi.domain;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.ValueHolder;

/**
 * Models the naming of a Java type where we may not have access to that type's {@link Class} reference.  Generally
 * speaking this is the case in various hibernate-tools and reverse-engineering use cases.
 *
 * @author Steve Ebersole
 */
public class JavaClassReference {

	// ClassLoaderService.classForName( className ) does not work for primitives, so add mapping
	//       from primitive class names -> class.
	private static final Map<String,Class<?>> primitiveClassesByName = new HashMap<String,Class<?>>();
	static {
		primitiveClassesByName.put("int", Integer.TYPE );
		primitiveClassesByName.put( "long", Long.TYPE );
		primitiveClassesByName.put( "double", Double.TYPE );
		primitiveClassesByName.put( "float", Float.TYPE );
		primitiveClassesByName.put( "bool", Boolean.TYPE );
		primitiveClassesByName.put( "char", Character.TYPE );
		primitiveClassesByName.put( "byte", Byte.TYPE );
		primitiveClassesByName.put( "void", Void.TYPE );
		primitiveClassesByName.put( "short", Short.TYPE );
	}
	private final String name;
	private final ValueHolder<Class<?>> classHolder;

	public JavaClassReference(final String name, final ClassLoaderService classLoaderService) {
		if ( name == null || classLoaderService == null ) {
			throw new IllegalArgumentException( "name and classLoaderService must be non-null." );
		}
		this.name = name;

		final Class<?> primitiveClass = primitiveClassesByName.get( name );
		if ( primitiveClass != null ) {
			this.classHolder = new ValueHolder<Class<?>>( primitiveClass );
		}
		else {
			this.classHolder = new ValueHolder<Class<?>>(
					new ValueHolder.DeferredInitializer<Class<?>>() {
						@Override
						public Class<?> initialize() {
							return classLoaderService.classForName( name );
						}
					}
			);
		}
	}

	public JavaClassReference(Class<?> theClass) {
		if ( theClass == null ) {
			throw new IllegalArgumentException( "theClass must be non-null." );
		}
		this.name = theClass.getName();
		this.classHolder = new ValueHolder<Class<?>>( theClass );
	}

	public String getName() {
		return name;
	}

	public Class<?> getResolvedClass() {
		return classHolder.getValue();
	}

	@Override
	public String toString() {
		return super.toString() + "[name=" + name + "]";
	}
}
