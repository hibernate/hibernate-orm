/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.AttributeConverter;

public class GenericsHelper {

	public static ParameterizedType extractParameterizedType(Type base) {
		if ( base == null ) {
			return null;
		}

		final Class<?> clazz = extractClass( base );
		if ( clazz == null ) {
			return null;
		}

		final List<Type> types = new ArrayList<>();
		types.add( clazz.getGenericSuperclass() );
		types.addAll( Arrays.asList( clazz.getGenericInterfaces() ) );

		for ( Type type : types ) {
			type = resolveType( type, base );
			if ( type instanceof ParameterizedType ) {
				final ParameterizedType parameterizedType = (ParameterizedType) type;
				if ( AttributeConverter.class.equals( parameterizedType.getRawType() ) ) {
					return parameterizedType;
				}
			}

			final ParameterizedType parameterizedType = extractParameterizedType( type );
			if ( parameterizedType != null ) {
				return parameterizedType;
			}
		}

		return null;
	}

	private static Type resolveTypeVariable(TypeVariable<?> typeVariable, ParameterizedType context) {
		final Class<?> clazz = extractClass( context.getRawType() );
		if ( clazz == null ) {
			return null;
		}

		final TypeVariable<?>[] typeParameters = clazz.getTypeParameters();
		for ( int idx = 0; idx < typeParameters.length; idx++ ) {
			if ( typeVariable.getName().equals( typeParameters[idx].getName() ) ) {
				return resolveType( context.getActualTypeArguments()[idx], context );
			}
		}

		return typeVariable;
	}

	public static Class<?> extractClass(Type type) {
		if ( type instanceof Class ) {
			return (Class<?>) type;
		}
		else if ( type instanceof ParameterizedType ) {
			return extractClass( ( (ParameterizedType) type ).getRawType() );
		}
		return null;
	}

	private static Type resolveType(Type target, Type context) {
		if ( target instanceof ParameterizedType ) {
			return resolveParameterizedType( (ParameterizedType) target, context );
		}
		else if ( target instanceof TypeVariable ) {
			return resolveTypeVariable( (TypeVariable<?>) target, (ParameterizedType) context );
		}
		return target;
	}

	private static ParameterizedType resolveParameterizedType(final ParameterizedType parameterizedType, Type context) {
		final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

		final Type[] resolvedTypeArguments = new Type[actualTypeArguments.length];
		for ( int idx = 0; idx < actualTypeArguments.length; idx++ ) {
			resolvedTypeArguments[idx] = resolveType( actualTypeArguments[idx], context );
		}
		return new ParameterizedType() {

			@Override
			public Type[] getActualTypeArguments() {
				return resolvedTypeArguments;
			}

			@Override
			public Type getRawType() {
				return parameterizedType.getRawType();
			}

			@Override
			public Type getOwnerType() {
				return parameterizedType.getOwnerType();
			}

		};
	}
}
