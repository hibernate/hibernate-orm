/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import java.time.ZoneOffset;
import java.util.Comparator;

/**
 * Descriptor for {@link ZoneOffset} handling.
 *
 * @author Gavin King
 */
public class ZoneOffsetJavaType extends AbstractClassJavaType<ZoneOffset> {
	public static final ZoneOffsetJavaType INSTANCE = new ZoneOffsetJavaType();

	public static class ZoneOffsetComparator implements Comparator<ZoneOffset> {
		public static final ZoneOffsetComparator INSTANCE = new ZoneOffsetComparator();

		public int compare(ZoneOffset o1, ZoneOffset o2) {
			return o1.getId().compareTo( o2.getId() );
		}
	}

	public ZoneOffsetJavaType() {
		super( ZoneOffset.class, ImmutableMutabilityPlan.instance(), ZoneOffsetComparator.INSTANCE );
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof ZoneOffset;
	}

	@Override
	public ZoneOffset cast(Object value) {
		return (ZoneOffset) value;
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	public String toString(ZoneOffset value) {
		return value.getId();
	}

	public ZoneOffset fromString(CharSequence string) {
		return ZoneOffset.of( string.toString() );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return StringJavaType.INSTANCE.getRecommendedJdbcType( context );
	}

	@Override
	public <X> X unwrap(ZoneOffset value, Class<X> type, WrapperOptions wrapperOptions) {
		if ( value == null ) {
			return null;
		}
		if ( ZoneOffset.class.isAssignableFrom( type ) ) {
			return type.cast( value );
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return type.cast( toString( value ) );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return type.cast( value.getTotalSeconds() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> ZoneOffset wrap(X value, WrapperOptions wrapperOptions) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof ZoneOffset zoneOffset ) {
			return zoneOffset;
		}
		if ( value instanceof CharSequence charSequence ) {
			return fromString( charSequence );
		}
		if ( value instanceof Integer integer ) {
			return ZoneOffset.ofTotalSeconds( integer );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return 6;
	}
}
