/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for various annotation reflection operations.
 *
 * @author Yanming Zhou
 */
public final class AnnotationUtil {

	private static final List<String> excludedMethodNames = List.of( "hashCode", "toString", "annotationType" );

	/**
	 * Retrieve the given annotation's attributes as a {@link Map}, preserving all
	 * attribute types.
	 * @param annotation the annotation to retrieve the attributes for
	 * @return the Map of annotation attributes, with attribute names as keys and
	 * corresponding attribute values as values (never {@code null})
	 */
	public static Map<String, Object> getAttributes( Annotation annotation ) {
		try {
			Map<String, Object> attributes = new HashMap<>();
			for ( Method m : annotation.getClass().getDeclaredMethods() ) {
				if ( m.getParameterCount() == 0 && !excludedMethodNames.contains( m.getName() ) ) {
					attributes.put( m.getName(), m.invoke( annotation ) );
				}
			}
			return attributes;
		}
		catch ( Exception ex ) {
			throw new RuntimeException( ex );
		}
	}
}
