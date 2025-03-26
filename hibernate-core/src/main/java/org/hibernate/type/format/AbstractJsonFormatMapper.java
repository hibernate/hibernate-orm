/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.lang.reflect.Type;

/**
 * @author Yanming Zhou
 */
public abstract class AbstractJsonFormatMapper implements FormatMapper {

	@SuppressWarnings("unchecked")
	@Override
	public final <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		final Type type = javaType.getJavaType();
		if ( type == String.class || type == Object.class ) {
			return (T) charSequence.toString();
		}
		return fromString( charSequence, type );
	}

	@Override
	public final <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		final Type type = javaType.getJavaType();
		if ( type == String.class || type == Object.class ) {
			return (String) value;
		}
		return toString( value, type );
	}

	protected abstract <T> T fromString(CharSequence charSequence, Type type);

	protected abstract <T> String toString(T value, Type type);

}
