/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpamodelgen.util;

/**
 * @author Hardy Ferentschik
 */
public final class StringUtil {
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
		if ( name == null ) {
			return false;
		}
		return checkPropertyName( name, PROPERTY_PREFIX_GET )
				|| checkPropertyName( name, PROPERTY_PREFIX_IS )
				|| checkPropertyName( name, PROPERTY_PREFIX_HAS );
	}

	private static boolean checkPropertyName(String name, String prefix) {
		if ( !name.startsWith( prefix ) ) {
			return false;
		}

		// the name has to start with the prefix and have at least one more character
		if ( name.length() < prefix.length() + 1 ) {
			return false;
		}

		if ( !Character.isUpperCase( name.charAt( prefix.length() ) ) ) {
			return false;
		}

		return true;
	}

	public static String getPropertyName(String name) {
		if ( !isPropertyName( name ) ) {
			return null;
		}

		String tmp = name;
		if ( name.startsWith( PROPERTY_PREFIX_GET ) ) {
			tmp = name.replaceFirst( PROPERTY_PREFIX_GET, "" );
		}
		else if ( name.startsWith( PROPERTY_PREFIX_IS ) ) {
			tmp = name.replaceFirst( PROPERTY_PREFIX_IS, "" );
		}
		else if ( name.startsWith( PROPERTY_PREFIX_HAS ) ) {
			tmp = name.replaceFirst( PROPERTY_PREFIX_HAS, "" );
		}
		return tmp.substring( 0, 1 ).toLowerCase() + tmp.substring( 1 );
	}
}


