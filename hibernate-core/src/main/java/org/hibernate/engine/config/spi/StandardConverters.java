/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.config.spi;


import static org.hibernate.engine.config.spi.ConfigurationService.Converter;

/**
 * Standard set of setting converters
 *
 * @author Steve Ebersole
 */
public class StandardConverters {
	public static final Converter<Boolean> BOOLEAN = new Converter<Boolean>() {
		@Override
		public Boolean convert(Object value) {
			if ( value == null ) {
				throw new IllegalArgumentException( "Null value passed to convert" );
			}

			return Boolean.class.isInstance( value )
					? Boolean.class.cast( value )
					: Boolean.parseBoolean( value.toString() );
		}
	};

	public static final Converter<String> STRING = new Converter<String>() {
		@Override
		public String convert(Object value) {
			if ( value == null ) {
				throw new IllegalArgumentException( "Null value passed to convert" );
			}

			return value.toString();
		}
	};

	/**
	 * Disallow direct instantiation
	 */
	private StandardConverters() {
	}
}
