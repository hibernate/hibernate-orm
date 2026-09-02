/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Specialized [JavaType] for temporal values and precision resolution.
///
/// @param <T> the represented temporal value type
/// @author Steve Ebersole
@Incubating
@SPI({ USE, IMPLEMENT })
public interface TemporalJavaType<T> extends BasicJavaType<T> {

	static int resolveJdbcTypeCode(
			@SuppressWarnings("deprecation")
			TemporalType requestedTemporalPrecision) {
		return switch ( requestedTemporalPrecision ) {
			case DATE -> Types.DATE;
			case TIME -> Types.TIME;
			case TIMESTAMP -> Types.TIMESTAMP;
		};
	}

	static Class<?> resolveJavaTypeClass(
			@SuppressWarnings("deprecation")
			TemporalType requestedTemporalPrecision) {
		return switch ( requestedTemporalPrecision ) {
			case DATE -> java.sql.Date.class;
			case TIME -> java.sql.Time.class;
			case TIMESTAMP -> java.sql.Timestamp.class;
		};
	}

	/**
	 * The precision represented by this type
	 */
	@SuppressWarnings("deprecation")
	TemporalType getPrecision();

	/**
	 * Resolve the appropriate TemporalJavaType for the given precision
	 * "relative" to this type.
	 */
	TemporalJavaType<T> resolveTypeForPrecision(
			@SuppressWarnings("deprecation") TemporalType precision,
			TypeConfiguration typeConfiguration);

	@Override
	default boolean isTemporalType() {
		return true;
	}
}
