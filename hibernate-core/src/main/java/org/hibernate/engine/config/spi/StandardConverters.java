/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.config.spi;


import static org.hibernate.engine.config.spi.ConfigurationService.Converter;
import static org.hibernate.internal.util.Validator.checkNotNullIAE;

/**
 * Standard set of setting converters
 *
 * @author Steve Ebersole
 */
public class StandardConverters {
	public static final Converter<Boolean> BOOLEAN = new Converter<Boolean>() {
		@Override
		public Boolean convert(Object value) {
			checkNotNullIAE( "value", value );

			return Boolean.class.isInstance( value )
					? Boolean.class.cast( value )
					: Boolean.parseBoolean( value.toString() );
		}
	};

	public static final Converter<String> STRING = new Converter<String>() {
		@Override
		public String convert(Object value) {
			checkNotNullIAE( "value", value );

			return value.toString();
		}
	};

	/**
	 * Disallow direct instantiation
	 */
	private StandardConverters() {
	}
}
