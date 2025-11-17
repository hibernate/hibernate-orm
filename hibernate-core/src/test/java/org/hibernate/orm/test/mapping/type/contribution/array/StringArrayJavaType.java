/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.contribution.array;

import java.sql.Types;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import static org.hibernate.internal.util.StringHelper.WHITESPACE;

/**
 * @author Steve Ebersole
 */
public class StringArrayJavaType implements BasicJavaType<String[]> {
	/**
	 * Singleton access
	 */
	public static final StringArrayJavaType INSTANCE = new StringArrayJavaType();

	@Override
	public Class<String[]> getJavaTypeClass() {
		return String[].class;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( Types.ARRAY );
	}

	@Override
	public String[] fromString(CharSequence string) {
		if ( string == null ) {
			return null;
		}
		return StringHelper.split( WHITESPACE, string.toString() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X unwrap(String[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( type.isArray() ) {
			assert type.getComponentType() == String.class;
			return (X) value;
		}

		if ( type == String.class ) {
			return (X) ArrayHelper.toString( value );
		}

		throw unsupported( type );
	}

	private <X> RuntimeException unsupported(Class<X> type) {
		return new RuntimeException( "String[] cannot be handled as `" + type.getName() + "`" );
	}

	@Override
	public <X> String[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof String[] ) {
			return (String[]) value;
		}

		if ( value instanceof CharSequence ) {
			return fromString( (CharSequence) value );
		}

		throw unsupported( value.getClass() );
	}
}
