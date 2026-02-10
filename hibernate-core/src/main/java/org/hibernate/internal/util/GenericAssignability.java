/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.lang.reflect.*;

/**
 * @author Gavin King
 */
public final class GenericAssignability {

	public static boolean isAssignableFrom(Type to, Type from) {
		return isAssignable( from, to );
	}

	public static boolean isAssignable(Type from, Type to) {
		if ( from.equals( to ) ) {
			return true;
		}

		// Wildcards (target side)
		if ( to instanceof WildcardType wt ) {
			return isAssignableToWildcard( from, wt );
		}

		// Type variables (target side)
		if ( to instanceof TypeVariable<?> tv ) {
			return isAssignableToTypeVariable( from, tv );
		}

		// Generic arrays
		if ( to instanceof GenericArrayType gaTo ) {
			return isAssignableToGenericArray( from, gaTo );
		}

		// From-side wildcard
		if ( from instanceof WildcardType wf ) {
			return isAssignableFromWildcard( wf, to );
		}

		// Parameterized types
		if ( from instanceof ParameterizedType pf &&
			to instanceof ParameterizedType pt ) {
			return isAssignableParameterized( pf, pt );
		}

		// Raw class or array class
		final var fromRaw = rawClass( from );
		final var toRaw = rawClass( to );

		if ( fromRaw != null && toRaw != null ) {
			return toRaw.isAssignableFrom( fromRaw )
				|| isAssignableViaInheritance( from, toRaw );
		}

		return false;
	}

	private static boolean isAssignableToGenericArray(Type from, GenericArrayType to) {
		final var toComponent = to.getGenericComponentType();
		if ( from instanceof GenericArrayType gaFrom ) {
			return isAssignable( gaFrom.getGenericComponentType(), toComponent );
		}
		else if ( from instanceof Class<?> c && c.isArray() ) {
			return isAssignable( c.getComponentType(), toComponent );
		}
		else {
			return false;
		}
	}

	private static boolean isAssignableParameterized(ParameterizedType from, ParameterizedType to) {

		final var fromRaw = (Class<?>) from.getRawType();
		final var toRaw = (Class<?>) to.getRawType();

		if ( toRaw.isAssignableFrom( fromRaw ) ) {
			final var superType = findGenericSuperType( from, toRaw );
			if ( !( superType instanceof ParameterizedType ps ) ) {
				return false;
			}
			else {
				final var fromArgs = ps.getActualTypeArguments();
				final var toArgs = to.getActualTypeArguments();
				if ( fromArgs.length != toArgs.length ) {
					return false;
				}
				for ( int i = 0; i < fromArgs.length; i++ ) {
					if ( !isTypeArgumentAssignable( fromArgs[i], toArgs[i] ) ) {
						return false;
					}
				}
				return true;
			}
		}
		else {
			return false;
		}

	}

	private static boolean isTypeArgumentAssignable(Type from, Type to) {
		return to instanceof WildcardType wt
				? isAssignableToWildcard( from, wt )
				: isAssignable( from, to );
	}

	private static boolean isAssignableViaInheritance(Type from, Class<?> toRaw) {

		final var raw = rawClass( from );
		if ( raw == null ) {
			return false;
		}

		// Check interfaces
		for ( var iface : raw.getGenericInterfaces() ) {
			if ( isAssignable( iface, toRaw ) ) {
				return true;
			}
		}

		// Check superclass
		final var superclass = raw.getGenericSuperclass();
		if ( superclass != null ) {
			return isAssignable( superclass, toRaw );
		}

		return false;
	}

	private static Type findGenericSuperType(Type from, Class<?> target) {
		final var raw = rawClass( from );
		if ( raw == null ) {
			return null;
		}

		if ( raw == target ) {
			return from;
		}

		for ( var iface : raw.getGenericInterfaces() ) {
			final var found = findGenericSuperType( iface, target );
			if ( found != null ) {
				return found;
			}
		}

		final var superclass = raw.getGenericSuperclass();
		if ( superclass != null ) {
			return findGenericSuperType( superclass, target );
		}

		return null;
	}

	private static boolean isAssignableToWildcard(Type from, WildcardType to) {
		for ( var lower : to.getLowerBounds() ) {
			if ( !isAssignable( lower, from ) ) {
				return false;
			}
		}
		for ( var upper : to.getUpperBounds() ) {
			if ( !isAssignable( from, upper ) ) {
				return false;
			}
		}
		return true;
	}

	private static boolean isAssignableFromWildcard(WildcardType from, Type to) {
		for ( var upper : from.getUpperBounds() ) {
			if ( isAssignable( upper, to ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean isAssignableToTypeVariable(Type from, TypeVariable<?> tv) {
		for ( var bound : tv.getBounds() ) {
			if ( !isAssignable( from, bound ) ) {
				return false;
			}
		}
		return true;
	}

	private static Class<?> rawClass(Type type) {
		if ( type instanceof Class<?> c ) {
			return c;
		}
		else if ( type instanceof ParameterizedType pt ) {
			return (Class<?>) pt.getRawType();
		}
		else if ( type instanceof GenericArrayType ga ) {
			return rawClass( ga.getGenericComponentType() ).arrayType();
		}
		else {
			return null;
		}
	}
}
