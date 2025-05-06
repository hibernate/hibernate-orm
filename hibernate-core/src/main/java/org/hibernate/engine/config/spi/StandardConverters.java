/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.config.spi;



import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static org.hibernate.engine.config.spi.ConfigurationService.Converter;

/**
 * Standard set of setting converters.
 *
 * @author Steve Ebersole
 */
public class StandardConverters {
	public static final Converter<Boolean> BOOLEAN = StandardConverters::asBoolean;

	public static Boolean asBoolean(Object value) {
		return value instanceof Boolean bool
				? bool
				: parseBoolean( value.toString() );
	}

	public static final Converter<String> STRING = StandardConverters::asString;

	public static String asString(Object value) {
		return value.toString();
	}

	public static final Converter<Integer> INTEGER = StandardConverters::asInteger;

	public static Integer asInteger(Object value) {
		return value instanceof Number number
				? number.intValue()
				: parseInt( value.toString() );
	}

	/**
	 * Disallow direct instantiation
	 */
	private StandardConverters() {
	}
}
