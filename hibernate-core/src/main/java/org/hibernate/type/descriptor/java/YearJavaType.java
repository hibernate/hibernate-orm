/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import java.time.Year;
import java.time.format.DateTimeFormatter;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Describes the {@link java.time.Year} Java type
 */
public class YearJavaType extends AbstractClassJavaType<Year> {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy" );
	public static final YearJavaType INSTANCE = new YearJavaType();

	public YearJavaType() {
		super( Year.class );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.INTEGER );
	}

	@Override
	public String toString(Year value) {
		return value == null ? null : value.format( FORMATTER );
	}

	@Override
	public Year fromString(CharSequence string) {
		return string == null ? null : Year.parse( string, FORMATTER );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(Year value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( type.isInstance( value ) ) {
			return (X) value;
		}

		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) Integer.valueOf( value.getValue() );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) Long.valueOf( value.getValue() );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Year wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Year) {
			return (Year) value;
		}

		if ( value instanceof Number ) {
			return Year.of( ( (Number) value ).intValue() );
		}

		if ( value instanceof String ) {
			return fromString( (String) value );
		}

		throw unknownWrap( value.getClass() );
	}

}
