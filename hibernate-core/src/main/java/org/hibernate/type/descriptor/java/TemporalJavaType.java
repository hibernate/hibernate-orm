/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;

import org.hibernate.Incubating;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * Specialized JavaType for temporal types.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface TemporalJavaType<T> extends BasicJavaType<T> {

	static int resolveJdbcTypeCode(TemporalType requestedTemporalPrecision) {
		switch ( requestedTemporalPrecision ) {
			case DATE:
				return Types.DATE;
			case TIME:
				return Types.TIME;
			case TIMESTAMP:
				return Types.TIMESTAMP;
		}
		throw new UnsupportedOperationException( "Unsupported precision: " + requestedTemporalPrecision );
	}

	static Class<?> resolveJavaTypeClass(TemporalType requestedTemporalPrecision) {
		switch ( requestedTemporalPrecision ) {
			case DATE:
				return java.sql.Date.class;
			case TIME:
				return java.sql.Time.class;
			case TIMESTAMP:
				return java.sql.Timestamp.class;
		}
		throw new UnsupportedOperationException( "Unsupported precision: " + requestedTemporalPrecision );
	}

	/**
	 * The precision represented by this type
	 */
	TemporalType getPrecision();

	/**
	 * Resolve the appropriate TemporalJavaType for the given precision
	 * "relative" to this type.
	 */
	<X> TemporalJavaType<X> resolveTypeForPrecision(
			TemporalType precision,
			TypeConfiguration typeConfiguration);

	@Override
	default boolean isTemporalType() {
		return true;
	}
}
