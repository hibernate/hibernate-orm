/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	public boolean isInstance(Object value) {
		return value instanceof Year;
	}

	@Override
	public Year cast(Object value) {
		return (Year) value;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.INTEGER );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(Year value) {
		return value == null ? null : value.format( FORMATTER );
	}

	@Override
	public Year fromString(CharSequence string) {
		return string == null ? null : Year.parse( string, FORMATTER );
	}

	@Override
	public <X> X unwrap(Year value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( type.isInstance( value ) ) {
			return type.cast( value );
		}

		if ( Integer.class.isAssignableFrom( type ) ) {
			return type.cast( value.getValue() );
		}

		if ( Long.class.isAssignableFrom( type ) ) {
			return type.cast( (long) value.getValue() );
		}

		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( toString( value ) );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Year wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof Year year) {
			return year;
		}

		if ( value instanceof Number number ) {
			return Year.of( number.intValue() );
		}

		if ( value instanceof String string ) {
			return fromString( string );
		}

		throw unknownWrap( value.getClass() );
	}

}
