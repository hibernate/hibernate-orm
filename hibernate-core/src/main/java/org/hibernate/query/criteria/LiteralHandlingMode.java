/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import org.hibernate.HibernateException;

/**
 * This enum defines how literals are handled by JPA Criteria.
 *
 * By default ({@code AUTO}), Criteria queries uses bind parameters for any literal that is not a numeric value.
 *
 * However, to increase the likelihood of JDBC statement caching,
 * you might want to use bind parameters for numeric values too.
 * The {@code BIND} mode will use bind variables for any literal value.
 *
 * The {@code INLINE} mode will inline literal values as-is.
 * To prevent SQL injection, never use {@code INLINE} with String variables.
 * Always use constants with the {@code INLINE} mode.
 *
 * @author Vlad Mihalcea
 */
public enum LiteralHandlingMode {

	AUTO,
	BIND,
	INLINE;

	/**
	 * Interpret the configured literalHandlingMode value.
	 * Valid values are either a {@link LiteralHandlingMode} object or its String representation.
	 * For string values, the matching is case insensitive, so you can use either {@code AUTO} or {@code auto}.
	 *
	 * @param literalHandlingMode configured {@link LiteralHandlingMode} representation
	 * @return associated {@link LiteralHandlingMode} object
	 */
	public static LiteralHandlingMode interpret(Object literalHandlingMode) {
		if ( literalHandlingMode == null ) {
			return AUTO;
		}
		else if ( literalHandlingMode instanceof LiteralHandlingMode ) {
			return (LiteralHandlingMode) literalHandlingMode;
		}
		else if ( literalHandlingMode instanceof String ) {
			for ( LiteralHandlingMode value : values() ) {
				if ( value.name().equalsIgnoreCase( (String) literalHandlingMode ) ) {
					return value;
				}
			}
		}
		throw new HibernateException(
				"Unrecognized literal_handling_mode value : " + literalHandlingMode
						+ ".  Supported values include 'auto', 'inline', and 'bind'."
		);
	}
}
