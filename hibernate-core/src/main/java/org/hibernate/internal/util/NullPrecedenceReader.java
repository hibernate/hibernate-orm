package org.hibernate.internal.util;

import org.hibernate.NullPrecedence;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class NullPrecedenceReader {
	public static NullPrecedence parse(String type) {
		if ( "none".equalsIgnoreCase( type ) ) {
			return NullPrecedence.NONE;
		} else if ( "first".equalsIgnoreCase( type ) ) {
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
