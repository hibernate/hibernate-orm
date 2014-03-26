/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.tuple.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.service.ServiceRegistry;

/**
 * A registry allowing users to define the default {@link ComponentTuplizer} class to use per {@link EntityMode}.
 *
 * @author Steve Ebersole
 */
public class ComponentTuplizerFactory implements Serializable {
	private static final Class[] COMPONENT_TUP_CTOR_SIG_NEW = new Class[] {
			ServiceRegistry.class,
			EmbeddableBinding.class,
			boolean.class
	};

	/**
	 * Construct an instance of the given tuplizer class.
	 *
	 * @param tuplizerClassName The name of the tuplizer class to instantiate
	 * @param metadata The metadata for the component.
	 *
	 * @return The instantiated tuplizer
	 *
	 * @throws HibernateException If class name cannot be resolved to a class reference, or if the
	 * {@link Constructor#newInstance} call fails.
	 */
	@SuppressWarnings({ "unchecked" })
	public ComponentTuplizer constructTuplizer(
			ServiceRegistry serviceRegistry,
			String tuplizerClassName,
			EmbeddableBinding metadata,
			boolean isIdentifierMapper) {
		try {
			final Class<? extends ComponentTuplizer> tuplizerClass = ReflectHelper.classForName( tuplizerClassName );
			assert isComponentTuplizerImplementor( tuplizerClass ) : "Specified ComponentTuplizer class does not implement ComponentTuplizer";

			return constructTuplizer( tuplizerClass, serviceRegistry, metadata, isIdentifierMapper );
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Could not locate specified tuplizer class [" + tuplizerClassName + "]" );
		}
	}

	private boolean isComponentTuplizerImplementor(Class tuplizerClass) {
		return ReflectHelper.implementsInterface( tuplizerClass, ComponentTuplizer.class );
	}

	private boolean hasProperConstructor(Class tuplizerClass) {
		return getProperConstructor( tuplizerClass, COMPONENT_TUP_CTOR_SIG_NEW ) != null;
	}

	/**
	 * Construct an instance of the given tuplizer class.
	 *
	 * @param tuplizerClass The tuplizer class to instantiate
	 * @param metadata The metadata for the component.
	 *
	 * @return The instantiated tuplizer
	 *
	 * @throws HibernateException if the {@link java.lang.reflect.Constructor#newInstance} call fails.
	 */
	public ComponentTuplizer constructTuplizer(
			Class<? extends ComponentTuplizer> tuplizerClass,
			ServiceRegistry serviceRegistry,
			EmbeddableBinding metadata,
			boolean isIdentifierMapper) {
		final Constructor<? extends ComponentTuplizer> constructor = getProperConstructor(
				tuplizerClass,
				COMPONENT_TUP_CTOR_SIG_NEW
		);
		assert hasProperConstructor( tuplizerClass ) : "Specified ComponentTuplizer class [" + tuplizerClass + "] did not have proper constructor";

		try {
			return constructor.newInstance( serviceRegistry, metadata, isIdentifierMapper );
		}
		catch ( Throwable t ) {
			throw new HibernateException( "Unable to instantiate default tuplizer [" + tuplizerClass.getName() + "]", t );
		}
	}

	/**
	 * Construct am instance of the default tuplizer for the given entity-mode.
	 *
	 * @param entityMode The entity mode for which to build a default tuplizer.
	 * @param metadata The metadata for the component.
	 *
	 * @return The instantiated tuplizer
	 *
	 * @throws HibernateException If no default tuplizer found for that entity-mode; may be re-thrown from
	 * {@link #constructTuplizer} too.
	 */
	public ComponentTuplizer constructDefaultTuplizer(
			EntityMode entityMode,
			ServiceRegistry serviceRegistry,
			EmbeddableBinding metadata,
			boolean isIdentifierMapper) {
		final Class<? extends ComponentTuplizer> tuplizerClass = determineTuplizerClass( entityMode );
		return constructTuplizer( tuplizerClass, serviceRegistry, metadata, isIdentifierMapper );
	}

	private Class<? extends ComponentTuplizer> determineTuplizerClass(EntityMode entityMode) {
		switch ( entityMode ) {
			case MAP: {
				return DynamicMapComponentTuplizer.class;
			}
			case POJO: {
				return PojoComponentTuplizer.class;
			}
			default: {
				throw new IllegalArgumentException( "Unknown EntityMode : " + entityMode );
			}
		}
	}

	private Constructor<? extends ComponentTuplizer> getProperConstructor(
			Class<? extends ComponentTuplizer> clazz,
			Class[] clazzConstructorSignature) {
		Constructor<? extends ComponentTuplizer> constructor = null;
		try {
			constructor = clazz.getDeclaredConstructor( clazzConstructorSignature );
			try {
				constructor.setAccessible( true );
			}
			catch ( SecurityException e ) {
				constructor = null;
			}
		}
		catch ( NoSuchMethodException ignore ) {
		}

		return constructor;
	}
}
