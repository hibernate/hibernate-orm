/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenericsHelper {

	public static ParameterizedType extractParameterizedType(Type base, Class<?> genericType) {
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
			if ( type instanceof ParameterizedType parameterizedType ) {
				if ( genericType.equals( parameterizedType.getRawType() ) ) {
					return parameterizedType;
				}
			}

			final ParameterizedType parameterizedType = extractParameterizedType( type, genericType );
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
		if ( type instanceof Class<?> clazz ) {
			return clazz;
		}
		else if ( type instanceof ParameterizedType parameterizedType ) {
			return extractClass( parameterizedType.getRawType() );
		}
		return null;
	}

	private static Type resolveType(Type target, Type context) {
		if ( target instanceof ParameterizedType parameterizedType ) {
			return resolveParameterizedType( parameterizedType, context );
		}
		else if ( target instanceof TypeVariable<?> typeVariable ) {
			return resolveTypeVariable( typeVariable, (ParameterizedType) context );
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
