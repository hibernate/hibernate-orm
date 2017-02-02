/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	@Override
	public BasicType basic(String name) {
		return typeResolver.basic( name );
	}

	@Override
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

	@Override
	public Type heuristicType(String name) {
		return typeResolver.heuristicType( name );
	}

	@Override
	public Type entity(Class entityClass) {
		return entity( entityClass.getName() );
	}

	@Override
	public Type entity(String entityName) {
		return typeResolver.getTypeFactory().manyToOne( entityName );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Type custom(Class userTypeClass) {
		return custom( userTypeClass, null );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Type custom(Class userTypeClass, Properties parameters) {
		if ( CompositeUserType.class.isAssignableFrom( userTypeClass ) ) {
			return typeResolver.getTypeFactory().customComponent( userTypeClass, parameters );
		}
		else {
			return typeResolver.getTypeFactory().custom( userTypeClass, parameters );
		}
	}

	@Override
	public Type any(Type metaType, Type identifierType) {
		return typeResolver.getTypeFactory().any( metaType, identifierType );
	}
}
