/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class Compatibility {
	private Compatibility() {
	}

	private static final Map<Class<?>,Class<?>> primitiveToWrapper;
	private static final Map<Class<?>,Class<?>> wrapperToPrimitive;
	static {
		primitiveToWrapper = new ConcurrentHashMap<>();
		wrapperToPrimitive = new ConcurrentHashMap<>();
		map( boolean.class, Boolean.class );
		map( char.class, Character.class );
		map( byte.class, Byte.class );
		map( short.class, Short.class );
		map( int.class, Integer.class );
		map( long.class, Long.class );
		map( float.class, Float.class );
		map( double.class, Double.class );
	}

	private static void map(Class<?> primitive, Class<?> wrapper) {
		primitiveToWrapper.put( primitive, wrapper );
		wrapperToPrimitive.put( wrapper, primitive );
	}

	public static boolean isWrapper(Class<?> potentialWrapper) {
		return wrapperToPrimitive.containsKey( potentialWrapper );
	}

	public static Class<?> primitiveEquivalent(Class<?> potentialWrapper) {
		assert isWrapper( potentialWrapper );
		return castNonNull( wrapperToPrimitive.get( potentialWrapper ) );
	}

	public static Class<?> wrapperEquivalent(Class<?> primitive) {
		assert primitive.isPrimitive();
		return castNonNull( primitiveToWrapper.get( primitive ) );
	}

	public static boolean areAssignmentCompatible(Class<?> to, Class<?> from) {
		assert to != null;
		assert from != null;

		if ( from == Void.class && !to.isPrimitive() ) {
			// treat Void as the bottom type, the class of null
			return true;
		}

		if ( to.isAssignableFrom( from ) ) {
			return true;
		}

		// if to/from are primitive or primitive wrappers we need to check a little deeper
		if ( to.isPrimitive() ) {
			if ( from.isPrimitive() ) {
				return areAssignmentCompatiblePrimitive( to, from );
			}
			else if ( isWrapper( from ) ) {
				return areAssignmentCompatiblePrimitive( to, primitiveEquivalent( from ) );
			}
		}

		return false;
	}

	private static boolean areAssignmentCompatiblePrimitive(Class<?> to, Class<?> from) {
		assert to != null;
		assert from != null;

		assert to.isPrimitive();
		assert from.isPrimitive();

		// technically any number can be assigned to any other number, although potentially with loss of precision

		if ( to == boolean.class ) {
			return from == boolean.class;
		}
		else if ( to == char.class ) {
			return from == char.class;
		}
		else if ( to == byte.class ) {
			return from == byte.class;
		}
		else if ( isIntegralTypePrimitive( to ) ) {
			return from == byte.class
				|| isCompatibleIntegralTypePrimitive( to, from )
				// this would for sure cause loss of precision
				|| isFloatingTypePrimitive( from );
		}
		else if ( isFloatingTypePrimitive( to ) ) {
			return from == byte.class
				|| isIntegralTypePrimitive( from )
				|| isCompatibleFloatingTypePrimitive( to, from );
		}

		return false;
	}

	public static boolean isIntegralType(Class<?> potentialIntegral) {
		if ( potentialIntegral.isPrimitive() ) {
			return isIntegralTypePrimitive( potentialIntegral );
		}

		return isWrapper( potentialIntegral )
			&& isIntegralTypePrimitive( primitiveEquivalent( potentialIntegral ) );

	}

	private static boolean isIntegralTypePrimitive(Class<?> potentialIntegral) {
		assert potentialIntegral.isPrimitive();

		return potentialIntegral == short.class
			|| potentialIntegral == int.class
			|| potentialIntegral == long.class;
	}

	private static boolean isCompatibleIntegralTypePrimitive(Class<?> to, Class<?> from) {
		assert isIntegralTypePrimitive( to );
		assert from.isPrimitive();

		if ( to == short.class ) {
			return from == short.class;
		}
		else if ( to == int.class ) {
			return from == short.class
				|| from == int.class;
		}
		else {
			return isIntegralTypePrimitive( from );
		}
	}

	public static boolean isFloatingType(Class<?> potentialFloating) {
		if ( potentialFloating.isPrimitive() ) {
			return isFloatingTypePrimitive( potentialFloating );
		}

		return isWrapper( potentialFloating )
			&& isFloatingTypePrimitive( primitiveEquivalent( potentialFloating ) );

	}

	private static boolean isFloatingTypePrimitive(Class<?> potentialFloating) {
		assert potentialFloating.isPrimitive();

		return potentialFloating == float.class
			|| potentialFloating == double.class;
	}

	private static boolean isCompatibleFloatingTypePrimitive(Class<?> to, Class<?> from) {
		assert isFloatingTypePrimitive( to );
		assert from.isPrimitive();

		return to == float.class ? from == float.class : isFloatingTypePrimitive( from );
	}
}
