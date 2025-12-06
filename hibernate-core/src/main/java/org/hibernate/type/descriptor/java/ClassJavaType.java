/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Descriptor for {@link Class} handling.
 *
 * @author Steve Ebersole
 */
public class ClassJavaType extends AbstractClassJavaType<Class<?>> {
	public static final ClassJavaType INSTANCE = new ClassJavaType();

	@SuppressWarnings({"unchecked", "rawtypes"} )
	public ClassJavaType() {
		super( (Class) Class.class );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Class;
	}

	@Override
	public Class<?> cast(Object value) {
		return (Class<?>) value;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Class<?> value) {
		return value.getName();
	}

	@Override
	public Class<?> fromString(CharSequence string) {
		if ( string == null ) {
			return null;
		}

		try {
			return ReflectHelper.classForName( string.toString() );
		}
		catch ( ClassNotFoundException e ) {
			throw new HibernateException( "Unable to locate named class " + string );
		}
	}

	@Override
	public <X> X unwrap(Class<?> value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Class.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( toString( value ) );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> Class<?> wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Class) {
			return (Class<?>) value;
		}
		if (value instanceof CharSequence) {
			return fromString( (CharSequence) value );
		}
		throw unknownWrap( value.getClass() );
	}

}
