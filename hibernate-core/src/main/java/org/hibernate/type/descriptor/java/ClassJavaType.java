/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
public class ClassJavaType extends AbstractClassJavaType<Class> {
	public static final ClassJavaType INSTANCE = new ClassJavaType();

	public ClassJavaType() {
		super( Class.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	public String toString(Class value) {
		return value.getName();
	}

	public Class fromString(CharSequence string) {
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

	@SuppressWarnings("unchecked")
	public <X> X unwrap(Class value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Class.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> Class wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof Class) {
			return (Class) value;
		}
		if (value instanceof CharSequence) {
			return fromString( (CharSequence) value );
		}
		throw unknownWrap( value.getClass() );
	}

}
