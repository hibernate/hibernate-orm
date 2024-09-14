/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.config.spi;



import static org.hibernate.engine.config.spi.ConfigurationService.Converter;

/**
 * Standard set of setting converters.
 *
 * @author Steve Ebersole
 */
public class StandardConverters {
	public static final Converter<Boolean> BOOLEAN = StandardConverters::asBoolean;

	public static Boolean asBoolean(Object value) {
		return value instanceof Boolean
				? (Boolean) value
				: Boolean.parseBoolean( value.toString() );
	}

	public static final Converter<String> STRING = StandardConverters::asString;

	public static String asString(Object value) {
		return value.toString();
	}

	public static final Converter<Integer> INTEGER = StandardConverters::asInteger;

	public static Integer asInteger(Object value) {
		if ( value instanceof Number ) {
			return ( (Number) value ).intValue();
		}

		return Integer.parseInt( value.toString() );
	}

	/**
	 * Disallow direct instantiation
	 */
	private StandardConverters() {
	}
}
