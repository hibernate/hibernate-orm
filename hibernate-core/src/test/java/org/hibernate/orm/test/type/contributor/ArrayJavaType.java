/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor;

import java.sql.Types;
import java.util.Arrays;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Vlad Mihalcea
 */
public class ArrayJavaType extends AbstractClassJavaType<Array> {

	private static final String DELIMITER = ",";

	public static final ArrayJavaType INSTANCE = new ArrayJavaType();

	public ArrayJavaType() {
		super( Array.class );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getJdbcType( Types.VARCHAR );
	}

	@Override
	public String toString(Array value) {
		StringBuilder builder = new StringBuilder();
		for ( String token : value ) {
			if ( builder.length() > 0 ) {
				builder.append( DELIMITER );
			}
			builder.append( token );
		}
		return builder.toString();
	}

	public static String extractString(Array value) {
		StringBuilder builder = new StringBuilder();
		for ( String token : value ) {
			if ( builder.length() > 0 ) {
				builder.append( DELIMITER );
			}
			builder.append( token );
		}
		return builder.toString();
	}

	@Override
	public Array fromString(CharSequence string) {
		if ( string == null || string.length() == 0 ) {
			return null;
		}
		String[] tokens = string.toString().split( DELIMITER );
		Array array = new Array();
		array.addAll( Arrays.asList(tokens) );
		return array;
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(Array value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Array.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value);
		}
		throw unknownUnwrap( type );
	}

	public <X> Array wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof String ) {
			return fromString( (String) value );
		}
		if ( value instanceof Array ) {
			return (Array) value;
		}
		throw unknownWrap( value.getClass() );
	}
}
