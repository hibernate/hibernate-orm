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
package org.hibernate.internal;
import java.io.Serializable;
import java.util.Properties;

import org.hibernate.TypeHelper;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;
import org.hibernate.usertype.CompositeUserType;

/**
 * Implementation of {@link org.hibernate.TypeHelper}
 *
 * @todo Do we want to cache the results of {@link #entity}, {@link #custom} and {@link #any} ?
 *
 * @author Steve Ebersole
 */
public class TypeLocatorImpl implements TypeHelper, Serializable {
	private final TypeResolver typeResolver;

	public TypeLocatorImpl(TypeResolver typeResolver) {
		this.typeResolver = typeResolver;
	}

	/**
	 * {@inheritDoc}
	 */
	public BasicType basic(String name) {
		return typeResolver.basic( name );
	}

	/**
	 * {@inheritDoc}
	 */
	public BasicType basic(Class javaType) {
		BasicType type = typeResolver.basic( javaType.getName() );
		if ( type == null ) {
			final Class variant = resolvePrimitiveOrPrimitiveWrapperVariantJavaType( javaType );
			if ( variant != null ) {
				type = typeResolver.basic( variant.getName() );
			}
		}
		return type;
	}

	private Class resolvePrimitiveOrPrimitiveWrapperVariantJavaType(Class javaType) {
		// boolean
		if ( Boolean.TYPE.equals( javaType ) ) {
			return Boolean.class;
		}
		if ( Boolean.class.equals( javaType ) ) {
			return Boolean.TYPE;
		}

		// char
		if ( Character.TYPE.equals( javaType ) ) {
			return Character.class;
		}
		if ( Character.class.equals( javaType ) ) {
			return Character.TYPE;
		}

		// byte
		if ( Byte.TYPE.equals( javaType ) ) {
			return Byte.class;
		}
		if ( Byte.class.equals( javaType ) ) {
			return Byte.TYPE;
		}

		// short
		if ( Short.TYPE.equals( javaType ) ) {
			return Short.class;
		}
		if ( Short.class.equals( javaType ) ) {
			return Short.TYPE;
		}

		// int
		if ( Integer.TYPE.equals( javaType ) ) {
			return Integer.class;
		}
		if ( Integer.class.equals( javaType ) ) {
			return Integer.TYPE;
		}

		// long
		if ( Long.TYPE.equals( javaType ) ) {
			return Long.class;
		}
		if ( Long.class.equals( javaType ) ) {
			return Long.TYPE;
		}

		// float
		if ( Float.TYPE.equals( javaType ) ) {
			return Float.class;
		}
		if ( Float.class.equals( javaType ) ) {
			return Float.TYPE;
		}

		// double
		if ( Double.TYPE.equals( javaType ) ) {
			return Double.class;
		}
		if ( Double.class.equals( javaType ) ) {
			return Double.TYPE;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type heuristicType(String name) {
		return typeResolver.heuristicType( name );
	}

	/**
	 * {@inheritDoc}
	 */
	public Type entity(Class entityClass) {
		return entity( entityClass.getName() );
	}

	/**
	 * {@inheritDoc}
	 */
	public Type entity(String entityName) {
		return typeResolver.getTypeFactory().manyToOne( entityName );
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public Type custom(Class userTypeClass) {
		return custom( userTypeClass, null );
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public Type custom(Class userTypeClass, Properties parameters) {
		if ( CompositeUserType.class.isAssignableFrom( userTypeClass ) ) {
			return typeResolver.getTypeFactory().customComponent( userTypeClass, parameters );
		}
		else {
			return typeResolver.getTypeFactory().custom( userTypeClass, parameters );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Type any(Type metaType, Type identifierType) {
		return typeResolver.getTypeFactory().any( metaType, identifierType );
	}
}
