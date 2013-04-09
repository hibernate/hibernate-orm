/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate;

/**
 * Defines precedence of null values within {@code ORDER BY} clause.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public enum NullPrecedence {
	/**
	 * Null precedence not specified. Relies on the RDBMS implementation.
	 */
	NONE,

	/**
	 * Null values appear at the beginning of the sorted collection.
	 */
	FIRST,

	/**
	 * Null values appear at the end of the sorted collection.
	 */
	LAST;

	/**
	 * Interprets a string representation of a NullPrecedence, returning {@code null} by default.  For
	 * alternative default handling, see {@link #parse(String, NullPrecedence)}
	 *
	 * @param name The String representation to interpret
	 *
	 * @return The recognized NullPrecedence, or {@code null}
	 */
	public static NullPrecedence parse(String name) {
		if ( "none".equalsIgnoreCase( name ) ) {
			return NullPrecedence.NONE;
		}
		else if ( "first".equalsIgnoreCase( name ) ) {
			return NullPrecedence.FIRST;
		}
		else if ( "last".equalsIgnoreCase( name ) ) {
			return NullPrecedence.LAST;
		}
		return null;
	}

	/**
	 * Interprets a string representation of a NullPrecedence, returning the specified default if not recognized.
	 *
	 * @param name The String representation to interpret
	 * @param defaultValue The default value to use
	 *
	 * @return The recognized NullPrecedence, or {@code defaultValue}.
	 */
	public static NullPrecedence parse(String name, NullPrecedence defaultValue) {
		final NullPrecedence value = parse( name );
		return value != null ? value : defaultValue;
	}
}
