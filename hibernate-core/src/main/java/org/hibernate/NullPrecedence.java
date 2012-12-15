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

	public static NullPrecedence parse(String type) {
		if ( "none".equalsIgnoreCase( type ) ) {
			return NullPrecedence.NONE;
		}
		else if ( "first".equalsIgnoreCase( type ) ) {
			return NullPrecedence.FIRST;
		}
		else if ( "last".equalsIgnoreCase( type ) ) {
			return NullPrecedence.LAST;
		}
		return null;
	}

	public static NullPrecedence parse(String type, NullPrecedence defaultValue) {
		final NullPrecedence value = parse( type );
		return value != null ? value : defaultValue;
	}
}
