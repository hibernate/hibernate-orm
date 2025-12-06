/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import java.time.ZoneId;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Describes the {@link ZoneId} Java type.
 */
public class ZoneIdJavaType extends AbstractClassJavaType<ZoneId> {
	/**
	 * Singleton access
	 */
	public static final ZoneIdJavaType INSTANCE = new ZoneIdJavaType();

	public ZoneIdJavaType() {
		super( ZoneId.class );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof ZoneId;
	}

	@Override
	public ZoneId cast(Object value) {
		return (ZoneId) value;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getJdbcType( Types.VARCHAR );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	@Override
	public String toString(ZoneId value) {
		return value == null ? null : value.getId();
	}

	@Override
	public ZoneId fromString(CharSequence string) {
		return string == null ? null : ZoneId.of( string.toString() );
	}

	@Override
	public <X> X unwrap(ZoneId value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( ZoneId.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( toString( value ) );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> ZoneId wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof ZoneId zoneId ) {
			return zoneId;
		}
		if ( value instanceof String string ) {
			return fromString( string );
		}
		throw unknownWrap( value.getClass() );
	}

}
