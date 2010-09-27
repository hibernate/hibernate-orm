/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// $Id$

package org.hibernate.jpamodelgen.util;

/**
 * @author Hardy Ferentschik
 */
public class StringUtil {
	private static final String NAME_SEPARATOR = ".";
	private static final String PROPERTY_PREFIX_GET = "get";
	private static final String PROPERTY_PREFIX_IS = "is";
	private static final String PROPERTY_PREFIX_HAS = "has";

	private StringUtil() {
	}

	public static String determineFullyQualifiedClassName(String defaultPackage, String name) {
		if ( isFullyQualified( name ) ) {
			return name;
		}
		else {
			return defaultPackage + NAME_SEPARATOR + name;
		}
	}

	public static boolean isFullyQualified(String name) {
		return name.contains( NAME_SEPARATOR );
	}

	public static String packageNameFromFqcn(String fqcn) {
		return fqcn.substring( 0, fqcn.lastIndexOf( NAME_SEPARATOR ) );
	}

	public static String classNameFromFqcn(String fqcn) {
		return fqcn.substring( fqcn.lastIndexOf( NAME_SEPARATOR ) + 1 );
	}

	public static boolean isPropertyName(String name) {
		return name.startsWith( PROPERTY_PREFIX_GET ) || name.startsWith( PROPERTY_PREFIX_IS ) || name.startsWith(
				PROPERTY_PREFIX_HAS
		);
	}

	public static String getPropertyName(String name) {
		if ( !isPropertyName( name ) ) {
			return null;
		}

		if ( name.startsWith( PROPERTY_PREFIX_GET ) ) {
			name = name.replaceFirst( PROPERTY_PREFIX_GET, "" );
		}
		else if ( name.startsWith( PROPERTY_PREFIX_IS ) ) {
			name = name.replaceFirst( PROPERTY_PREFIX_IS, "" );
		}
		else if ( name.startsWith( PROPERTY_PREFIX_HAS ) ) {
			name = name.replaceFirst( PROPERTY_PREFIX_HAS, "" );
		}
		return name.substring(0,1).toLowerCase() + name.substring(1);
	}
}


