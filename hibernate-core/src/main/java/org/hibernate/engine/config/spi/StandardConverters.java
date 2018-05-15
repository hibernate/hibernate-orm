/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.config.spi;


import static org.hibernate.engine.config.spi.ConfigurationService.Converter;

/**
 * Standard set of setting converters
 *
 * @author Steve Ebersole
 */
public class StandardConverters {
	public static final Converter<Boolean> BOOLEAN = value -> {
		if ( value == null ) {
			throw new IllegalArgumentException( "Null value passed to convert" );
		}

		return Boolean.class.isInstance( value )
				? Boolean.class.cast( value )
				: Boolean.parseBoolean( value.toString() );
	};

	public static final Converter<String> STRING = value -> {
		if ( value == null ) {
			throw new IllegalArgumentException( "Null value passed to convert" );
		}

		return value.toString();
	};

	public static final Converter<Integer> INTEGER = value -> {
		if ( value == null ) {
			throw new IllegalArgumentException( "Null value passed to convert" );
		}

		if ( value instanceof Number ) {
			return ( (Number) value ).intValue();
		}

		return Integer.parseInt( value.toString() );
	};

	/**
	 * Disallow direct instantiation
	 */
	private StandardConverters() {
	}
}
