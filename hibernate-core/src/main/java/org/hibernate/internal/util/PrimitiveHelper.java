/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

/**
 * Utility class for handling primitive types.
 * @author Gavin King
 * @since 8.0
 */
public class PrimitiveHelper {

	public static Class<?> boxedType(Class<?> type) {
		if ( !type.isPrimitive() ) {
			return type;
		}
		else if ( type == boolean.class ) {
			return Boolean.class;
		}
		else if ( type == byte.class ) {
			return Byte.class;
		}
		else if ( type == char.class ) {
			return Character.class;
		}
		else if ( type == double.class ) {
			return Double.class;
		}
		else if ( type == float.class ) {
			return Float.class;
		}
		else if ( type == int.class ) {
			return Integer.class;
		}
		else if ( type == long.class ) {
			return Long.class;
		}
		else if ( type == short.class ) {
			return Short.class;
		}
		else {
			return Void.class;
		}
	}

}
