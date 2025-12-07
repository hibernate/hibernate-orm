/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.type;

/**
 * Maps primitive types to their wrapper counterparts.
 *
 * @author Gavin King
 */
public final class PrimitiveWrappers {

	public static <T> Class<T> canonicalize(Class<T> type) {
		if ( type.isPrimitive() ) {
			@SuppressWarnings("unchecked")
			// completely safe, boolean.class is a Class<Boolean>
			final var wrapperClass = (Class<T>) wrapperClass( type );
			return wrapperClass;
		}
		else {
			return type;
		}
	}

	private static Class<?> wrapperClass(Class<?> primitiveClass) {
		return switch ( primitiveClass.getName() ) {
			case "boolean" -> Boolean.class;
			case "char" -> Character.class;
			case "byte" -> Byte.class;
			case "short" -> Short.class;
			case "int" -> Integer.class;
			case "long" -> Long.class;
			case "float" -> Float.class;
			case "double" -> Double.class;
			default -> throw new AssertionError( "Unknown primitive type: " + primitiveClass );
		};
	}

	public static <X> X cast(Class<X> type, Object value) {
		return canonicalize( type ).cast( value );
	}

	public static boolean isInstance(Class<?> type, Object value) {
		return canonicalize( type ).isInstance( value );
	}
}
