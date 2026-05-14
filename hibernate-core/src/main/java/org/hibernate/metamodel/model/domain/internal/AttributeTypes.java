/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.temporal.Temporal;
import java.util.function.Predicate;

import static org.hibernate.internal.util.type.PrimitiveWrappers.canonicalize;

final class AttributeTypes {
	enum Classification {
		TEXT,
		BOOLEAN,
		NUMERIC,
		TEMPORAL,
		COMPARABLE,
		BASIC
	}

	static Classification classification(Class<?> javaType) {
		final var wrapperJavaType = canonicalize( javaType );
		if ( wrapperJavaType == String.class ) {
			return Classification.TEXT;
		}
		else if ( wrapperJavaType == Boolean.class ) {
			return Classification.BOOLEAN;
		}
		else if ( Number.class.isAssignableFrom( wrapperJavaType )
					&& isComparableToSelf( wrapperJavaType ) ) {
			return Classification.NUMERIC;
		}
		else if ( Temporal.class.isAssignableFrom( wrapperJavaType )
					&& isComparableToSuper( wrapperJavaType ) ) {
			return Classification.TEMPORAL;
		}
		else if ( isComparableToSuper( wrapperJavaType ) ) {
			return Classification.COMPARABLE;
		}
		else {
			return Classification.BASIC;
		}
	}

	private static boolean isComparableToSelf(Class<?> javaType) {
		return isComparableTo( javaType, comparableType -> comparableType == javaType );
	}

	private static boolean isComparableToSuper(Class<?> javaType) {
		return isComparableTo( javaType, comparableType -> comparableType.isAssignableFrom( javaType ) );
	}

	private static boolean isComparableTo(Class<?> javaType, Predicate<Class<?>> predicate) {
		if ( javaType.isEnum() ) {
			return true;
		}
		for ( Type genericInterface : javaType.getGenericInterfaces() ) {
			if ( isComparableTo( genericInterface, predicate ) ) {
				return true;
			}
		}
		final Type genericSuperclass = javaType.getGenericSuperclass();
		if ( isComparableTo( genericSuperclass, predicate ) ) {
			return true;
		}
		final Class<?> superclass = javaType.getSuperclass();
		return superclass != null && isComparableTo( superclass, predicate );
	}

	private static boolean isComparableTo(Type type, Predicate<Class<?>> predicate) {
		if ( type instanceof ParameterizedType parameterizedType ) {
			final Type rawType = parameterizedType.getRawType();
			if ( rawType == Comparable.class ) {
				final Type comparableType = parameterizedType.getActualTypeArguments()[0];
				if ( comparableType instanceof Class<?> comparableClass ) {
					return predicate.test( comparableClass );
				}
				else if ( comparableType instanceof ParameterizedType comparableParameterizedType
						&& comparableParameterizedType.getRawType() instanceof Class<?> comparableClass ) {
					return predicate.test( comparableClass );
				}
			}
			else if ( rawType instanceof Class<?> rawClass ) {
				return isComparableTo( rawClass, predicate );
			}
		}
		else if ( type instanceof Class<?> typeClass ) {
			return isComparableTo( typeClass, predicate );
		}
		return false;
	}
}
