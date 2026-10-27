package org.hibernate.engine.config.spi;

import jakarta.annotation.Nonnull;

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

	public static @Nonnull Boolean asBoolean(@Nonnull Object value) {
		return value instanceof Boolean bool
				? bool
				: parseBoolean( value.toString() );
	}

	public static final Converter<String> STRING = StandardConverters::asString;

	public static @Nonnull String asString(@Nonnull Object value) {
		return value.toString();
	}

	public static final Converter<Integer> INTEGER = StandardConverters::asInteger;

	public static @Nonnull Integer asInteger(@Nonnull Object value) {
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
